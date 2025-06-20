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

import java.net.http.HttpRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrRequest;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.solr.MCRSolrConstants;

/**
 * Shared manager for adding authentication to Solr requests.
 */
public final class MCRSolrAuthenticationManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SOLR_AUTH_PROPERTY_PREFIX = MCRSolrConstants.SOLR_CONFIG_PREFIX + "Server.Auth.";

    private static final String CLASS_SUFFIX = ".Class";

    private final ConcurrentMap<MCRSolrAuthenticationLevel, MCRSolrAuthenticator> authenticators =
        new ConcurrentHashMap<>();

    private MCRSolrAuthenticationManager() {
        for (MCRSolrAuthenticationLevel level : MCRSolrAuthenticationLevel.values()) {
            initAuthenticator(level);
        }
        MCRConfiguration2.addPropertyChangeEventLister(
            key -> key.startsWith(SOLR_AUTH_PROPERTY_PREFIX),
            (key, oldValue, newValue) -> updateAuthenticator(key));
    }

    private void initAuthenticator(MCRSolrAuthenticationLevel level) {
        authenticators.put(level, MCRConfiguration2.getInstanceOfOrThrow(MCRSolrAuthenticator.class,
            SOLR_AUTH_PROPERTY_PREFIX + level.getPropertyName() + CLASS_SUFFIX));
    }

    private void updateAuthenticator(String key) {
        String keySuffix = key.substring(SOLR_AUTH_PROPERTY_PREFIX.length());
        int index = keySuffix.indexOf('.');
        if (index != -1) {
            String levelString = keySuffix.substring(0, index);
            for (MCRSolrAuthenticationLevel level : MCRSolrAuthenticationLevel.values()) {
                if (level.getPropertyName().equals(levelString)) {
                    try {
                        LOGGER.debug("Reinitializing authenticator for level: {}", levelString);
                        initAuthenticator(level);
                    } catch (MCRConfigurationException e) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Failed to reinitialize authenticator for level: " + levelString, e);
                        }
                    }
                    break;
                }
            }
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

    public static MCRSolrAuthenticationManager obtainInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    private static final class LazyInstanceHolder {
        public static final MCRSolrAuthenticationManager SINGLETON_INSTANCE = new MCRSolrAuthenticationManager();
    }

}
