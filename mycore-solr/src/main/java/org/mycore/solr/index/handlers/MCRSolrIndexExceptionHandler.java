package org.mycore.solr.index.handlers;

import org.mycore.solr.index.MCRSolrIndexHandler;

public interface MCRSolrIndexExceptionHandler {
    void handleException(MCRSolrIndexHandler indexHandler, Throwable exc);
}
