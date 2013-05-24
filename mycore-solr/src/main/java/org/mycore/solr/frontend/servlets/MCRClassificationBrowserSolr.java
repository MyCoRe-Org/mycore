/*
* $Revision: 23924 $ $Date: 2012-03-20 10:29:36 +0100 (Tue, 20 Mar 2012) $
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

package org.mycore.solr.frontend.servlets;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.mycore.frontend.servlets.MCRClassificationBrowser2;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.search.MCRSolrQueryEngine;

/**
 * This servlet provides a way to visually navigate through the tree of
 * categories of a classification. The XML output is transformed to HTML
 * using classificationBrowserData.xsl on the server side, then sent to
 * the client browser, where AJAX does the rest.
 *
 * 
 * @author Thomas Scheffler 
 */
public class MCRClassificationBrowserSolr extends MCRClassificationBrowser2 {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRClassificationBrowserSolr.class);

    @Override
    protected MCRQueryAdapter getQueryAdapter(final String fieldName) {
        return new MCRSolrQueryAdapter(fieldName);
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRClassificationBrowser2#configureQueryAdapter(org.mycore.frontend.servlets.MCRClassificationBrowser2.MCRQueryAdapter, javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected void configureQueryAdapter(MCRQueryAdapter queryAdapter, HttpServletRequest req) {
        super.configureQueryAdapter(queryAdapter, req);
        MCRSolrQueryAdapter solrQueryAdapter = (MCRSolrQueryAdapter) queryAdapter;
        boolean filter = "true".equals(getProperty(req, "filterCategory"));
        solrQueryAdapter.filterCategory(filter);
        solrQueryAdapter.prepareQuery();
    }

    private static final class MCRSolrQueryAdapter implements MCRQueryAdapter {
        private final String fieldName;

        private String restriction = "";

        private String objectType = "";

        private String category;

        private boolean filterCategory;

        private SolrQuery solrQuery;

        public MCRSolrQueryAdapter(String fieldName) {
            this.fieldName = fieldName;
        }

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
            LOGGER.debug("query: " + solrQuery.toString());
            solrQuery.set("rows", 0);
            QueryResponse queryResponse;
            try {
                queryResponse = MCRSolrServerFactory.getSolrServer().query(solrQuery);
            } catch (SolrServerException e) {
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
            String queryString = filterCategory ? MessageFormat.format("{0}{1}", objectType, restriction) : MessageFormat.format(
                    "+{0}:\"{1}\"{2}{3}", fieldName, category, objectType, restriction);
            this.solrQuery.setQuery(queryString.trim());
            if (filterCategory) {
                solrQuery.setFilterQueries(MessageFormat.format("{0}+{1}:\"{2}\"", MCRSolrQueryEngine.JOIN_PATTERN, fieldName, category));
            }
        }
    }
}
