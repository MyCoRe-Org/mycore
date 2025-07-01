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

package org.mycore.orcid.oauth;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.services.http.MCRURLQueryParameter;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

/**
 * Utility class working as a client for the OAuth2 API of orcid.org.
 * Used to get access tokens. Minimum configuration requires to set
 * <ul>
 * <li>MCR.ORCID.OAuth.BaseURL</li>
 * <li>MCR.ORCID.OAuth.ClientID</li>
 * <li>MCR.ORCID.OAuth.ClientSecret</li>
 * </ul>
 *
 * @author Frank Lützenkirchen
 */
@MCRConfigurationProxy(proxyClass = MCROAuthClient.Factory.class)
public final class MCROAuthClient {

    private static final String CLIENT_PROPERTY = "MCR.ORCID.OAuth";

    private final Settings settings;

    private final Client client;

    public MCROAuthClient(Settings settings) {
        this.settings = Objects.requireNonNull(settings, "Settings must not be null");
        this.client = ClientBuilder.newClient();
    }

    @MCRFactory
    public static MCROAuthClient obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public static MCROAuthClient createInstance() {
        String classProperty = CLIENT_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCROAuthClient.class, classProperty);
    }

    public String getClientID() {
        return settings.clientId;
    }

    /**
     * Builds am OAuth2 token request.
     */
    public MCRTokenRequest getTokenRequest() {
        MCRTokenRequest req = new MCRTokenRequest(client.target(settings.baseUrl));
        req.set("client_id", settings.clientId);
        req.set("client_secret", settings.clientSecret);
        return req;
    }

    public MCRRevokeRequest getRevokeRequest(String token) {
        MCRRevokeRequest req = new MCRRevokeRequest(client.target(settings.baseUrl));
        req.set("client_id", settings.clientId);
        req.set("client_secret", settings.clientSecret);
        req.set("token", token);
        return req;
    }

    /**
     * Builds the URL where to redirect the user's browser to initiate a three-way authorization
     * and request permission to access the given scopes. If
     * <p>
     * MCR.ORCID.PreFillRegistrationForm=true
     * <p>
     * submits the current user's E-Mail address, first and last name to the ORCID registration form
     * to simplify registration. May be disabled for more data privacy.
     *
     * @param redirectURL The URL to redirect back to after the user has granted permission
     * @param scopes the scope(s) to request permission for, if multiple separate by blanks
     */
    String getCodeRequestURL(String redirectURL, String scopes) throws URISyntaxException, MalformedURLException {
        List<MCRURLQueryParameter> parameters = new ArrayList<>();
        parameters.add(new MCRURLQueryParameter("client_id", settings.clientId));
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
        return new URI(settings.baseUrl + "/authorize" + MCRURLQueryParameter.toQueryString(parameters)).toString();
    }

    /**
     * If
     * <p>
     * MCR.ORCID.PreFillRegistrationForm=true
     * <p>
     * submits the current user's E-Mail address, first and last name to the ORCID registration form
     * to simplify registration. May be disabled for more data privacy.
     * <p>
     * See https://members.orcid.org/api/resources/customize
     */
    private void preFillRegistrationForm(List<MCRURLQueryParameter> parameters) {
        MCRUser user = MCRUserManager.getCurrentUser();
        String eMail = user.getEMail();
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


    public record Settings(
        String baseUrl,
        String clientId,
        String clientSecret) {

        public Settings {
            Objects.requireNonNull(baseUrl, "Base URL must not be null");
            Objects.requireNonNull(clientId, "Client ID must not be null");
            Objects.requireNonNull(clientSecret, "Client secret must not be null");
        }

    }

    public static class Factory implements Supplier<MCROAuthClient> {

        @MCRProperty(name = "BaseURL")
        public String baseUrl;

        @MCRProperty(name = "ClientID")
        public String clientId;

        @MCRProperty(name = "ClientSecret")
        public String clientSecret;

        @Override
        public MCROAuthClient get() {

            Settings settings = new Settings(
                baseUrl,
                clientId,
                clientSecret
            );

            return new MCROAuthClient(settings);

        }

    }

    private static final class LazyInstanceHolder {
        public static final MCROAuthClient SHARED_INSTANCE = createInstance();
    }

}
