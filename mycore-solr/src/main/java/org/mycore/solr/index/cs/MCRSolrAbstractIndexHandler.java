package org.mycore.solr.index.cs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.solr.MCRSolrServerFactory;

public abstract class MCRSolrAbstractIndexHandler implements MCRSolrIndexHandler {

    protected SolrServer solrServer;

    protected MCRSolrAbstractContentStream<?> stream;

    public MCRSolrAbstractIndexHandler(MCRSolrAbstractContentStream<?> stream) {
        this(stream, MCRSolrServerFactory.getSolrServer());
    }

    public MCRSolrAbstractIndexHandler(MCRSolrAbstractContentStream<?> stream, SolrServer solrServer) {
        this.stream = stream;
        this.solrServer = solrServer;
    }

    public MCRSolrAbstractContentStream<?> getStream() {
        return this.stream;
    }

    public SolrServer getSolrServer() {
        return this.solrServer;
    }

    public abstract void index() throws IOException, SolrServerException;

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return getStream().getSourceInfo();
    }

}
