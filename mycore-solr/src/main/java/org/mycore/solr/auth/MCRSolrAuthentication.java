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

package org.mycore.solr.auth;import org.apache.http.client.methods.HttpRequestBase;
import org.apache.solr.client.solrj.SolrRequest;

/**
 * Interface for adding authentication to Solr requests.
 */
public interface MCRSolrAuthentication {

    /**
     * Set the authentication level for this instance.
     * @param level the level to set
     */
    void setLevel(MCRSolrAuthenticationLevel level);

    /**
     * Add authentication to a Solr request.
     * @param request the request to add authentication to
     */
    void applyAuthentication(SolrRequest<?> request);

    /**
     * Add authentication to an HTTP request.
     * @param request the request to add authentication to
     */
    void applyAuthentication(HttpRequestBase request);

}
