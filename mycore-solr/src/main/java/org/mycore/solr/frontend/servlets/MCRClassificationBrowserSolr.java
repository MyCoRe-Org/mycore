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

import java.text.MessageFormat;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.mycore.frontend.servlets.MCRClassificationBrowser2;
import org.mycore.solr.MCRSolrServerFactory;

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

    private static final class MCRSolrQueryAdapter implements MCRQueryAdapter {
        private final String fieldName;

        private String restriction = "";

        private String objectType = "";

        private String category;

        public MCRSolrQueryAdapter(String fieldName) {
            this.fieldName = fieldName;
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
        public void setCategory(String text) {
            this.category = text;
        }

        @Override
        public long getResultCount() {
            String queryString = getQueryAsString();
            LOGGER.debug("query: " + queryString);
            SolrQuery query = new SolrQuery(queryString);
            query.set("rows", 0);
            QueryResponse queryResponse;
            try {
                queryResponse = MCRSolrServerFactory.getSolrServer().query(query);
            } catch (SolrServerException e) {
                LOGGER.warn("Could not query SOLR.", e);
                return -1;
            }
            return queryResponse.getResults().getNumFound();
        }

        @Override
        public String getQueryAsString() {
            return MessageFormat.format("+{0}:\"{1}\"{2}{3}", fieldName, category, objectType, restriction);
        }
    }
}
