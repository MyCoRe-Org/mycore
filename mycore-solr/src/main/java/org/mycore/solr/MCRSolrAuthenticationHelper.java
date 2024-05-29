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

package org.mycore.solr;

import java.util.Base64;
import java.util.Optional;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.solr.client.solrj.SolrRequest;
import org.mycore.common.config.MCRConfiguration2;

public class MCRSolrAuthenticationHelper {

    public enum AuthenticationLevel {
        ADMIN("Admin"), INDEX("Index"), SEARCH("Search");

        private final String propertyName;

        AuthenticationLevel(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }

    private static final String SOLR_AUTH_PROPERTY_PREFIX = MCRSolrConstants.SOLR_CONFIG_PREFIX + "Server.Auth.";

    private static final String SOLR_USERNAME_PROPERTY_SUFFIX = ".Username";

    private static final String SOLR_PASSWORD_PROPERTY_SUFFIX = ".Password";

    public static void addAuthentication(SolrRequest<?> request, AuthenticationLevel level) {
        Optional<String> userNameOpt = MCRConfiguration2.getString(SOLR_AUTH_PROPERTY_PREFIX +
            level.getPropertyName() + SOLR_USERNAME_PROPERTY_SUFFIX);

        Optional<String> passwordOpt = MCRConfiguration2.getString(SOLR_AUTH_PROPERTY_PREFIX +
            level.getPropertyName() + SOLR_PASSWORD_PROPERTY_SUFFIX);

        if (userNameOpt.isEmpty() || passwordOpt.isEmpty()) {
            return;
        }

        request.setBasicAuthCredentials(userNameOpt.get(), passwordOpt.get());
    }

    /**
     * Add basic authentication to the request, if username and password are configured.
     * @param request the request to add the authentication to
     */
    public static void addAuthentication(HttpRequestBase request, AuthenticationLevel level) {
        Optional<String> userNameOpt = MCRConfiguration2.getString(SOLR_AUTH_PROPERTY_PREFIX +
            level.getPropertyName() + SOLR_USERNAME_PROPERTY_SUFFIX);

        Optional<String> passwordOpt = MCRConfiguration2.getString(SOLR_AUTH_PROPERTY_PREFIX +
            level.getPropertyName() + SOLR_PASSWORD_PROPERTY_SUFFIX);

        if (userNameOpt.isEmpty() || passwordOpt.isEmpty()) {
            return;
        }

        String authString = userNameOpt.get() + ":" + passwordOpt.get();
        request.addHeader("Authorization", "Basic " +
            Base64.getEncoder().encodeToString(authString.getBytes()));
    }

}
