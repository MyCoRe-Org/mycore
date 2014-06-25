package org.mycore.solr.index.handlers.stream;


import org.apache.solr.client.solrj.SolrServer;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.cs.MCRSolrAbstractContentStream;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;

public abstract class MCRSolrAbstractStreamIndexHandler extends MCRSolrAbstractIndexHandler implements MCRSolrIndexHandler {

    protected MCRSolrAbstractContentStream<?> stream;

    protected MCRSolrAbstractStreamIndexHandler() {
        this(null);
    }

    public MCRSolrAbstractStreamIndexHandler(MCRSolrAbstractContentStream<?> stream) {
        this(stream, MCRSolrServerFactory.getSolrServer());
    }

    public MCRSolrAbstractStreamIndexHandler(MCRSolrAbstractContentStream<?> stream, SolrServer solrServer) {
        this.stream = stream;
        this.solrServer = solrServer;
        this.commitWithin = -1;
    }

    public MCRSolrAbstractContentStream<?> getStream() {
        return this.stream;
    }

    @Override
    public String toString() {
        return getStream().getSourceInfo();
    }

}
