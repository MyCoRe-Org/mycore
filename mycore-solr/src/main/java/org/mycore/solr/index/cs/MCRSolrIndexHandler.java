package org.mycore.solr.index.cs;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

public interface MCRSolrIndexHandler {

    public void index() throws IOException, SolrServerException;

}
