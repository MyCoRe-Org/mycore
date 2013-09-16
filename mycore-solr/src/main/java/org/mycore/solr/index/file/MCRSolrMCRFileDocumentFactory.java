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

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRCache.ModifiedHandle;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRAudioVideoExtender;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.services.urn.MCRURNManager;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.stream.MCRSolrFileIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrMCRFileDocumentFactory {

    private static final String DEFAULT_CONTENT_TYPE_ID = MCRFileContentTypeFactory.getDefaultType().getID();

    private static Logger LOGGER = Logger.getLogger(MCRSolrMCRFileDocumentFactory.class);

    private static MCRSolrMCRFileDocumentFactory instance = MCRConfiguration.instance().getInstanceOf(
            CONFIG_PREFIX + "SolrInputDocument.MCRFile.Factory", MCRSolrMCRFileDocumentFactory.class);

    private static final MCRCategoryDAO CATEGORY_DAO = MCRCategoryDAOFactory.getInstance();

    private static MimetypesFileTypeMap mimetypesMap = new MimetypesFileTypeMap();

    private static final MCRCache<String, String> derivateModified = new MCRCache<>(10000, "derivateID ISODateString cache");

    private static MCRXMLMetadataManager XML_MANAGER = MCRXMLMetadataManager.instance();

    public static MCRSolrMCRFileDocumentFactory getInstance() {
        return instance;
    }

    /**
     * Generates a {@link SolrInputDocument} from a {@link MCRFile} instance.
     * @see MCRSolrFileIndexHandler
     * @see MCRSolrFilesIndexHandler
     * @see MCRSolrIndexHandlerFactory
     * @param input
     * @return
     * @throws IOException
     */
    public SolrInputDocument getDocument(MCRFile input) throws IOException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", input.getID());
        MCRObjectID mcrObjID = input.getMCRObjectID();
        String absolutePath = input.getAbsolutePath();
        String ownerID = input.getOwnerID();
        if (mcrObjID == null) {
            LOGGER.warn("Could not determine MCRObject for file " + absolutePath);
            doc.setField("returnId", ownerID);
        } else {
            doc.setField("returnId", mcrObjID.toString());
            doc.setField("objectProject", mcrObjID.getProjectId());
        }
        doc.setField("objectType", "data_file");
        doc.setField("derivateID", ownerID);
        doc.setField("fileName", input.getName());
        doc.setField("filePath", absolutePath);
        doc.setField("stream_size", input.getSize());
        doc.setField("stream_name", absolutePath);
        doc.setField("stream_source_info", input.getStoreID() + ":" + input.getStorageID());
        MCRFileContentType contentType = input.getContentType();
        String mimeType;
        //if file content type is default: look for correct mime type
        if (contentType.getID().equals(DEFAULT_CONTENT_TYPE_ID)) {
            mimeType = mimetypesMap.getContentType(input.getName());
        } else {
            mimeType = contentType.getMimeType();
        }
        doc.setField("stream_content_type", mimeType);
        doc.setField("extension", input.getExtension());
        doc.setField("contentTypeID", input.getContentTypeID());
        doc.setField("contentType", input.getContentType().getLabel());
        String urn = MCRURNManager.getURNForFile(ownerID, absolutePath.substring(0, absolutePath.lastIndexOf("/") + 1), input.getName());
        if (urn != null) {
            doc.setField("fileURN", urn);
        }
        Collection<MCRCategoryID> linksFromReference = MCRCategLinkServiceFactory.getInstance().getLinksFromReference(
                MCRFile.getCategLinkReference(MCRObjectID.getInstance(ownerID), absolutePath));
        HashSet<MCRCategoryID> linkedCategories = new HashSet<>(linksFromReference);
        for (MCRCategoryID category : linksFromReference) {
            for (MCRCategory parent : CATEGORY_DAO.getParents(category)) {
                linkedCategories.add(parent.getId());
            }
        }
        for (MCRCategoryID category : linkedCategories) {
            doc.addField("fileCategory", category.toString());
        }
        MCRISO8601Date iDate = new MCRISO8601Date();
        iDate.setDate(input.getLastModified().getTime());
        doc.setField("modified", iDate.getISOString());

        //        MCRDerivate d = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(input.getOwnerID()));
        //        Date date = d.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        //        MCRISO8601Date date2 = new MCRISO8601Date();
        //        date2.setDate(date);
        //        doc.setField("derivateModified", date2.getISOString());
        doc.setField("derivateModified", getDerivateModified(ownerID));

        if (input.hasAudioVideoExtender()) {
            MCRAudioVideoExtender ext = input.getAudioVideoExtender();
            doc.setField("bitRate", ext.getBitRate());
            doc.setField("frameRate", ext.getFrameRate());
            doc.setField("duration", ext.getDurationTimecode());
            doc.setField("mediaType", (ext.hasVideoStream() ? "video" : "audio"));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MCRFile " + input.getID() + " transformed to:\n" + doc.toString());
        }

        return doc;
    }

    /**
     * returns ISO8601 formated string of when derivate was last modified
     * @param derivateID
     * @throws IOException thrown by {@link ModifiedHandle#getLastModified()}
     */
    public static String getDerivateModified(final String derivateID) throws IOException {
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
