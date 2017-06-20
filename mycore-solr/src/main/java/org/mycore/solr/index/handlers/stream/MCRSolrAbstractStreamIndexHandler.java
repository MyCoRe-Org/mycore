package org.mycore.solr.index.handlers.stream;

import org.apache.solr.client.solrj.SolrClient;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.cs.MCRSolrAbstractContentStream;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;

public abstract class MCRSolrAbstractStreamIndexHandler extends MCRSolrAbstractIndexHandler
    implements MCRSolrIndexHandler {

    protected MCRSolrAbstractContentStream<?> stream;

    protected MCRSolrAbstractStreamIndexHandler() {
        this(null);
    }

    public MCRSolrAbstractStreamIndexHandler(MCRSolrAbstractContentStream<?> stream) {
        this(stream, MCRSolrClientFactory.getSolrClient());
    }

    public MCRSolrAbstractStreamIndexHandler(MCRSolrAbstractContentStream<?> stream, SolrClient solrClient) {
        this.stream = stream;
        this.solrClient = solrClient;
        this.commitWithin = -1;
    }

    public MCRSolrAbstractContentStream<?> getStream() {
        return this.stream;
    }

    @Override
    public String toString() {
        return "index " + getStream().getName();
    }

}
