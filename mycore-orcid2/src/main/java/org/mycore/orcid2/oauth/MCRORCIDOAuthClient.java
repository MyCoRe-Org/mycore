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

package org.mycore.orcid2.oauth;

import java.util.Objects;

import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

/**
 * Client for the OAuth2 API of orcid.org.
 * Used to exchange or revoke access tokens.
 * Minimum configuration requires to set:
 * <p>
 * MCR.ORCID2.BaseURL
 * MCR.ORCID2.OAuth.ClientID
 * MCR.ORCID2.OAuth.ClientSecret
 */
public final class MCRORCIDOAuthClient {

    private final Settings settings;

    private final WebTarget webTarget;

    public MCRORCIDOAuthClient(Settings settings) {
        this.settings = Objects.requireNonNull(settings, "Settings must not be null");
        webTarget = ClientBuilder.newClient().target(settings.baseUrl).path("oauth");
    }

    /**
     * Initializes and returns client instance.
     *
     * @return client instance
     */
    @MCRFactory
    public static MCRORCIDOAuthClient obtainInstance() {
        return LazyInstanceHelper.SHARED_INSTANCE;
    }

    public static MCRORCIDOAuthClient createInstance() {
        return new MCRORCIDOAuthClient(new Settings(
            MCRORCIDConstants.ORCID_BASE_URL,
            MCRORCIDConstants.ORCID_OAUTH_CLIENT_ID,
            MCRORCIDConstants.ORCID_OAUTH_CLIENT_SECRET
        ));
    }

     /**
     * Revokes given bearer access token.
     *
     * @param token revoke token
     * @throws MCRORCIDRequestException if request fails
     */
    public void revokeToken(String token) {
        Form form = new Form();
        form.param("client_id", settings.clientId);
        form.param("client_secret", settings.clientSecret);
        form.param("token", token);
        final Response response = webTarget.path("revoke").request().post(Entity.form(form));
        if (!Objects.equals(response.getStatusInfo().getFamily(), Response.Status.Family.SUCCESSFUL)) {
            handleError(response);
        }
    }

    /**
     * Exchanges authorization code for an MCRORCIDOAuthAccessTokenResponse.
     * 
     * @param code the ORCID auth code
     * @param redirectURI the redirect URI
     * @return the MCRORCIDOAuthAccessTokenResponse
     * @throws MCRORCIDRequestException if request fails
     * @see MCRORCIDOAuthAccessTokenResponse
     */
    public MCRORCIDOAuthAccessTokenResponse exchangeCode(String code, String redirectURI) {
        Form form = new Form();
        form.param("client_id", settings.clientId);
        form.param("client_secret", settings.clientSecret);
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("redirect_uri", redirectURI);
        final Response response = webTarget.path("token").request().post(Entity.form(form));
        if (!Objects.equals(response.getStatusInfo().getFamily(), Response.Status.Family.SUCCESSFUL)) {
            handleError(response);
        }
        return response.readEntity(MCRORCIDOAuthAccessTokenResponse.class);
    }

    private void handleError(Response response) {
        throw new MCRORCIDRequestException(response);
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

    private static final class LazyInstanceHelper {
        static final MCRORCIDOAuthClient SHARED_INSTANCE = createInstance();
    }

}
