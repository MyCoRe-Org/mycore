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

package org.mycore.solr.index.handlers.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.document.MCRSolrInputDocumentFactory;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrMCRContentMapIndexHandler extends MCRSolrAbstractIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrMCRContentMapIndexHandler.class);

    private List<MCRSolrIndexHandler> subhandlers;

    private Map<MCRObjectID, MCRContent> contentMap;

    public MCRSolrMCRContentMapIndexHandler(Map<MCRObjectID, MCRContent> contentMap) {
        this(contentMap, MCRSolrClientFactory.getSolrClient());
    }

    public MCRSolrMCRContentMapIndexHandler(Map<MCRObjectID, MCRContent> contentMap, SolrClient solrClient) {
        super();
        this.contentMap = contentMap;
        this.subhandlers = new ArrayList<>(contentMap.size());
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.DOCUMENTS;
    }

    @Override
    public void index() throws IOException, SolrServerException {
        int totalCount = contentMap.size();
        LOGGER.info("Handling {} documents", totalCount);
        //multithread processing will result in too many http request
        UpdateResponse updateResponse;
        try {
            Iterator<SolrInputDocument> documents = MCRSolrInputDocumentFactory.getInstance().getDocuments(contentMap);
            SolrClient solrClient = getSolrClient();
            if (solrClient instanceof ConcurrentUpdateSolrClient) {
                //split up to speed up processing
                splitup(documents);
                return;
            }
            if (LOGGER.isDebugEnabled()) {
                ArrayList<SolrInputDocument> debugList = new ArrayList<>();
                while (documents.hasNext()) {
                    debugList.add(documents.next());
                }
                LOGGER.debug("Sending these documents: {}", debugList);
                //recreate documents interator;
                documents = debugList.iterator();
            }
            if (solrClient instanceof HttpSolrClient) {
                updateResponse = solrClient.add(documents);
            } else {
                ArrayList<SolrInputDocument> docs = new ArrayList<>(totalCount);
                while (documents.hasNext()) {
                    docs.add(documents.next());
                }
                updateResponse = solrClient.add(docs);
            }
        } catch (Throwable e) {
            LOGGER.warn("Error while indexing document collection. Split and retry.", e);
            splitup();
            return;
        }
        if (updateResponse.getStatus() != 0) {
            LOGGER.error("Error while indexing document collection. Split and retry: {}", updateResponse.getResponse());
            splitup();
        } else {
            LOGGER.info("Sending {} documents was successful in {} ms.", totalCount, updateResponse.getElapsedTime());
        }

    }

    private void splitup(Iterator<SolrInputDocument> documents) {
        while (documents.hasNext()) {
            MCRSolrInputDocumentHandler subhandler = new MCRSolrInputDocumentHandler(documents.next());
            subhandler.setCommitWithin(getCommitWithin());
            subhandlers.add(subhandler);
        }
        contentMap.clear();
    }

    private void splitup() {
        for (Map.Entry<MCRObjectID, MCRContent> entry : contentMap.entrySet()) {
            MCRSolrMCRContentIndexHandler subHandler = new MCRSolrMCRContentIndexHandler(entry.getKey(),
                entry.getValue(), getSolrClient());
            subHandler.setCommitWithin(getCommitWithin());
            subhandlers.add(subHandler);
        }
        contentMap.clear();
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return subhandlers;
    }

    @Override
    public int getDocuments() {
        return contentMap.size();
    }

    @Override
    public String toString() {
        return "bulk index " + contentMap.size() + " documents";
    }

}
