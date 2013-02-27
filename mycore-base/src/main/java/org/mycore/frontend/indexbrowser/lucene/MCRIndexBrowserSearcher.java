package org.mycore.frontend.indexbrowser.lucene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom2.output.XMLOutputter;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRCachedQueryData;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Searcher class for the index browser. Creates a list of MCRIndexBrowserEntries
 * which contains the results of the search.
 * <p>
 * This class is excluded from MCRIndexBrowserData.
 * </p>
 * @author Matthias Eichner
 *
 */
public class MCRIndexBrowserSearcher implements MCRIIndexBrowserSearcher {

    protected static Logger LOGGER = Logger.getLogger(MCRIndexBrowserSearcher.class);

    protected MCRIndexBrowserIncomingData browseData;

    protected MCRIndexBrowserConfig indexConfig;

    protected MCRQuery query;

    protected MCRResults results;

    protected List<MCRIndexBrowserEntry> hitList;

    public MCRIndexBrowserSearcher(MCRIndexBrowserIncomingData browseData, MCRIndexBrowserConfig indexConfig) {
        this.browseData = browseData;
        this.indexConfig = indexConfig;
    }

    /**
     * Starts the search and returns the result list.
     * @return the result list
     */
    public List<MCRIndexBrowserEntry> doSearch() {
        query = buildQuery();
        // for further search and research (by refine and other posibilities
        // the query must be in the Cache
        MCRCachedQueryData qd = MCRCachedQueryData.cache(query, query.buildXML());
        results = qd.getResults();
        LOGGER.debug("Results found hits:" + results.getNumHits());
        hitList = createLinkedListfromSearch();
        return hitList;
    }

    /**
     * Creates the search query for a new index browser
     * request. The result of this query will be cached.
     * @return a new MCRQuery.
     */
    protected MCRQuery buildQuery() {
        MCRCondition cond = buildCondition();
        List<MCRSortBy> sortCriteria = buildSortCriteria();
        MCRQuery query = new MCRQuery(cond, sortCriteria, 0);
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
            LOGGER.debug("Query: \n" + out.outputString(query.buildXML()));
        }
        return query;
    }

    /**
     * Create the condition of the query.
     * @return a new condition.
     */
    protected MCRCondition buildCondition() {
        MCRAndCondition cAnd = new MCRAndCondition();
        String objectProject = "objectProject";
        String objectType = "objectType";
        if (indexConfig.getIndex().contains(",")) {
            MCROrCondition cOr = new MCROrCondition();
            StringTokenizer st = new StringTokenizer(indexConfig.getIndex(), ",");
            while (st.hasMoreTokens()) {
                String next = st.nextToken();
                int ilen = next.indexOf("_");
                if (ilen == -1) {
                    cOr.addChild(new MCRQueryCondition(objectType, "=", next));
                } else {
                    MCRAndCondition iAnd = new MCRAndCondition();
                    iAnd.addChild(new MCRQueryCondition(objectType, "=", next.substring(ilen + 1, next.length())));
                    iAnd.addChild(new MCRQueryCondition(objectProject, "=", next.substring(0, ilen)));
                    cOr.addChild(iAnd);
                }
            }
            cAnd.addChild(cOr);
        } else {
            int ilen = indexConfig.getIndex().indexOf("_");
            if (ilen == -1) {
                cAnd.addChild(new MCRQueryCondition(objectType, "=", indexConfig.getIndex()));
            } else {
                cAnd.addChild(new MCRQueryCondition(objectType, "=", indexConfig.getIndex().substring(ilen + 1,
                        indexConfig.getIndex().length())));
                cAnd.addChild(new MCRQueryCondition(objectProject, "=", indexConfig.getIndex().substring(0, ilen)));
            }
        }
        if (browseData.getSearch() != null && !browseData.getSearch().isEmpty()) {
            String value = browseData.getSearch();
            String operator = getOperator();
            if ("prefix".equals(browseData.getMode()))
                value += "*";
            else if ("like".equals(operator)) {
                if (!value.startsWith("\\*")) {
                    value = "*" + value;
                }
                if (!value.endsWith("\\*")) {
                    value = value + "*";
                }
            }
            cAnd.addChild(new MCRQueryCondition(indexConfig.getBrowseField(), operator, value));
        }
        return cAnd;
    }

    /**
     * Creates the sort criteria of the query.
     * @return a new list of sort criteria.
     */
    protected List<MCRSortBy> buildSortCriteria() {
        boolean order = "ascending".equalsIgnoreCase(indexConfig.getOrder());
        List<MCRSortBy> sortCriteria = new ArrayList<MCRSortBy>();

        for (String sortFieldValue : indexConfig.getSortFields()) {
            MCRFieldDef field = MCRFieldDef.getDef(sortFieldValue);
            if (null != field) {
                sortCriteria.add(new MCRSortBy(field, order));
            } else {
                LOGGER.error("MCRFieldDef not available: " + sortFieldValue);
            }
        }
        return sortCriteria;
    }

    /**
     * Creates a list of MCRIndexBrowserEntries from the results of the search.
     * Each entry has gets the id and the sort values of the mcr hit object.
     * 
     * @return a new list of MCRIndexBrowserEntries
     */
    protected List<MCRIndexBrowserEntry> createLinkedListfromSearch() {
        // at first we must create the full list with all results
        List<MCRIndexBrowserEntry> hitList = new LinkedList<MCRIndexBrowserEntry>();
        String mainFieldName = this.query.getSortBy().get(0).getFieldName();

        for (MCRHit hit : results) {
            MCRIndexBrowserEntry entry = new MCRIndexBrowserEntry();
            List<MCRFieldValue> sortData = hit.getSortData();
            // only necessary if the sort entry list is empty 
            // or the first sort entry differs from the query sort entry
            if (sortData.size() == 0 || !sortData.get(0).getFieldName().equals(mainFieldName)) {
                //main sortfield has no value for this hit
                MCRFieldValue value = new MCRFieldValue(mainFieldName, "???undefined???");
                sortData.add(0, value);
            }
            for (MCRFieldValue aSortData : sortData) {
                entry.addSortValue(aSortData.getValue());
            }
            entry.setObjectId(hit.getID());
            hitList.add(entry);
        }
        return hitList;
    }

    /** The default query condition operator to use if not given in request */
    protected final static String defaultOperator = "like";

    /**
     * Returns the query condition operator to be used. 
     */
    protected String getOperator() {
        if (browseData == null)
            return defaultOperator;

        String mode = browseData.getMode();

        if ("equals".equalsIgnoreCase(mode))
            return "=";
        else if ("prefix".equalsIgnoreCase(mode) || "like".equalsIgnoreCase(mode))
            return "like";
        else if ("contains".equalsIgnoreCase(mode))
            return "contains";
        else
            return defaultOperator;
    }

    /**
     * Returns the final created result list of the cache.
     * @return a list of MCRIndexBrowserEntries
     */
    public List<MCRIndexBrowserEntry> getResultList() {
        return hitList;
    }

}
