/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.mycore.frontend.servlets.MCRClassificationBrowser2.MCRQueryAdapter;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;

public class MCRSolrQueryAdapter implements MCRQueryAdapter {
    private static final Logger LOGGER = LogManager.getLogger(MCRSolrQueryAdapter.class);

    private String fieldName;

    private String restriction = "";

    private String objectType = "";

    private String category;

    private boolean filterCategory;

    private SolrQuery solrQuery;

    public void filterCategory(boolean filter) {
        this.filterCategory = filter;
    }

    @Override
    public void setRestriction(String text) {
        this.restriction = " " + text;
    }

    @Override
    public void setObjectType(String text) {
        this.objectType = " +objectType:" + text;
    }

    @Override
    public String getObjectType() {
        return this.objectType.isEmpty() ? null : this.objectType.split(":")[1];
    }

    @Override
    public void setCategory(String text) {
        this.category = text;
    }

    @Override
    public long getResultCount() {
        configureSolrQuery();
        LOGGER.debug("query: {}", solrQuery);
        solrQuery.set("rows", 0);
        QueryResponse queryResponse;
        try {
            queryResponse = MCRSolrClientFactory.getSolrClient().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            LOGGER.warn("Could not query SOLR.", e);
            return -1;
        }
        return queryResponse.getResults().getNumFound();
    }

    @Override
    public String getQueryAsString() throws UnsupportedEncodingException {
        configureSolrQuery();
        return solrQuery.toString();
    }

    public void prepareQuery() {
        this.solrQuery = new SolrQuery();
    }

    private void configureSolrQuery() {
        this.solrQuery.clear();
        String queryString = filterCategory ? MessageFormat.format("{0}{1}", objectType, restriction)
            : MessageFormat
                .format("+{0}:\"{1}\"{2}{3}", fieldName, category, objectType, restriction);
        this.solrQuery.setQuery(queryString.trim());
        if (filterCategory) {
            solrQuery.setFilterQueries(MessageFormat.format("{0}+{1}:\"{2}\"", MCRSolrConstants.JOIN_PATTERN,
                fieldName, category));
        }
    }

    @Override
    public void setFieldName(String fieldname) {
        this.fieldName = fieldname;
    }

    @Override
    public void configure(HttpServletRequest request) {
        String objectType = request.getParameter("objecttype");
        if ((objectType != null) && (objectType.trim().length() > 0)) {
            setObjectType(objectType);
        }
        String restriction = request.getParameter("restriction");
        if ((restriction != null) && (restriction.trim().length() > 0)) {
            setRestriction(restriction);
        }
        boolean filter = "true".equals(MCRServlet.getProperty(request, "filterCategory"));
        filterCategory(filter);
        prepareQuery();
    }
}
