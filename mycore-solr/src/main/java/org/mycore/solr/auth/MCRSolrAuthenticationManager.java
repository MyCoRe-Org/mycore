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
import java.util.EnumMap;
import java.util.Map;

/**
 * Shared manager for adding authentication to Solr requests.
 */
public final class MCRSolrAuthenticationManager {

    private static final String SOLR_AUTH_PROPERTY_PREFIX = MCRSolrConstants.SOLR_CONFIG_PREFIX + "Server.Auth.";

    private final Map<MCRSolrAuthenticationLevel, MCRSolrAuthenticator> authenticators
        = new EnumMap<>(MCRSolrAuthenticationLevel.class);

    private MCRSolrAuthenticationManager() {
        for (MCRSolrAuthenticationLevel level : MCRSolrAuthenticationLevel.values()) {
            authenticators.put(level, MCRConfiguration2.getInstanceOfOrThrow(MCRSolrAuthenticator.class,
                SOLR_AUTH_PROPERTY_PREFIX + level.getPropertyName() + ".Class"));
        }
    }

    /**
     * Add authentication to a Solr request.
     * @param request the request to add the authentication to
     * @param level the level of authentication to add
     */
    public void applyAuthentication(SolrRequest<?> request, MCRSolrAuthenticationLevel level) {
        authenticators.get(level).applyAuthentication(request);
    }

    /**
     * Add authentication to an HTTP request.
     * @param request the request to add the authentication to
     * @param level the level of authentication to add
     */
    public void applyAuthentication(HttpRequest.Builder request, MCRSolrAuthenticationLevel level) {
        authenticators.get(level).applyAuthentication(request);
    }

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    static MCRSolrAuthenticationManager getInstance() {
        return obtainInstance();
    }

    public static MCRSolrAuthenticationManager obtainInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    private static final class LazyInstanceHolder {
        public static final MCRSolrAuthenticationManager SINGLETON_INSTANCE = new MCRSolrAuthenticationManager();
    }

}
