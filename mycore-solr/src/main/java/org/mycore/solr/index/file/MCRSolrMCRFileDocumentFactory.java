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

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.ifs.MCRAudioVideoExtender;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.urn.MCRURNManager;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.stream.MCRSolrFileIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrMCRFileDocumentFactory {

    private static Logger LOGGER = Logger.getLogger(MCRSolrMCRFileDocumentFactory.class);

    private static MCRSolrMCRFileDocumentFactory instance = MCRConfiguration.instance().getInstanceOf(
        CONFIG_PREFIX + "SolrInputDocument.MCRFile.Factory", MCRSolrMCRFileDocumentFactory.class);

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
        if (mcrObjID == null) {
            LOGGER.warn("Could not determine MCRObject for file " + absolutePath);
            doc.setField("returnId", input.getOwnerID());
        } else {
            doc.setField("returnId", mcrObjID.toString());
            doc.setField("objectProject", mcrObjID.getProjectId());
        }
        doc.setField("objectType", "data_file");
        doc.setField("derivateID", input.getOwnerID());
        doc.setField("fileName", input.getName());
        doc.setField("filePath", absolutePath);
        doc.setField("stream_size", input.getSize());
        doc.setField("stream_name", absolutePath);
        doc.setField("stream_source_info", input.getClass().getCanonicalName());
        doc.setField("stream_content_type", input.getContentType().getMimeType());
        doc.setField("extension", input.getExtension());
        doc.setField("contentTypeID", input.getContentTypeID());
        doc.setField("contentType", input.getContentType().getLabel());
        String urn = MCRURNManager.getURNForFile(input.getOwnerID(), absolutePath.substring(0, absolutePath.lastIndexOf("/") + 1),
            input.getName());
        if (urn != null) {
            doc.setField("urn", urn);
        }
        Collection<MCRCategoryID> linksFromReference = MCRCategLinkServiceFactory.getInstance().getLinksFromReference(
            MCRFile.getCategLinkReference(MCRObjectID.getInstance(input.getOwnerID()), absolutePath));
        for (MCRCategoryID category : linksFromReference) {
            doc.addField("category", category.toString());
        }
        MCRISO8601Date iDate = new MCRISO8601Date();
        iDate.setDate(input.getLastModified().getTime());
        doc.setField("modified", iDate.getISOString());

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

}
