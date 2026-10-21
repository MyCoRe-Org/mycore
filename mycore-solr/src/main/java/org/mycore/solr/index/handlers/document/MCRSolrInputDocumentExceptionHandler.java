package org.mycore.solr.index.handlers.document;

public interface MCRSolrInputDocumentExceptionHandler {
    void handleException(String id, Throwable exc);
}
