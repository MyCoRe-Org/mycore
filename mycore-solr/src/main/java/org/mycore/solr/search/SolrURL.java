/**
 * 
 */
package org.mycore.solr.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;

/**
 * Convenience class for holding the parameters for the solr search url.
 * 
 * @author shermann
 */
public class SolrURL {
    private static final Logger LOGGER = Logger.getLogger(SolrURL.class);

    public static final String VERSION_2_2 = "2.2";

    private CommonsHttpSolrServer solrServer;

    private String q, fixedURLPart, sortOptions;

    private int start, rows;

    boolean returnScore;

    /**
     * @param solrServer the solr server to use
     */
    public SolrURL(CommonsHttpSolrServer solrServer) {
        this.solrServer = solrServer;
        start = 0;
        rows = 10;
        q = null;
        fixedURLPart = "/select/?version=2.2";
        sortOptions = new String();
        returnScore = false;
    }

    /**
     * @param solrServer
     * @param returnScore specify whether to return the score with results;
     */
    public SolrURL(CommonsHttpSolrServer solrServer, boolean returnScore) {
        this(solrServer);
        this.returnScore = returnScore;
    }

    /**
     * @return a ready to invoke {@link URL} object or null
     */
    public URL getUrl() {
        try {
            return new URL(solrServer.getBaseURL() + fixedURLPart + "&q=" + URLEncoder.encode(q, "UTF-8") + "&start=" + start + "&rows="
                    + rows + "&sort=" + URLEncoder.encode(sortOptions, "UTF-8") + (returnScore ? "&fl=*,score" : ""));
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
            return new URL(solrServer.getBaseURL() + "/admin/luke");
        } catch (MalformedURLException e) {
            LOGGER.error("Error building solr luke url", e);
        }
        return null;
    }

    /**
     * An abbrevation for getUrl().openStream();
     * 
     * @throws IOException
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
        sortOptions += sortBy + " " + order;
    }

    /**
     * Sets the unencoded query parameter.
     * 
     * @param query
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
     * 
     * @param start
     */
    public void setStart(int start) {
        this.start = start;

    }

    /**
     * Sets the rows parameter.
     * 
     * @param rows
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    /**
     * @param yesOrNo
     */
    public void setReturnScore(boolean yesOrNo) {
        this.returnScore = yesOrNo;
    }

    /**
     * @return true if the score is returned with the results, false otherwise
     */
    public boolean returnsScore() {
        return this.returnScore;
    }
}
