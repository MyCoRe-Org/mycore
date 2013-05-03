/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 16, 2013 $
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

package org.mycore.solr.index.handlers;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.cs.MCRSolrFileContentStream;
import org.mycore.solr.index.file.MCRSolrMCRFileDocumentFactory;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFileIndexHandler;
import org.mycore.solr.index.strategy.MCRSolrIndexStrategyManager;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRSolrIndexHandlerFactory {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrIndexHandlerFactory.class);

    private static MCRSolrIndexHandlerFactory instance = (MCRSolrIndexHandlerFactory) MCRConfiguration.instance().getInstanceOf(
        CONFIG_PREFIX + "IndexHandler.Factory", MCRSolrContentStreamHandlerFactory.class.getCanonicalName());

    public static MCRSolrIndexHandlerFactory getInstance() {
        return instance;
    }

    public abstract MCRSolrIndexHandler getIndexHandler(MCRContent content, MCRObjectID id);

    public abstract MCRSolrIndexHandler getIndexHandler(Map<MCRObjectID, MCRContent> contentMap);

    public MCRSolrIndexHandler getIndexHandler(MCRObjectID... ids) throws IOException {
        if (ids.length == 1) {
            MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(ids[0]);
            return getIndexHandler(content, ids[0]);
        }
        HashMap<MCRObjectID, MCRContent> contentMap = new HashMap<>();
        for (MCRObjectID id : ids) {
            MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(id);
            contentMap.put(id, content);
        }
        return getIndexHandler(contentMap);
    }

    public MCRSolrIndexHandler getIndexHandler(MCRBase... derOrObjs) {
        if (derOrObjs.length == 1) {
            MCRBaseContent content = new MCRBaseContent(derOrObjs[0]);
            return getIndexHandler(content, derOrObjs[0].getId());
        }
        HashMap<MCRObjectID, MCRContent> contentMap = new HashMap<>();
        for (MCRBase derOrObj : derOrObjs) {
            MCRBaseContent content = new MCRBaseContent(derOrObj);
            contentMap.put(derOrObj.getId(), content);
        }
        return getIndexHandler(contentMap);
    }

    public MCRSolrIndexHandler getIndexHandler(MCRFile file, SolrServer solrServer) throws IOException {
        return this.getIndexHandler(file, solrServer, checkFile(file));
    }

    public MCRSolrIndexHandler getIndexHandler(MCRFile file, SolrServer solrServer, boolean sendContent) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: submitting file \"" + file.getAbsolutePath() + " (" + file.getID() + ")\" for indexing");
        }
        MCRSolrIndexHandler indexHandler;
        long start = System.currentTimeMillis();
        if (sendContent) {
            /* extract metadata with tika */
            indexHandler = new MCRSolrFileIndexHandler(new MCRSolrFileContentStream(file), solrServer);
        } else {
            SolrInputDocument doc = MCRSolrMCRFileDocumentFactory.getInstance().getDocument(file);
            indexHandler = new MCRSolrInputDocumentHandler(doc, solrServer);
        }
        long end = System.currentTimeMillis();
        indexHandler.getStatistic().addTime(end - start);
        return indexHandler;
    }

    public boolean checkFile(MCRFile file) {
        return MCRSolrIndexStrategyManager.checkFile(file);
    }

}
