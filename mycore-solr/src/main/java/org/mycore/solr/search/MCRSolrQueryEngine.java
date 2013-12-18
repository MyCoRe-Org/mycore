package org.mycore.solr.search;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRDefaultQueryEngine;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.legacy.MCRSolrAdapter;

public class MCRSolrQueryEngine extends MCRDefaultQueryEngine {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrQueryEngine.class);

    public static final String JOIN_PATTERN = "{!join from=returnId to=id}";

    private static MCRSolrAdapter ADAPTER = new MCRSolrAdapter();

    private HashSet<String> joinFields;

    public MCRSolrQueryEngine() {
        super();
        LOGGER.info(MessageFormat.format("Using {0} as QueryEngine", this.getClass().getCanonicalName()));
        joinFields = new HashSet<>(MCRConfiguration.instance().getStrings("MCR.Module-solr.JoinQueryFields"));
    }

    @Override
    protected MCRResults buildResults(@SuppressWarnings("rawtypes") MCRCondition cond, int maxResults,
        List<MCRSortBy> sortBy, boolean addSortData, String index) {
        SolrQuery solrQuery = ADAPTER.getSolrQuery(cond, sortBy, maxResults);
        try {
            return ADAPTER.getResults(solrQuery);
        } catch (SolrServerException e) {
            throw new MCRException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected MCRResults buildCombinedResults(MCRSetCondition cond, List<MCRSortBy> sortBy, boolean not, int maxHits) {
        LOGGER.info("Using the overwritten buildCombinedResults!");
        boolean and = cond instanceof MCRAndCondition;
        HashMap<String, List<MCRCondition>> table = groupConditionsByIndex(cond);
        List<MCRResults> results = new LinkedList<MCRResults>();

        SolrQuery solrRequestQuery = buildMergedSolrQuery(sortBy, not, and, table, maxHits);

        try {
            results.add(ADAPTER.getResults(solrRequestQuery));
        } catch (SolrServerException e) {
            throw new MCRException("Could not get the results!", e);
        }

        LOGGER.info(MessageFormat.format("The generated query is {0}", solrRequestQuery.toString()));
        if (and) {
            return MCRResults.intersect(results.toArray(new MCRResults[results.size()]));
        } else {
            return MCRResults.union(results.toArray(new MCRResults[results.size()]));
        }
    }

    /**
     * Builds SOLR query.
     * 
     * Automatically builds JOIN-Query if content search fields are used in query.
     * @param sortBy sort criteria
     * @param not true, if all conditions should be negated
     * @param and AND or OR connective between conditions  
     * @param table conditions per "content" or "metadata"
     * @param maxHits maximum hits
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static SolrQuery buildMergedSolrQuery(List<MCRSortBy> sortBy, boolean not, boolean and,
        HashMap<String, List<MCRCondition>> table, int maxHits) {
        List<MCRCondition> queryConditions = table.get("metadata");
        MCRCondition combined = buildSubCondition(queryConditions, and, not);
        SolrQuery solrRequestQuery = ADAPTER.getSolrQuery(combined, sortBy, maxHits);

        for (Map.Entry<String, List<MCRCondition>> mapEntry : table.entrySet()) {
            if (!mapEntry.getKey().equals("metadata")) {
                MCRCondition combinedFilterQuery = buildSubCondition(mapEntry.getValue(), and, not);
                SolrQuery filterQuery = ADAPTER.getSolrQuery(combinedFilterQuery, sortBy, maxHits);
                solrRequestQuery.addFilterQuery(JOIN_PATTERN + filterQuery.getQuery());
            }
        }
        return solrRequestQuery;
    }

    @Override
    protected String getIndex(String fieldName) {
        return joinFields.contains(fieldName) ? "content" : "metadata";
    }

}
