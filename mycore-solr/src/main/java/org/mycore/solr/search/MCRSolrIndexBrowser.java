/**
 * 
 */
package org.mycore.solr.search;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.frontend.indexbrowser.lucene.MCRIIndexBrowserSearcher;
import org.mycore.frontend.indexbrowser.lucene.MCRIndexBrowserConfig;
import org.mycore.frontend.indexbrowser.lucene.MCRIndexBrowserEntry;
import org.mycore.frontend.indexbrowser.lucene.MCRIndexBrowserIncomingData;
import org.mycore.solr.MCRSolrServerFactory;

/**
 * @author shermann
 *
 */
public class MCRSolrIndexBrowser implements MCRIIndexBrowserSearcher {

    protected static final Logger LOGGER = Logger.getLogger(MCRSolrIndexBrowser.class);

    private MCRIndexBrowserIncomingData browseData;

    private MCRIndexBrowserConfig indexConfig;

    private List<MCRIndexBrowserEntry> results;

    final static String DEFAULT_OPERATOR = "like";

    /**
     * @param browseData
     * @param indexConfig
     */
    public MCRSolrIndexBrowser(MCRIndexBrowserIncomingData browseData, MCRIndexBrowserConfig indexConfig) {
        this.browseData = browseData;
        this.indexConfig = indexConfig;
        results = new LinkedList<MCRIndexBrowserEntry>();
    }

    public List<MCRIndexBrowserEntry> doSearch() {
        String q = this.buildSorlQuery();
        SolrQuery solrQuery = new SolrQuery(q);
        solrQuery.setStart(0);
        solrQuery.setRows(20);

        try {
            SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
            QueryResponse queryResponse = solrServer.query(solrQuery);
            SolrDocumentList docs = queryResponse.getResults();
            results = createResultList(docs);
        } catch (SolrServerException solrServerException) {
            LOGGER.error("Error executing query ", solrServerException);
        }

        return results;
    }

    /**
     * Creates a list of MCRIndexBrowserEntries from the results of the search.
     * Each entry has gets the id and the sort values of the mcr hit object.
     * 
     * @param docs 
     * 
     * @return a new list of MCRIndexBrowserEntries
     */
    private List<MCRIndexBrowserEntry> createResultList(SolrDocumentList docs) {
        // at first we must create the full list with all results
        List<MCRIndexBrowserEntry> hitList = new LinkedList<MCRIndexBrowserEntry>();

        for (SolrDocument hit : docs) {
            MCRIndexBrowserEntry entry = new MCRIndexBrowserEntry();
            entry.setObjectId(hit.get("id").toString());
            hitList.add(entry);
        }
        return hitList;
    }

    /**
     * Creates a query string suitable as input for a {@link SolrQuery}
     * 
     * @return the ready to use query string
     */
    private String buildSorlQuery() {
        String q = new String();
        if (indexConfig.getIndex().contains(",")) {
            StringTokenizer st = new StringTokenizer(indexConfig.getIndex(), ",");
            while (st.hasMoreTokens()) {
                String next = st.nextToken();
                int ilen = next.indexOf("_");
                if (ilen == -1) {
                    q += "object_type:" + next;

                } else {
                    String and = new String();

                    and += " (object_type:" + next.substring(ilen + 1, next.length());
                    and += " AND object_project:" + next.substring(0, ilen) + ")";

                    q += and;
                }

                if (st.hasMoreTokens()) {
                    q += " OR ";
                }
            }
        } else {
            int ilen = indexConfig.getIndex().indexOf("_");
            if (q.length() > 0) {
                q += " AND ";
            }
            if (ilen == -1) {
                q += "object_type:" + indexConfig.getIndex();
            } else {
                q += "object_type:" + indexConfig.getIndex().substring(ilen + 1, indexConfig.getIndex().length());
                q += " AND ";
                q += "object_project:" + indexConfig.getIndex().substring(0, ilen);
            }
        }

        if (browseData.getSearch() != null && !browseData.getSearch().isEmpty()) {
            String searchTerm = browseData.getSearch();

            q += " AND ";
            q += indexConfig.getBrowseField() + ":" + searchTerm;
            q = "(" + q + ")";
            q += " OR (object_type:" + indexConfig.getIndex() + " AND (gnd:*" + searchTerm + "* OR ppn:*" + searchTerm + "*))";
        }

        LOGGER.info(q);
        return q;
    }

    /**
     * Returns the query condition operator to be used. 
     */
    protected String getOperator() {
        if (browseData == null)
            return DEFAULT_OPERATOR;
        String mode = browseData.getMode();

        if ("equals".equalsIgnoreCase(mode))
            return "=";
        else if ("prefix".equalsIgnoreCase(mode) || "like".equalsIgnoreCase(mode))
            return "like";
        else if ("contains".equalsIgnoreCase(mode))
            return "contains";
        else
            return DEFAULT_OPERATOR;
    }

    public List<MCRIndexBrowserEntry> getResultList() {
        return results;
    }
}
