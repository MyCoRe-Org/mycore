package org.mycore.solr.index.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.solr.index.MCRSolrIndexHandler;

public class MCRSolrIndexExceptionLoggingHandler implements MCRSolrIndexExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handleException(MCRSolrIndexHandler indexHandler, Throwable exc) {
        LOGGER.error(() -> "Error while submitting index handler: " + indexHandler, exc);
    }
}
