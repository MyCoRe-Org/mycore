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

package org.mycore.solr.index.handlers.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;
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

    private static Logger LOGGER = LogManager.getLogger(MCRSolrInputDocumentsHandler.class);

    public MCRSolrInputDocumentsHandler(Collection<SolrInputDocument> documents) {
        this(documents, MCRSolrClientFactory.getSolrClient());
    }

    public MCRSolrInputDocumentsHandler(Collection<SolrInputDocument> documents, SolrClient solrClient) {
        super(solrClient);
        this.documents = documents;
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
    public void index() throws IOException, SolrServerException {
        if (documents == null || documents.isEmpty()) {
            LOGGER.warn("No input documents to index.");
            return;
        }
        int totalCount = documents.size();
        LOGGER.info("Handling " + totalCount + " documents");
        SolrClient solrClient = getSolrClient();
        if (solrClient instanceof ConcurrentUpdateSolrClient) {
            LOGGER.info("Detected ConcurrentUpdateSolrClient. Split up batch update.");
            splitDocuments();
            //for statistics:
            documents.clear();
            return;
        }
        UpdateResponse updateResponse;
        try {
            UpdateRequest updateRequest = getUpdateRequest(MCRSolrConstants.UPDATE_PATH);
            updateRequest.add(documents);
            updateResponse = updateRequest.process(getSolrClient());
        } catch (Throwable e) {
            LOGGER.warn("Error while indexing document collection. Split and retry.");
            splitDocuments();
            return;
        }
        if (updateResponse.getStatus() != 0) {
            LOGGER.error("Error while indexing document collection. Split and retry: " + updateResponse.getResponse());
            splitDocuments();
        } else {
            LOGGER.info("Sending " + totalCount + " documents was successful in " + updateResponse.getElapsedTime()
                + " ms.");
        }
    }

    private void splitDocuments() {
        subHandlerList = new ArrayList<>(documents.size());
        for (SolrInputDocument document : documents) {
            MCRSolrInputDocumentHandler subHandler = new MCRSolrInputDocumentHandler(document, getSolrClient());
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
