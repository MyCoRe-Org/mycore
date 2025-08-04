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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.MCRSolrIndexer;
import org.mycore.solr.index.handlers.MCRSolrAbstractErrorIndexHandler;
import org.mycore.solr.index.handlers.MCRSolrErrorIndexHandler;

/**
 * A "fallback" index handler that attempts to index a minimal version of a document that previously
 * failed to index.
 * <p>
 * This handler filters the original {@link SolrInputDocument}, keeping only a whitelisted set of "safe" fields
 * defined in the `MCR.Solr.SolrInputDocument.MinimalFields` property. It also adds error information to the document:
 * <ul>
 *   <li><b>id</b>: The ID of the document that failed to index.</li>
 *   <li><b>error_message</b>: A string containing the exception message.</li>
 *   <li><b>error_stacktrace</b>: The stacktrace of the error.</li>
 * </ul>
 * <p>
 * If indexing this minimal document also fails, it delegates to
 * {@link org.mycore.solr.index.handlers.MCRSolrErrorIndexHandler} as a final fallback to ensure the error is recorded.
 *
 * @author Matthias Eichner
 */
public class MCRSolrMinimalInputDocumentHandler extends MCRSolrAbstractErrorIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Set<String> minimalFields;

    static {
        try {
            minimalFields = MCRConfiguration2
                .getOrThrow("MCR.Solr.SolrInputDocument.MinimalFields", MCRConfiguration2::splitValue)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            LOGGER.error("Could not initialize minimal fields list.", e);
        }
    }

    private final SolrInputDocument original;

    private final Exception cause;

    private final SolrClient client;

    private final List<MCRSolrIndexHandler> subHandlers;

    /**
     * Constructs a new minimal document index handler.
     *
     * @param original The full document that failed to index.
     * @param cause    The exception that caused the initial failure.
     * @param client   The Solr clients to use for indexing.
     */
    public MCRSolrMinimalInputDocumentHandler(SolrInputDocument original, Exception cause, SolrClient client) {
        this.original = original;
        this.cause = cause;
        this.client = client;
        this.subHandlers = new ArrayList<>();
    }

    @Override
    public void index() throws IOException, SolrServerException {
        // delete old
        String failedId = original.get("id").getValue().toString();
        if (MCRSolrUtils.useNestedDocuments()) {
            MCRSolrIndexer.deleteById(client, failedId);
        }
        // create a new error document
        SolrInputDocument minimalDoc = buildDocument(failedId, cause);
        minimalFields.forEach(fieldKey -> {
            SolrInputField field = original.get(fieldKey);
            if (field != null) {
                minimalDoc.setField(fieldKey, field.getValue());
            }
        });
        UpdateRequest updateRequest = getUpdateRequest(MCRSolrConstants.SOLR_UPDATE_PATH);
        updateRequest.add(minimalDoc);
        try {
            updateRequest.process(client);
        } catch (Exception e) {
            LOGGER.error(() -> "Could not index the minimal document for id '" + failedId + "'.", e);
            this.subHandlers.add(new MCRSolrErrorIndexHandler(failedId, e, List.of(client)));
        }
        LOGGER.info("Successfully indexed error document for id '{}'.", failedId);
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return subHandlers;
    }

}
