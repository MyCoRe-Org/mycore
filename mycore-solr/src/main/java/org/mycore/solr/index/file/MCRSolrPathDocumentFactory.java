/*
 * $Id$
 * $Revision: 5697 $ $Date: May 3, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.solr.index.file;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRCache.ModifiedHandle;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.struct.AbstractLogicalDiv;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.LogicalSubDiv;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.stream.MCRSolrFileIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;
import org.mycore.urn.services.MCRURNManager;

import com.google.common.io.Files;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

/**
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRSolrPathDocumentFactory {

    private static Logger LOGGER = Logger.getLogger(MCRSolrPathDocumentFactory.class);

    private static MCRSolrPathDocumentFactory instance = MCRConfiguration.instance()
        .<MCRSolrPathDocumentFactory> getInstanceOf(CONFIG_PREFIX + "SolrInputDocument.Path.Factory", (String) null);

    private static final MCRCategoryDAO CATEGORY_DAO = MCRCategoryDAOFactory.getInstance();

    private static final MCRCache<String, String> derivateModified = new MCRCache<>(10000,
        "derivateID ISODateString cache");

    private static MCRXMLMetadataManager XML_MANAGER = MCRXMLMetadataManager.instance();

    public static MCRSolrPathDocumentFactory getInstance() {
        return instance;
    }

    /**
     * Generates a {@link SolrInputDocument} from a {@link MCRPath} instance.
     * 
     * @see MCRSolrFileIndexHandler
     * @see MCRSolrFilesIndexHandler
     * @see MCRSolrIndexHandlerFactory
     */
    public SolrInputDocument getDocument(Path input, BasicFileAttributes attr) throws IOException,
        MCRPersistenceException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", input.toUri().toString());
        String absolutePath = '/' + input.subpath(0, input.getNameCount()).toString();
        try {
            MCRPath mcrPath = MCRPath.toMCRPath(input); //check if this is an MCRPath -> more metadata
            MCRObjectID mcrObjID = MCRMetadataManager.getObjectId(MCRObjectID.getInstance(mcrPath.getOwner()), 10,
                TimeUnit.SECONDS);
            if (mcrObjID == null) {
                LOGGER.warn("Could not determine MCRObject for file " + absolutePath);
                doc.setField("returnId", mcrPath.getOwner());
            } else {
                doc.setField("returnId", mcrObjID.toString());
                doc.setField("objectProject", mcrObjID.getProjectId());
            }
            String ownerID = mcrPath.getOwner();
            doc.setField("derivateID", ownerID);
            String urn = MCRURNManager.getURNForFile(ownerID,
                absolutePath.substring(0, absolutePath.lastIndexOf("/") + 1), input.getFileName().toString());
            if (urn != null) {
                doc.setField("fileURN", urn);
            }
            doc.setField("derivateModified", getDerivateModified(ownerID));
            Collection<MCRCategoryID> linksFromReference = MCRCategLinkServiceFactory.getInstance()
                .getLinksFromReference(new MCRCategLinkReference(mcrPath));
            HashSet<MCRCategoryID> linkedCategories = new HashSet<>(linksFromReference);
            for (MCRCategoryID category : linksFromReference) {
                for (MCRCategory parent : CATEGORY_DAO.getParents(category)) {
                    linkedCategories.add(parent.getId());
                }
            }
            for (MCRCategoryID category : linkedCategories) {
                doc.addField("fileCategory", category.toString());
            }
        } catch (ProviderMismatchException e) {
            LOGGER.warn("Cannot build all fields as input is not an instance of MCRPath: " + input);
        }
        doc.setField("objectType", "data_file");
        doc.setField("fileName", input.getFileName().toString());
        doc.setField("filePath", absolutePath);
        doc.setField("stream_size", attr.size());
        doc.setField("stream_name", absolutePath);
        doc.setField("stream_source_info", input.toString());
        doc.setField("stream_content_type", MCRContentTypes.probeContentType(input));
        doc.setField("extension", Files.getFileExtension(input.getFileName().toString()));
        MCRISO8601Date iDate = new MCRISO8601Date();
        iDate.setDate(new Date(attr.lastModifiedTime().toMillis()));
        doc.setField("modified", iDate.getISOString());

        //        if (input.hasAudioVideoExtender()) {
        //            MCRAudioVideoExtender ext = input.getAudioVideoExtender();
        //            doc.setField("bitRate", ext.getBitRate());
        //            doc.setField("frameRate", ext.getFrameRate());
        //            doc.setField("duration", ext.getDurationTimecode());
        //            doc.setField("mediaType", (ext.hasVideoStream() ? "video" : "audio"));
        //        }

        String metsFileName = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        if (input.getFileName().toString().equalsIgnoreCase(metsFileName)) {
            try {
                Document d = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRPathContent(input));
                Mets p = new Mets(d);
                LogicalStructMap structMap = (LogicalStructMap) p.getStructMap(LogicalStructMap.TYPE);
                LogicalDiv rootDiv = structMap.getDivContainer();
                List<AbstractLogicalDiv> childs = getAllChilds(rootDiv);

                for (AbstractLogicalDiv abstractLogicalDiv : childs) {
                    doc.addField("content", abstractLogicalDiv.getLabel());

                    // some types contain - or other illegal character and they should be removed
                    String cleanType = abstractLogicalDiv.getType().replaceAll("[\\-\\s]","");

                    doc.addField("mets." + cleanType, abstractLogicalDiv.getLabel());
                }
            } catch (Exception e) {
                throw new MCRPersistenceException("could not parse mets.xml", e);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MCRFile " + input.toString() + " transformed to:\n" + doc.toString());
        }

        return doc;
    }

    private List<AbstractLogicalDiv> getAllChilds(AbstractLogicalDiv rootDiv) {
        ArrayList<AbstractLogicalDiv> allChildren = new ArrayList<AbstractLogicalDiv>();
        List<LogicalSubDiv> children = rootDiv.getChildren();

        allChildren.add(rootDiv);
        for (LogicalSubDiv logicalSubDiv : children) {
            allChildren.addAll(getAllChilds(logicalSubDiv));
        }
        return allChildren;
    }

    /**
     * returns ISO8601 formated string of when derivate was last modified
     * 
     * @param derivateID
     * @throws IOException
     *             thrown by {@link ModifiedHandle#getLastModified()}
     */
    private static String getDerivateModified(final String derivateID) throws IOException {
        MCRObjectID derID = MCRObjectID.getInstance(derivateID);
        ModifiedHandle modifiedHandle = XML_MANAGER.getLastModifiedHandle(derID, 30, TimeUnit.SECONDS);
        String modified = derivateModified.getIfUpToDate(derivateID, modifiedHandle);
        if (modified == null) {
            Date date = new Date(modifiedHandle.getLastModified());
            MCRISO8601Date date2 = new MCRISO8601Date();
            date2.setDate(date);
            modified = date2.getISOString();
            derivateModified.put(derivateID, modified);
        }
        return modified;
    }

}
