package org.mycore.frontend.indexbrowser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.output.XMLOutputter;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRCachedQueryData;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
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
public class MCRIndexBrowserSearcher {

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
        results = search();
        LOGGER.debug("Results found hits:" + results.getNumHits());
        hitList = createLinkedListfromSearch();
        // for further search and research (by refine and other posibilities
        // the query must be in the Cache
        MCRCachedQueryData.cache(results, query.buildXML(), query.getCondition());
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
        if(LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
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
        MCRFieldDef fieldproject;
        MCRFieldDef fieldtype;
        if (indexConfig.getTable().indexOf(",") != -1) {
            MCROrCondition cOr = new MCROrCondition();
            StringTokenizer st = new StringTokenizer(indexConfig.getTable(), ",");
            while (st.hasMoreTokens()) {
                String next = st.nextToken();
                int ilen = next.indexOf("_");
                if (ilen == -1) {
                    fieldtype = MCRFieldDef.getDef("objectType");
                    cOr.addChild(new MCRQueryCondition(fieldtype, "=", next));
                } else {
                    MCRAndCondition iAnd = new MCRAndCondition();
                    fieldtype = MCRFieldDef.getDef("objectType");
                    iAnd.addChild(new MCRQueryCondition(fieldtype, "=", next.substring(ilen + 1, next.length())));
                    fieldproject = MCRFieldDef.getDef("objectProject");
                    iAnd.addChild(new MCRQueryCondition(fieldproject, "=", next.substring(0, ilen)));
                    cOr.addChild(iAnd);
                }
            }
            cAnd.addChild(cOr);
        } else {
            int ilen = indexConfig.getTable().indexOf("_");
            if (ilen == -1) {
                fieldtype = MCRFieldDef.getDef("objectType");
                cAnd.addChild(new MCRQueryCondition(fieldtype, "=", indexConfig.getTable()));
            } else {
                fieldtype = MCRFieldDef.getDef("objectType");
                cAnd.addChild(new MCRQueryCondition(fieldtype, "=", indexConfig.getTable().substring(ilen + 1, indexConfig.getTable().length())));
                fieldproject = MCRFieldDef.getDef("objectProject");
                cAnd.addChild(new MCRQueryCondition(fieldproject, "=", indexConfig.getTable().substring(0, ilen)));
            }
        }
        if (browseData.getSearch() != null && browseData.getSearch().length() > 0) {
            MCRFieldDef field = MCRFieldDef.getDef(indexConfig.getBrowseField());
            String value = browseData.getSearch();
            String operator = getOperator();
            cAnd.addChild(new MCRQueryCondition(field, operator, value));
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
            if (null != field)
                sortCriteria.add(new MCRSortBy(field, order));
            else
                LOGGER.error("MCRFieldDef not available: " + sortFieldValue);
        }
        return sortCriteria;
    }

    protected MCRResults search() {
        return MCRQueryManager.search(query);
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

        for (MCRHit hit : results) {
            MCRIndexBrowserEntry entry = new MCRIndexBrowserEntry();

            List<MCRFieldValue> sortData = hit.getSortData();
            // only necessary if the sort entry list is empty 
            // or the first sort entry differs from the query sort entry 
            MCRFieldDef mainSortField = query.getSortBy().get(0).getField();
            if (sortData.size() == 0 || !sortData.get(0).getField().equals(mainSortField)) {
                //main sortfield has no value for this hit
                MCRFieldValue value = new MCRFieldValue(mainSortField, "???undefined???");
                sortData.add(0, value);
            }
            Iterator<MCRFieldValue> it = sortData.iterator();
            while(it.hasNext()) {
                entry.addSortValue(it.next().getValue());
            }
            entry.setObjectId(hit.getID());
            hitList.add(entry);
        }
        return hitList;
    }

    /**
     * Returns the lucene search operator as String to be used doing a lucene
     * query. This will be taken from MyBrowseData.mode; If MyBrowseData.mode ==
     * "prefix" -> return "like", If MyBrowseData.mode == "equals" -> return
     * "=", Else return "like"
     * 
     * @return The lucene search operator as String
     * 
     */
    protected String getOperator() {
        if (browseData != null && browseData.getMode() != null && browseData.getMode().equalsIgnoreCase("equals"))
            return "=";
        else if (browseData != null && browseData.getMode() != null && browseData.getMode().equalsIgnoreCase("prefix"))
            return "like";
        else
            return "like";
    }

    /**
     * Returns the final created result list of the cache.
     * @return a list of MCRIndexBrowserEntries
     */
    public List<MCRIndexBrowserEntry> getResultList() {
        return hitList;
    }
    
}
