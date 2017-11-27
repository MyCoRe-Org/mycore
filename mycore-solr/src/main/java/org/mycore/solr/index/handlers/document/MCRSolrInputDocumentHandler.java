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

package org.mycore.solr.index.handlers.document;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrInputDocumentHandler extends MCRSolrAbstractIndexHandler {

    private static Logger LOGGER = LogManager.getLogger(MCRSolrInputDocumentHandler.class);

    SolrInputDocument document;

    public MCRSolrInputDocumentHandler(SolrInputDocument document) {
        super();
        this.document = document;
    }

    public MCRSolrInputDocumentHandler(SolrInputDocument document, SolrClient solrClient) {
        super(solrClient);
        this.document = document;
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.MCRSolrIndexHandler#index()
     */
    @Override
    public void index() throws IOException, SolrServerException {
        LOGGER.info("Sending {} to SOLR...", document.getFieldValue("id"));
        UpdateRequest updateRequest = getUpdateRequest(MCRSolrConstants.UPDATE_PATH);
        updateRequest.add(document);
        updateRequest.process(getSolrClient());
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.MCRSolrIndexHandler#getStatistic()
     */
    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.DOCUMENTS;
    }

    @Override
    public String toString() {
        return "index " + document.getFieldValue("id");
    }

}
