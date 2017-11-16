package org.mycore.solr.index.handlers.stream;

import org.apache.solr.client.solrj.SolrClient;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.cs.MCRSolrAbstractContentStream;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;

/**
 * Base class for solr indexing using content streams.
 */
public abstract class MCRSolrAbstractStreamIndexHandler extends MCRSolrAbstractIndexHandler {

    protected MCRSolrAbstractStreamIndexHandler() {
        this(null);
    }

    public MCRSolrAbstractStreamIndexHandler(SolrClient solrClient) {
        super(solrClient);
    }

    protected abstract MCRSolrAbstractContentStream<?> getStream();

}
