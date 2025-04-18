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

package org.mycore.solr.auth;

import org.apache.solr.client.solrj.SolrRequest;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.MCRSolrConstants;

import java.net.http.HttpRequest;

/**
 * Interface for adding authentication to Solr requests.
 */
public interface MCRSolrAuthenticationManager {

    /**
     * Add basic authentication to the request, if username and password are configured.
     * Should be thread-safe.
     *
     * @param request the request to add the authentication to
     * @param level   the level of authentication to add
     */
    void applyAuthentication(SolrRequest<?> request, MCRSolrAuthenticationLevel level);

    /**
     * Add basic authentication to the request, if username and password are configured.
     * Should be thread-safe.
     *
     * @param request the request to add the authentication to
     * @param level   the level of authentication to add
     */
    void applyAuthentication(HttpRequest.Builder request, MCRSolrAuthenticationLevel level);

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    static MCRSolrAuthenticationManager getInstance() {
        return obtainInstance();
    }

    static MCRSolrAuthenticationManager obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRSolrAuthenticationManager.class,
            MCRSolrConstants.SOLR_CONFIG_PREFIX + "Server.Auth.Manager.Class");
    }

}
