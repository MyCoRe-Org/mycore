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
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.solr.client.solrj.SolrRequest;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Basic authentication implementation that uses the username and password from the configuration.
 * The username and password are configured using the properties {@code Username} and {@code Password}.
 */
public class MCRSolrBasicPropertyAuthentication implements MCRSolrAuthentication {

    private static final String SOLR_USERNAME_PROPERTY_NAME = "Username";

    private static final String SOLR_PASSWORD_PROPERTY_NAME = "Password";

    private String username;

    private String password;

    private MCRSolrAuthenticationLevel level;

    @Override
    public void setLevel(MCRSolrAuthenticationLevel level) {
        this.level = level;
    }

    public MCRSolrAuthenticationLevel getLevel() {
        return level;
    }

    @Override
    public void applyAuthentication(SolrRequest<?> request) {
        request.setBasicAuthCredentials(username, password);
    }

    @Override
    public void applyAuthentication(HttpRequest.Builder request) {
        String authString = username + ":" + password;
        request.header("Authorization", "Basic " +
                Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8)));
    }

    public String getPassword() {
        return password;
    }

    @MCRProperty(name = SOLR_PASSWORD_PROPERTY_NAME)
    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    @MCRProperty(name = SOLR_USERNAME_PROPERTY_NAME)
    public void setUsername(String username) {
        this.username = username;
    }
}
