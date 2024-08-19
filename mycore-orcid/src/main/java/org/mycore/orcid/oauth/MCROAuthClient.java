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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.services.http.MCRURLQueryParameter;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

/**
 * Utility class working as a client for the OAuth2 API of orcid.org.
 * Used to get access tokens. Minimum configuration requires to set
 *
 * MCR.ORCID.OAuth.BaseURL
 * MCR.ORCID.OAuth.ClientID
 * MCR.ORCID.OAuth.ClientSecret
 *
 * @author Frank Lützenkirchen
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

        baseURL = MCRConfiguration2.getStringOrThrow(prefix + "BaseURL");
        clientID = MCRConfiguration2.getStringOrThrow(prefix + "ClientID");
        clientSecret = MCRConfiguration2.getStringOrThrow(prefix + "ClientSecret");

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

    public MCRRevokeRequest getRevokeRequest(String token) {
        MCRRevokeRequest req = new MCRRevokeRequest(client.target(baseURL));
        req.set("client_id", clientID);
        req.set("client_secret", clientSecret);
        req.set("token", token);
        return req;
    }

    /**
     * Builds the URL where to redirect the user's browser to initiate a three-way authorization
     * and request permission to access the given scopes. If
     *
     * MCR.ORCID.PreFillRegistrationForm=true
     *
     * submits the current user's E-Mail address, first and last name to the ORCID registration form
     * to simplify registration. May be disabled for more data privacy.
     *
     * @param redirectURL The URL to redirect back to after the user has granted permission
     * @param scopes the scope(s) to request permission for, if multiple separate by blanks
     */
    String getCodeRequestURL(String redirectURL, String scopes) throws URISyntaxException, MalformedURLException {
        List<MCRURLQueryParameter> parameters = new ArrayList<>();
        parameters.add(new MCRURLQueryParameter("client_id", clientID));
        parameters.add(new MCRURLQueryParameter("response_type", "code"));
        parameters.add(new MCRURLQueryParameter("redirect_uri", redirectURL));
        parameters.add(new MCRURLQueryParameter("scope", scopes.trim()));
        parameters.add(new MCRURLQueryParameter("state", buildStateParam()));
        parameters.add(new MCRURLQueryParameter("prompt", "login"));

        // check if current lang is supported
        List<String> supportedLanguages = Arrays
            .asList(MCRConfiguration2.getStringOrThrow("MCR.ORCID.SupportedLanguages").split(",", 0));
        if (supportedLanguages.contains(MCRSessionMgr.getCurrentSession().getCurrentLanguage())) {
            parameters.add(new MCRURLQueryParameter("lang", MCRSessionMgr.getCurrentSession().getCurrentLanguage()));
        } else {
            parameters.add(new MCRURLQueryParameter("lang", "en"));
        }

        if (MCRConfiguration2.getOrThrow("MCR.ORCID.PreFillRegistrationForm", Boolean::parseBoolean)) {
            preFillRegistrationForm(parameters);
        }
        return new URI(baseURL + "/authorize" + MCRURLQueryParameter.toQueryString(parameters)).toString();
    }

    /**
     * If
     *
     * MCR.ORCID.PreFillRegistrationForm=true
     *
     * submits the current user's E-Mail address, first and last name to the ORCID registration form
     * to simplify registration. May be disabled for more data privacy.
     *
     * See https://members.orcid.org/api/resources/customize
     */
    private void preFillRegistrationForm(List<MCRURLQueryParameter> parameters) {
        MCRUser user = MCRUserManager.getCurrentUser();
        String eMail = user.getEMailAddress();
        if (eMail != null) {
            parameters.add(new MCRURLQueryParameter("email", eMail));
        }

        String name = user.getRealName();
        String firstName = null;
        String lastName = name;

        if (name.contains(",")) {
            String[] nameParts = name.split(",");
            if (nameParts.length == 2) {
                firstName = nameParts[1].trim();
                lastName = nameParts[0].trim();
            }
        } else if (name.contains(" ")) {
            String[] nameParts = name.split(" ");
            if (nameParts.length == 2) {
                firstName = nameParts[0].trim();
                lastName = nameParts[1].trim();
            }
        }

        if (firstName != null) {
            parameters.add(new MCRURLQueryParameter("given_names", firstName));
        }
        if (lastName != null) {
            parameters.add(new MCRURLQueryParameter("family_names", lastName));
        }
    }

    /**
     * Builds a state parameter to be used with OAuth to defend against cross-site request forgery.
     * Comparing state ensures the user is still the same that initiated the authorization process.
     */
    static String buildStateParam() {
        String userID = MCRUserManager.getCurrentUser().getUserID();
        byte[] bytes = userID.getBytes(StandardCharsets.UTF_8);
        MessageDigest md5Digest = MCRUtils.buildMessageDigest(MCRMD5Digest.ALGORITHM);
        md5Digest.update(bytes);
        byte[] digest = md5Digest.digest();
        return MCRUtils.toHexString(digest);
    }
}
