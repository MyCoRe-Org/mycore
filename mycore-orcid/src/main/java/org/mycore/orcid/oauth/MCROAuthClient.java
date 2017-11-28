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

package org.mycore.orcid.oauth;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.mycore.common.config.MCRConfiguration;

/**
 * Utility class working as a client for the OAuth2 API of orcid.org.
 * Used to get access tokens. Minimum configuration requires to set
 *
 * MCR.ORCID.OAuth.BaseURL
 * MCR.ORCID.OAuth.ClientID
 * MCR.ORCID.OAuth.ClientSecret
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCROAuthClient {

    private static MCROAuthClient SINGLETON = new MCROAuthClient();

    private String baseURL;

    private String clientID;

    private String clientSecret;

    private Client client;

    public static MCROAuthClient instance() {
        return SINGLETON;
    }

    private MCROAuthClient() {
        String prefix = "MCR.ORCID.OAuth.";
        MCRConfiguration config = MCRConfiguration.instance();

        baseURL = config.getString(prefix + "BaseURL");
        clientID = config.getString(prefix + "ClientID");
        clientSecret = config.getString(prefix + "ClientSecret");

        client = ClientBuilder.newClient();
    }

    public String getClientID() {
        return clientID;
    }

    /**
     * Builds am OAuth2 token request.
     */
    public MCRTokenRequest getTokenRequest() {
        MCRTokenRequest req = new MCRTokenRequest(client.target(baseURL));
        req.set("client_id", clientID);
        req.set("client_secret", clientSecret);
        return req;
    }

    /**
     * Builds the URL where to redirect the user's browser to initiate a three-way authorization
     * and request permission to access the given scopes
     *
     * @param redirectURL The URL to redirect back to after the user has granted permission
     * @param scopes the scope(s) to request permission for, if multiple separate by blanks
     */
    String getCodeRequestURL(String redirectURL, String scopes) throws URISyntaxException, MalformedURLException {
        URIBuilder builder = new URIBuilder(baseURL + "/authorize");
        builder.addParameter("client_id", clientID);
        builder.addParameter("response_type", "code");
        builder.addParameter("redirect_uri", redirectURL);
        builder.addParameter("scope", scopes.trim().replace(" ", "%20"));
        return builder.build().toURL().toExternalForm();
    }
}
