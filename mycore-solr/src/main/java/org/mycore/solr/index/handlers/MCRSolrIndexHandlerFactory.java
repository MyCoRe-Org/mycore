/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.index.handlers;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.file.MCRSolrPathDocumentFactory;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFileIndexHandler;
import org.mycore.solr.index.strategy.MCRSolrIndexStrategyManager;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRSolrIndexHandlerFactory {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrIndexHandlerFactory.class);

    private static MCRSolrIndexHandlerFactory instance = new MCRSolrLazyInputDocumentHandlerFactory();

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

    public boolean checkFile(Path file, BasicFileAttributes attrs) {
        return MCRSolrIndexStrategyManager.checkFile(file, attrs);
    }

    public MCRSolrIndexHandler getIndexHandler(Path file, BasicFileAttributes attrs, SolrClient solrClient)
        throws IOException {
        return this.getIndexHandler(file, attrs, solrClient, checkFile(file, attrs));
    }

    public MCRSolrIndexHandler getIndexHandler(Path file, BasicFileAttributes attrs, SolrClient solrClient,
        boolean sendContent) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: submitting file \"{} for indexing", file);
        }
        MCRSolrIndexHandler indexHandler;
        long start = System.currentTimeMillis();
        if (sendContent) {
            /* extract metadata with tika */
            indexHandler = new MCRSolrFileIndexHandler(file, attrs, solrClient);
        } else {
            SolrInputDocument doc = MCRSolrPathDocumentFactory.getInstance().getDocument(file, attrs);
            indexHandler = new MCRSolrInputDocumentHandler(doc, solrClient);
        }
        long end = System.currentTimeMillis();
        indexHandler.getStatistic().addTime(end - start);
        return indexHandler;
    }

}
