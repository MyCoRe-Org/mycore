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

    protected Integer commitWithin;

    public MCRSolrAbstractIndexHandler(MCRSolrAbstractContentStream<?> stream) {
        this(stream, MCRSolrServerFactory.getSolrServer());
    }

    public MCRSolrAbstractIndexHandler(MCRSolrAbstractContentStream<?> stream, SolrServer solrServer) {
        this.stream = stream;
        this.solrServer = solrServer;
        this.commitWithin = null;
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

    /**
     * Time in milliseconds solr should index the stream. Null by default,
     * says that solr decide when to commit.
     */
    public void setCommitWithin(Integer commitWithin) {
        this.commitWithin = commitWithin;
    }

    public Integer getCommitWithin() {
        return commitWithin;
    }

}
