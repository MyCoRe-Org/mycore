/**
 * 
 */
package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.mycore.common.MCRBaseClass;
import org.mycore.common.MCRUsageException;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRFalseCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.parsers.bool.MCRTrueCondition;

public class MCRDefaultQueryEngine extends MCRBaseClass implements MCRQueryEngine {
    /**
     * Executes a query and returns the query results. If the query contains
     * fields from different indexes or should span across multiple hosts, the
     * results of multiple searchers are combined.
     * 
     * @param query
     *            the query
     * 
     * @return the query results
     */
    public MCRResults search(MCRQuery query) {
        return search(query, false);
    }

    /**
     * Executes a query and returns the query results. If the query contains
     * fields from different indexes or should span across multiple hosts, the
     * results of multiple searchers are combined.
     * 
     * @param query
     *            the query
     * @param comesFromRemoteHost
     *            if true, this query is originated from a remote host, so no
     *            sorting of results is done for performance reasons
     * 
     * @return the query results
     */
    public MCRResults search(MCRQuery query, boolean comesFromRemoteHost) {
        long start = System.currentTimeMillis();

        int maxResults = query.getMaxResults();

        // Build results of local query
        getLOGGER().info("Query: " + query.getCondition().toString());
        MCRResults results = buildResults(query.getCondition(), maxResults, query.getSortBy(), comesFromRemoteHost);
        if (results.isReadonly() && !query.getHosts().isEmpty()) {
            //need to produce a mergeable MCRResults
            MCRResults mResults = new MCRResults();
            for (MCRHit hit : results) {
                mResults.addHit(hit);
            }
            results = mResults;
        }

        // Add results of remote query
        MCRQueryClient.search(query, results);

        // Add missing sort data and sort results, if not already sorted
        sortResults(query, results);

        // After sorting, cut result list to maxResults if needed
        results.cutResults(maxResults);

        long qtime = System.currentTimeMillis() - start;
        getLOGGER().debug("total query time: " + qtime);

        return results;
    }

    /**
     * Sorts the results if not already done and if the query contains sort
     * criteria. Data needed for sorting is automatically added to the hits if
     * not present.
     * 
     * @param query
     *            the original query
     * @param results
     *            the result list to be sorted
     */
    private void sortResults(MCRQuery query, final MCRResults results) {
        List<MCRSortBy> sortBy = query.getSortBy();
        if (results.getNumHits() == 0 || results.isSorted() || sortBy.isEmpty()) {
            return;
        }

        // Iterator over all MCRHits that have no sort data set
        Iterator<MCRHit> hitIterator = new Iterator<MCRHit>() {
            private int i = 0;

            private int max = results.getNumHits();

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                for (; i < max; i++) {
                    if (!results.getHit(i).hasSortData()) {
                        return true;
                    }
                }

                return false;
            }

            public MCRHit next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return results.getHit(i++);
            }
        };

        String index = sortBy.get(0).getField().getIndex();
        MCRSearcher searcher = MCRSearcherFactory.getSearcherForIndex(index);
        searcher.addSortData(hitIterator, sortBy);
        results.sortBy(query.getSortBy());
    }

    /**
     * If a condition references fields from multiple indexes, this constant is
     * returned
     */
    protected static final String mixed = "--mixed--";

    /**
     * Returns the ID of the index of all fields referenced in this condition.
     * If the fields come from multiple indexes, the constant mixed is returned.
     */
    protected static String getIndex(MCRCondition cond) {
        if (cond instanceof MCRQueryCondition) {
            MCRQueryCondition queryCondition = ((MCRQueryCondition) cond);
            return MCRFieldDef.getDef(queryCondition.getFieldName()).getIndex();
        } else if (cond instanceof MCRNotCondition) {
            return getIndex(((MCRNotCondition) cond).getChild());
        }

        List<MCRCondition> children = ((MCRSetCondition) cond).getChildren();

        String index = getIndex(children.get(0));
        for (int i = 1; i < children.size(); i++) {
            String other = getIndex(children.get(i));
            if (!index.equals(other)) {
                return mixed; // mixed indexes here!
            }
        }
        return index;
    }

    /** Executes query, if necessary splits into subqueries for each index */
    protected MCRResults buildResults(MCRCondition cond, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        checkCondition(cond);
        String index = getIndex(cond);
        if (index != mixed) {
            return buildResults(cond, maxResults, sortBy, addSortData, index);
        } else if (cond instanceof MCRSetCondition) {
            return buildCombinedResults((MCRSetCondition) cond, sortBy, false, maxResults);
        } else { // Move not down: not(a and/or b)=(not a) and/or (not b)
            MCRCondition child = ((MCRNotCondition) cond).getChild();
            return buildCombinedResults((MCRSetCondition) child, sortBy, true, maxResults);
        }
    }

    protected MCRResults buildResults(MCRCondition cond, int maxResults, List<MCRSortBy> sortBy, boolean addSortData,
        String index) {
        // All fields are from same index, just one searcher
        MCRSearcher searcher = MCRSearcherFactory.getSearcherForIndex(index);
        // Filter sort criteria only for those fields of the same index
        List<MCRSortBy> sortByCopy = new ArrayList<MCRSortBy>();
        for (MCRSortBy sb : sortBy) {
            if (sb.getField().getIndex().equals(index)) {
                sortByCopy.add(sb);
            }
        }
        return searcher.search(cond, maxResults, sortByCopy, addSortData);
    }

    /**
     * Checks a condition makes sense. 
     * @param cond the condition to check.
     * @throws MCRUsageException if the condition makes no sense
     */
    protected void checkCondition(MCRCondition cond) throws MCRUsageException {
        if (cond instanceof MCRTrueCondition || cond instanceof MCRFalseCondition) {
            String msg = "Your query makes no sense. What do you mean when you search for '" + cond.toString() + "'?";
            throw new MCRUsageException(msg);
        }
    }

    /** Split query into subqueries for each index, recombine results 
     * @param maxResults TODO*/
    protected MCRResults buildCombinedResults(MCRSetCondition cond, List<MCRSortBy> sortBy, boolean not, int maxResults) {
        boolean and = cond instanceof MCRAndCondition;
        HashMap<String, List<MCRCondition>> table = groupConditionsByIndex(cond);
        List<MCRResults> results = new LinkedList<MCRResults>();

        for (Map.Entry<String, List<MCRCondition>> mapEntry : table.entrySet()) {
            List<MCRCondition> conditions = mapEntry.getValue();
            String index = mapEntry.getKey();
            if (!index.equals(mixed)) {
                MCRCondition subCond = buildSubCondition(conditions, and, not);
                results.add(buildResults(subCond, 0, sortBy, true));
            } else {
                for (MCRCondition subCond : conditions) {
                    if (not) {
                        subCond = new MCRNotCondition(subCond);
                    }
                    results.add(buildResults(subCond, 0, sortBy, true));
                }
            }
        }

        if (and) {
            return MCRResults.intersect(results.toArray(new MCRResults[results.size()]));
        } else {
            return MCRResults.union(results.toArray(new MCRResults[results.size()]));
        }
    }

    /**
     * Build a table from index ID to a List of conditions referencing this
     * index
     */
    public static HashMap<String, List<MCRCondition>> groupConditionsByIndex(MCRSetCondition cond) {
        HashMap<String, List<MCRCondition>> table = new HashMap<String, List<MCRCondition>>();
        List<MCRCondition> children = cond.getChildren();

        for (MCRCondition child : children) {
            String index = getIndex(child);
            List<MCRCondition> conditions = table.get(index);
            if (conditions == null) {
                conditions = new ArrayList<MCRCondition>();
                table.put(index, conditions);
            }
            conditions.add(child);
        }
        return table;
    }

    /** Builds a new condition for all fields from one single index */
    protected static MCRCondition buildSubCondition(List<MCRCondition> conditions, boolean and, boolean not) {
        MCRCondition subCond;
        if (conditions.size() == 1) {
            subCond = conditions.get(0);
        } else if (and) {
            subCond = new MCRAndCondition().addAll(conditions);
        } else {
            subCond = new MCROrCondition().addAll(conditions);
        }
        if (not) {
            subCond = new MCRNotCondition(subCond);
        }
        return subCond;
    }
}