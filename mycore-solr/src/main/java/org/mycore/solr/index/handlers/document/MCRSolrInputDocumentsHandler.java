/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.solr.index.handlers.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrInputDocumentsHandler extends MCRSolrAbstractIndexHandler {
    Collection<SolrInputDocument> documents;

    List<MCRSolrIndexHandler> subHandlerList;

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRSolrInputDocumentsHandler(Collection<SolrInputDocument> documents, MCRSolrCoreType coreType) {
        this.documents = documents;
        setCoreType(coreType);
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.MCRSolrIndexHandler#getStatistic()
     */
    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.DOCUMENTS;
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler#index()
     */
    @Override
    public void index() {
        if (documents == null || documents.isEmpty()) {
            LOGGER.warn("No input documents to index.");
            return;
        }
        int totalCount = documents.size();
        LOGGER.info("Handling {} documents", totalCount);
        for (SolrClient client : getClients()) {
            if (client instanceof ConcurrentUpdateHttp2SolrClient) {
                LOGGER.info("Detected ConcurrentUpdateSolrClient. Split up batch update.");
                splitDocuments();
                //for statistics:
                documents.clear();
                return;
            }
        }
        for (SolrClient client : getClients()) {
            UpdateResponse updateResponse;
            try {
                UpdateRequest updateRequest = getUpdateRequest(MCRSolrConstants.SOLR_UPDATE_PATH);
                getSolrAuthenticationFactory().applyAuthentication(updateRequest,
                    MCRSolrAuthenticationLevel.INDEX);
                updateRequest.add(documents);
                updateResponse = updateRequest.process(client);
            } catch (Exception e) {
                LOGGER.warn("Error while indexing document collection. Split and retry.");
                splitDocuments();
                return;
            }
            if (updateResponse.getStatus() != 0) {
                LOGGER.error("Error while indexing document collection. Split and retry: {}",
                    updateResponse::getResponse);
                splitDocuments();
            } else {
                LOGGER.info("Sending {} documents was successful in {} ms.", () -> totalCount,
                    updateResponse::getElapsedTime);
            }
        }
    }

    private void splitDocuments() {
        subHandlerList = new ArrayList<>(documents.size());
        for (SolrInputDocument document : documents) {
            MCRSolrInputDocumentHandler subHandler = new MCRSolrInputDocumentHandler(() -> document,
                String.valueOf(document.getFieldValue("id")), getCoreType());
            subHandler.setCommitWithin(getCommitWithin());
            this.subHandlerList.add(subHandler);
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return subHandlerList == null ? super.getSubHandlers() : subHandlerList;
    }

    @Override
    public int getDocuments() {
        return documents.size();
    }

    @Override
    public String toString() {
        return "index " + this.documents.size() + " mycore documents";
    }

}
