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

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.index.MCRSolrIndexer;

/**
 * A specialized index handler responsible for indexing a minimal error document when a
 * primary indexing operation fails. This ensures that indexing failures are discoverable
 * directly within the Solr index.
 * <p>
 * The handler creates a document with the following fields:
 * <ul>
 *   <li><b>id</b>: The ID of the document that failed to index.</li>
 *   <li><b>error_message</b>: A string containing the exception message.</li>
 *   <li><b>error_stacktrace</b>: The stacktrace of the error.</li>
 * </ul>
 *
 * @author Matthias Eichner
 */
public class MCRSolrErrorIndexHandler extends MCRSolrAbstractErrorIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<SolrClient> clients;

    private final Exception cause;

    private final String failedId;

    /**
     * Constructs a new error handler.
     *
     * @param failedId The ID of the document that failed to index.
     * @param cause The exception that caused the failure.
     * @param clients The list of Solr clients to which the error document should be sent.
     */
    public MCRSolrErrorIndexHandler(String failedId, Exception cause, List<SolrClient> clients) {
        this.clients = clients;
        this.cause = cause;
        this.failedId = failedId;
    }

    /**
     * Creates and indexes a minimal document containing error information.
     *
     * @throws IOException if an I/O error occurs.
     * @throws SolrServerException if a base Solr error occurs.
     */
    @Override
    public void index() throws IOException, SolrServerException {
        SolrInputDocument errorDoc = buildDocument(failedId, cause);

        UpdateRequest errorRequest = getUpdateRequest(MCRSolrConstants.SOLR_UPDATE_PATH);
        errorRequest.add(errorDoc);

        for (SolrClient solrClient : clients) {
            try {
                if (MCRSolrUtils.useNestedDocuments()) {
                    MCRSolrIndexer.deleteById(solrClient, failedId);
                }
                errorRequest.process(solrClient);
                LOGGER.info("Successfully indexed error document for id '{}'.", failedId);
            } catch (Exception ex) {
                LOGGER.error(() -> "FATAL: Could not index error document for id '" + failedId + "'.", ex);
            }
        }
    }

}
