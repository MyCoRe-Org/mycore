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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.streams.MCRMD5InputStream;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

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
        URIBuilder builder = new URIBuilder(baseURL + "/authorize");
        builder.addParameter("client_id", clientID);
        builder.addParameter("response_type", "code");
        builder.addParameter("redirect_uri", redirectURL);
        builder.addParameter("scope", scopes.trim().replace(" ", "%20"));
        builder.addParameter("state", buildStateParam());
        builder.addParameter("lang", MCRSessionMgr.getCurrentSession().getCurrentLanguage());

        if (MCRConfiguration.instance().getBoolean("MCR.ORCID.PreFillRegistrationForm")) {
            preFillRegistrationForm(builder);
        }

        return builder.build().toURL().toExternalForm();
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
    private void preFillRegistrationForm(URIBuilder builder) {
        MCRUser user = MCRUserManager.getCurrentUser();
        String eMail = user.getEMailAddress();
        if (eMail != null) {
            builder.addParameter("email", eMail);
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
            builder.addParameter("given_names", firstName);
        }
        if (lastName != null) {
            builder.addParameter("family_names", lastName);
        }
    }

    /**
     * Builds a state parameter to be used with OAuth to defend against cross-site request forgery.
     * Comparing state ensures the user is still the same that initiated the authorization process.
     */
    static String buildStateParam() {
        String userID = MCRUserManager.getCurrentUser().getUserID();
        byte[] bytes = userID.getBytes(StandardCharsets.UTF_8);
        MessageDigest md5Digest = MCRMD5InputStream.buildMD5Digest();
        md5Digest.update(bytes);
        byte[] digest = md5Digest.digest();
        return MCRMD5InputStream.getMD5String(digest);
    }
}
