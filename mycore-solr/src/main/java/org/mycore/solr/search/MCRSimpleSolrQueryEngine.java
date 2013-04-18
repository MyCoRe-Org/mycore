package org.mycore.solr.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.common.MCRException;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryEngine;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.legacy.MCRSolrAdapter;
import org.mycore.solr.legacy.MCRSolrResults;

public class MCRSimpleSolrQueryEngine implements MCRQueryEngine {

    @Override
    public MCRResults search(MCRQuery query) {
        return this.search(query, false);
    }

    @Override
    public MCRResults search(MCRQuery query, boolean comesFromRemoteHost) {
        MCRSolrAdapter adapter = new MCRSolrAdapter();
        SolrQuery solrRequestQuery = adapter.getSolrQuery(query.getCondition(), query.getSortBy(), query.getMaxResults());
        try {
            SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
            SolrDocumentList solrDocumentList = solrServer.query(solrRequestQuery).getResults();
            return new MCRSolrResults(solrDocumentList);
        } catch (SolrServerException e) {
            throw new MCRException("Could not get the results!", e);
        }
    }

}
