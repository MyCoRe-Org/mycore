/**
 * 
 */
package org.mycore.solr.search;

import static org.mycore.solr.MCRSolrConstants.QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.QUERY_XML_PROTOCOL_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

/**
 * Convenience class for holding the parameters for the solr search url.
 * 
 * @author shermann
 */
public class MCRSolrURL {
    private static final Logger LOGGER = LogManager.getLogger(MCRSolrURL.class);

    public static final String FIXED_URL_PART = MessageFormat.format("{0}?version={1}", QUERY_PATH,
        QUERY_XML_PROTOCOL_VERSION);

    private HttpSolrClient solrClient;

    private String urlQuery, q, sortOptions, wt;

    private int start, rows;

    boolean returnScore;

    /**
     * @param solrClient the solr server connection to use
     */
    public MCRSolrURL(HttpSolrClient solrClient) {
        this.solrClient = solrClient;
        start = 0;
        rows = 10;
        q = null;
        wt = null;
        sortOptions = new String();
        returnScore = false;
    }

    /**
     * Creates a new solr url using your own url query. Be aware that you cannot
     * use the MCRSolrURL setter methods to edit your request. Only the urlQuery is
     * used.
     * 
     * @param solrClient the solr server connection to use
     * @param urlQuery e.g. q=allMeta:Hello&amp;rows=20&amp;defType=edismax
     */
    public MCRSolrURL(HttpSolrClient solrClient, String urlQuery) {
        this.solrClient = solrClient;
        this.urlQuery = urlQuery;
    }

    /**
     * @param solrClient the solr server connection to use
     * @param returnScore specify whether to return the score with results;
     */
    public MCRSolrURL(HttpSolrClient solrClient, boolean returnScore) {
        this(solrClient);
        this.returnScore = returnScore;
    }

    /**
     * @return a ready to invoke {@link URL} object or null
     */
    public URL getUrl() {
        try {
            if (this.urlQuery == null) {
                return new URL(
                    solrClient.getBaseURL() + FIXED_URL_PART + "&q=" + URLEncoder.encode(q, "UTF-8") + "&start=" + start
                        + "&rows=" + rows + "&sort=" + URLEncoder.encode(sortOptions, "UTF-8")
                        + (returnScore ? "&fl=*,score" : "")
                        + (wt != null ? "&wt=" + wt : ""));
            } else {
                return new URL(solrClient.getBaseURL() + FIXED_URL_PART + "&" + urlQuery);
            }
        } catch (Exception urlException) {
            LOGGER.error("Error building solr url", urlException);
        }

        return null;
    }

    /**
     * Invoke this method to get a {@link URL} referring to the luke interface of a solr server. 
     * Under this URL one can find useful information about the solr schema. 
     * 
     * @return a {@link URL} refering to the luke interface or null
     */
    public URL getLukeURL() {
        try {
            return new URL(solrClient.getBaseURL() + "/admin/luke");
        } catch (MalformedURLException e) {
            LOGGER.error("Error building solr luke url", e);
        }
        return null;
    }

    /**
     * An abbreviation for getUrl().openStream();
     */
    public InputStream openStream() throws IOException {
        URL url = this.getUrl();
        LOGGER.info(url.toString());
        return url.openStream();
    }

    /**
     * Adds a sort option to the solr url.
     * 
     * @param sortBy the name of the field to sort by
     * @param order the sort order, one can use {@link ORDER#asc} or {@link ORDER#desc} 
     */
    public void addSortOption(String sortBy, String order) {
        if (sortOptions.length() > 0) {
            sortOptions += ",";
        }
        sortOptions += sortBy + " " + (order != null ? order : "desc");
    }

    /**
     * Adds a sort option to the solr url
     * 
     * @param sort the sort option e.g. 'maintitle desc'
     */
    public void addSortOption(String sort) {
        if (sort == null || sort.equals("")) {
            return;
        }
        if (sortOptions.length() > 0) {
            sortOptions += ",";
        }
        sortOptions += sort;
    }

    /**
     * Sets the unencoded query parameter.
     */
    public void setQueryParamter(String query) {
        this.q = query;
    }

    /**
     * @return the query parameter
     */
    public String getQueryParamter() {
        return this.q;
    }

    /**
     * @return the start parameter
     */
    public int getStart() {
        return start;
    }

    /**
     * @return the rows parameter
     */
    public int getRows() {
        return rows;
    }

    /**
     * Sets the start parameter.
     */
    public void setStart(int start) {
        this.start = start;

    }

    /**
     * Sets the rows parameter.
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setReturnScore(boolean yesOrNo) {
        this.returnScore = yesOrNo;
    }

    /**
     * The wt (writer type) parameter is used by Solr to determine which QueryResponseWriter should be
     * used to process the request. Valid values are any of the names specified by &lt;queryResponseWriter... /&gt;
     * declarations in solrconfig.xml. The default value is "standard" (xml).
     */
    public void setWriterType(String wt) {
        this.wt = wt;
    }

    /**
     * @return true if the score is returned with the results, false otherwise
     */
    public boolean returnsScore() {
        return this.returnScore;
    }
}
