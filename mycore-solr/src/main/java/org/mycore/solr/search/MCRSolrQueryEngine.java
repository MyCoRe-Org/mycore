package org.mycore.solr.search;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.common.MCRException;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRDefaultQueryEngine;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.legacy.MCRLuceneSolrAdapter;

public class MCRSolrQueryEngine extends MCRDefaultQueryEngine {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrQueryEngine.class);
    
    private static final String JOIN_PATTERN = "{!join from=returnId to=id}";
    
    public MCRSolrQueryEngine() {
        super();
        LOGGER.info(MessageFormat.format("Using {0} as QueryEngine", this.getClass().getCanonicalName()));
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
            results.add(MCRLuceneSolrAdapter.getResults(solrRequestQuery));
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

    @SuppressWarnings("rawtypes")
    public static SolrQuery buildMergedSolrQuery(List<MCRSortBy> sortBy, boolean not, boolean and, HashMap<String, List<MCRCondition>> table, int maxHits) {
        List<MCRCondition> queryConditions = table.get("metadata");
        MCRCondition combined = buildSubCondition(queryConditions, and, not);
        SolrQuery solrRequestQuery = MCRLuceneSolrAdapter.getSolrQuery(combined, sortBy, maxHits); 
        
        
        for (Map.Entry<String, List<MCRCondition>> mapEntry : table.entrySet()) {
            if(!mapEntry.getKey().equals("metadata")){
                MCRCondition combinedFilterQuery = buildSubCondition(mapEntry.getValue(), and, not);
                SolrQuery filterQuery = MCRLuceneSolrAdapter.getSolrQuery(combinedFilterQuery, sortBy, maxHits); 
                solrRequestQuery.addFilterQuery(JOIN_PATTERN + filterQuery.getQuery());
            }
        }
        return solrRequestQuery;
    }
 
}
