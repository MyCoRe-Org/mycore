/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.search;

import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_XML_PROTOCOL_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.HttpSolrClientBase;

/**
 * Convenience class for holding the parameters for the solr search url.
 *
 * @author shermann
 */
public class MCRSolrURL {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String FIXED_URL_PART = new MessageFormat("?version={0}", Locale.ROOT)
        .format(new Object[] { SOLR_QUERY_XML_PROTOCOL_VERSION });

    private HttpSolrClientBase solrClient;

    private String urlQuery;
    private String q;
    private String sortOptions;
    private String wt;

    private int start;
    private int rows;

    boolean returnScore;

    private Optional<String> requestHandler;

    private MCRSolrURL() {
        requestHandler = Optional.empty();
    }

    /**
     * @param solrClient the solr server connection to use
     */
    public MCRSolrURL(HttpSolrClientBase solrClient) {
        this();
        this.solrClient = solrClient;
        start = 0;
        rows = 10;
        q = null;
        wt = null;
        sortOptions = "";
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
    public MCRSolrURL(HttpSolrClientBase solrClient, String urlQuery) {
        this();
        this.solrClient = solrClient;
        this.urlQuery = urlQuery;
    }

    /**
     * @param solrClient the solr server connection to use
     * @param returnScore specify whether to return the score with results;
     */
    public MCRSolrURL(HttpSolrClientBase solrClient, boolean returnScore) {
        this(solrClient);
        this.returnScore = returnScore;
    }

    /**
     * @return a ready to invoke {@link URL} object or null
     */
    public URL getUrl() {
        try {
            if (this.urlQuery == null) {
                return new URI(
                    solrClient.getBaseURL() + getRequestHandler() + FIXED_URL_PART + "&q=" + URLEncoder
                        .encode(q, StandardCharsets.UTF_8)
                        + "&start=" + start
                        + "&rows=" + rows + "&sort=" + URLEncoder.encode(sortOptions, StandardCharsets.UTF_8)
                        + (returnScore ? "&fl=*,score" : "")
                        + (wt != null ? "&wt=" + wt : ""))
                    .toURL();
            } else {
                return new URI(solrClient.getBaseURL() + getRequestHandler() + FIXED_URL_PART + "&" + urlQuery).toURL();
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
            return new URI(solrClient.getBaseURL() + "/admin/luke").toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.error("Error building solr luke url", e);
        }
        return null;
    }

    /**
     * An abbreviation for getUrl().openStream();
     */
    public InputStream openStream() throws IOException {
        URL url = this.getUrl();
        LOGGER.info(url);
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

    /**
     * Sets the solr request handler.
     *
     * @param requestHandler the name of the request handler to set e.g. /foo
     * */
    public void setRequestHandler(String requestHandler) {
        this.requestHandler = Optional.ofNullable(requestHandler);
    }

    /**
     * Returns the current request handler.
     *
     * @return the solr request handler
     * */
    public String getRequestHandler() {
        return this.requestHandler.orElse(SOLR_QUERY_PATH);
    }
}
