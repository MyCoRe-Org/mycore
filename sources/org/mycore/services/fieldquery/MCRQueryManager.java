/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;

/**
 * Executes queries on all configured searchers and returns query results.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryManager {

    protected static final Logger LOGGER = Logger.getLogger(MCRQueryManager.class);
  
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
    public static MCRResults search(MCRQuery query) {
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
    public static MCRResults search(MCRQuery query, boolean comesFromRemoteHost) {
        long start = System.currentTimeMillis();

        int maxResults = query.getMaxResults();

        // Build results of local query
        final MCRResults results = buildResults(query.getCondition(), maxResults, query.getSortBy(), comesFromRemoteHost);

        // Add results of remote query
        MCRQueryClient.search(query, results);

        // Add missing sort data and sort results, if not already sorted
        sortResults(query, results);

        // After sorting, cut result list to maxResults if needed
        results.cutResults(maxResults);

        long qtime = System.currentTimeMillis() - start;
        LOGGER.debug("total query time: " + qtime);
        
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
    private static void sortResults(MCRQuery query, final MCRResults results) {
        List<MCRSortBy> sortBy = query.getSortBy();
        if ((results.getNumHits() == 0) || results.isSorted() || sortBy.isEmpty())
            return;

        // Iterator over all MCRHits that have no sort data set
        Iterator hitIterator = new Iterator() {
            private int i = 0;

            private int max = results.getNumHits();

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                for (; i < max; i++)
                    if (!results.getHit(i).hasSortData())
                        return true;

                return false;
            }

            public Object next() {
                if (!hasNext())
                    throw new NoSuchElementException();

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
    private final static String mixed = "--mixed--";

    /**
     * Returns the ID of the index of all fields referenced in this condition.
     * If the fields come from multiple indexes, the constant mixed is returned.
     */
    private static String getIndex(MCRCondition cond) {
        if (cond instanceof MCRQueryCondition)
            return ((MCRQueryCondition) cond).getField().getIndex();
        else if (cond instanceof MCRNotCondition)
            return getIndex(((MCRNotCondition) cond).getChild());

        List children = null;
        if (cond instanceof MCRAndCondition)
            children = ((MCRAndCondition) cond).getChildren();
        else
            children = ((MCROrCondition) cond).getChildren();

        String index = getIndex((MCRCondition) (children.get(0)));
        for (int i = 1; i < children.size(); i++) {
            String other = getIndex((MCRCondition) (children.get(i)));
            if (!index.equals(other))
                return mixed; // mixed indexes here!
        }
        return index;
    }

    /** Executes query, if necessary splits into subqueries for each index */
    private static MCRResults buildResults(MCRCondition cond, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        String index = getIndex(cond);
        if (index != mixed) {
            // All fields are from same index, just one searcher
            MCRSearcher searcher = MCRSearcherFactory.getSearcherForIndex(index);
            return searcher.search(cond, maxResults, sortBy, addSortData);
        } else if ((cond instanceof MCRAndCondition) || (cond instanceof MCROrCondition)) {
            return buildCombinedResults(cond, sortBy, false);
        } else { // Move not down: not(a and/or b)=(not a) and/or (not b)
            MCRCondition child = ((MCRNotCondition) cond).getChild();
            return buildCombinedResults(child, sortBy, true);
        }
    }

    /** Split query into subqueries for each index, recombine results */
    private static MCRResults buildCombinedResults(MCRCondition cond, List<MCRSortBy> sortBy, boolean not) {
        boolean and = (cond instanceof MCRAndCondition);
        Hashtable table = groupConditionsByIndex(cond);
        MCRResults totalResults = null;

        for (Enumeration indexes = table.keys(); indexes.hasMoreElements();) {
            List conditions = (List) (table.get(indexes.nextElement()));
            MCRCondition subCond = buildSubCondition(conditions, and, not);

            MCRResults subResults = buildResults(subCond, 0, sortBy, true);

            if (totalResults == null)
                totalResults = subResults;
            else if (and) {
                totalResults.and(subResults);
                if (totalResults.getNumHits() == 0)
                    break; // 0 and ? := 0, we do not need to query the rest
            } else
                totalResults.or(subResults);

        }
        return totalResults;
    }

    /**
     * Build a table from index ID to a List of conditions referencing this
     * index
     */
    private static Hashtable groupConditionsByIndex(MCRCondition cond) {
        Hashtable<String,List<MCRCondition>> table = new Hashtable<String,List<MCRCondition>>();
        List children = null;

        if (cond instanceof MCRAndCondition)
            children = ((MCRAndCondition) cond).getChildren();
        else
            children = ((MCROrCondition) cond).getChildren();

        for (int i = 0; i < children.size(); i++) {
            MCRCondition child = (MCRCondition) (children.get(i));
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
    private static MCRCondition buildSubCondition(List conditions, boolean and, boolean not) {
        MCRCondition subCond;
        if (conditions.size() == 1) {
            subCond = (MCRCondition) (conditions.get(0));
        } else if (and) {
            MCRAndCondition andCond = new MCRAndCondition();
            for (int i = 0; i < conditions.size(); i++)
                andCond.addChild((MCRCondition) (conditions.get(i)));
            subCond = andCond;
        } else { // or
            MCROrCondition orCond = new MCROrCondition();
            for (int i = 0; i < conditions.size(); i++)
                orCond.addChild((MCRCondition) (conditions.get(i)));
            subCond = orCond;
        }
        if (not)
            subCond = new MCRNotCondition(subCond);
        return subCond;
    }
}
