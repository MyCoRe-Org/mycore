package org.mycore.solr.legacy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.search.BooleanQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.MCRSolrServerFactory;

public class MCRSolrAdapter {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrAdapter.class);

    static {
        BooleanQuery.setMaxClauseCount(10000);
    }

    @SuppressWarnings("rawtypes")
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        MCRSolrResults solrResults = null;
        if (maxResults == 0) {
            LOGGER.debug("maxResults should be explicitly set. Try to use paging.");
        }
        try {
            SolrQuery q = getSolrQuery(condition, sortBy, maxResults);
            solrResults = getResults(q);
            LOGGER.debug(solrResults.getNumHits() + " document(s) found");
        } catch (Exception e) {
            LOGGER.error("Exception in while processing legacy lucene query:", e);
        }
        return solrResults != null ? solrResults : new MCRResults();
    }

    public SolrQuery getSolrQuery(@SuppressWarnings("rawtypes") MCRCondition condition, List<MCRSortBy> sortBy, int maxResults) {
        String queryString = getQueryString(condition);
        SolrQuery q = applySortOptions(new SolrQuery(queryString), sortBy);
        q.setIncludeScore(true);
        q.setRows(maxResults == 0 ? Integer.MAX_VALUE : maxResults);
    
        String sort = q.getSortField();
        LOGGER.info("Legacy Query transformed by " + getClass().getName() + " to: " + q.getQuery() + (sort != null ? " " + sort : ""));
        return q;
    }

    protected String getQueryString(@SuppressWarnings("rawtypes") MCRCondition condition) {
        Set<String> usedFields=new HashSet<>();
        String queryString = MCRConditionTransformer.toSolrQueryString(condition, usedFields);
        return queryString;
    }

    public MCRSolrResults getResults(SolrQuery q) throws SolrServerException {
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        SolrDocumentList solrDocumentList = solrServer.query(q).getResults();
        return new MCRSolrResults(solrDocumentList);
    }

    /**
     * @param q
     * @param sortBy
     * @return
     */
    protected SolrQuery applySortOptions(SolrQuery q, List<MCRSortBy> sortBy) {
        for (MCRSortBy option : sortBy) {
            SortClause sortClause = new SortClause(option.getFieldName(), option.getSortOrder() ? ORDER.asc
                : ORDER.desc);
            q.addSort(sortClause);
        }
        return q;
    }
}
