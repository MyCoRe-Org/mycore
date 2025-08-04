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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.MCRSolrIndexer;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.handlers.MCRSolrErrorIndexHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * Handles the indexing of a single {@link SolrInputDocument}.
 * <p>
 * This class uses a {@link Supplier} to lazily create the Solr document, deferring the potentially
 * expensive transformation process until the handler is executed by the indexer.
 * <p>
 * This handler is robust against failures. If an exception occurs, either during the creation of the
 * Solr document or during its submission to Solr, it will delegate the error handling to a new
 * {@link MCRSolrErrorIndexHandler}. This new handler is added to the sub-handler queue, ensuring that
 * a minimal error document is indexed in place of the failed one, making indexing errors searchable.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrInputDocumentHandler extends MCRSolrAbstractIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String id;

    private final Supplier<SolrInputDocument> documentSupplier;

    private final List<MCRSolrIndexHandler> subHandlers;

    /**
     * Constructs a new handler for a single Solr document.
     *
     * @param documentSupplier A supplier that provides the {@link SolrInputDocument} to be indexed.
     *                         This allows for lazy creation of the document.
     * @param id The unique identifier of the MyCoRe object being indexed. Used for logging and error reporting.
     * @param coreType The type of Solr core this document should be sent to (e.g., main).
     */
    public MCRSolrInputDocumentHandler(Supplier<SolrInputDocument> documentSupplier, String id,
        MCRSolrCoreType coreType) {
        this.id = id;
        this.documentSupplier = documentSupplier;
        this.subHandlers = new ArrayList<>();
        this.setCoreType(coreType);
    }

    /**
     * Attempts to index a single {@link SolrInputDocument}.
     * First, it resolves the document from the supplier. If this fails, it queues an error handler.
     * If successful, it proceeds to send the document to all configured Solr clients for the specified core type.
     * If sending the document fails for any client, an error handler is queued for that specific client.
     * When {@link MCRSolrUtils#useNestedDocuments()} is true, it performs a delete-by-id operation before adding
     * the new document to ensure clean updates of parent/child relationships.
     *
     * @throws IOException if an I/O error occurs during communication with Solr.
     * @throws SolrServerException if a base Solr error occurs.
     */
    @Override
    public void index() throws IOException, SolrServerException {
        // get clients
        List<SolrClient> solrClients = getClients();

        // get solr document
        SolrInputDocument document;
        try {
            document = documentSupplier.get();
        } catch (Exception e) {
            LOGGER.error(() -> "Error while creating solr document '" + id + "'.", e);
            this.subHandlers.add(new MCRSolrErrorIndexHandler(id, e, solrClients));
            return;
        }
        String docId = String.valueOf(document.getFieldValue("id"));

        LOGGER.info("Sending {} to SOLR...", docId);
        UpdateRequest updateRequest = getUpdateRequest(MCRSolrConstants.SOLR_UPDATE_PATH);
        updateRequest.add(document);
        for (SolrClient solrClient : solrClients) {
            if (MCRSolrUtils.useNestedDocuments()) {
                MCRSolrIndexer.deleteById(solrClient, docId);
            }
            try {
                updateRequest.process(solrClient);
            } catch (Exception e) {
                LOGGER.error(() -> "Error while indexing document '" + docId + "'.", e);
                this.subHandlers.add(new MCRSolrMinimalInputDocumentHandler(document, e, solrClient));
            }
        }
    }

    /**
     * Returns a list of subsequent handlers to be executed.
     * <p>
     * This list is empty on a successful run but will contain {@link MCRSolrErrorIndexHandler}
     * if an error occurred during the indexing process.
     *
     * @return a list of sub-handlers.
     */
    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return subHandlers;
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
        return "index " + this.id;
    }

    public String getId() {
        return id;
    }
}
