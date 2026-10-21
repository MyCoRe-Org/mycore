package org.mycore.solr.index.handlers.document;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MCRSolrInputDocumentExceptionLoggingHandler implements MCRSolrInputDocumentExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handleException(String id, Throwable exc) {
        LOGGER.error(() -> "Error while indexing document '" + id + "'.", exc);
    }
}
