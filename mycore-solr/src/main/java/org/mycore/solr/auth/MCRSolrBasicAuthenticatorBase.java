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

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Authentication implementation that adds <code>Authorization</code> headers with
 * Basic access authentication to Solr requests.  
 */
public abstract class MCRSolrBasicAuthenticatorBase implements MCRSolrAuthenticator {

    @Override
    public final void applyAuthentication(SolrRequest<?> request) {
        request.setBasicAuthCredentials(getUsername(), getPassword());
    }

    @Override
    public final void applyAuthentication(HttpRequest.Builder request) {
        request.header("Authorization", "Basic " + getEncodedAuthString());
    }

    private String getEncodedAuthString() {
        String authString = getUsername() + ":" + getPassword();
        return Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
    }

    protected abstract String getPassword();

    protected abstract String getUsername();

}
