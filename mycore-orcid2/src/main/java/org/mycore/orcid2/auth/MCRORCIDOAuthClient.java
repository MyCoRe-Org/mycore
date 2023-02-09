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

package org.mycore.orcid2.auth;

import java.util.Objects;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.user.MCRORCIDCredentials;

/**
 * Client for the OAuth2 API of orcid.org. Used to
 * get or revoke access tokens. Minimum configuration requires to set
 *
 * MCR.ORCID2.BaseURL
 * MCR.ORCID2.OAuth.ClientID
 * MCR.ORCID2.OAuth.ClientSecret
 */
public class MCRORCIDOAuthClient {

    private static final String CONFIG_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "OAuth.";

    private static final String ORCID_BASE_URL
        = MCRConfiguration2.getStringOrThrow(MCRORCIDConstants.CONFIG_PREFIX + "BaseURL");

    /**
     * Client id of the client.
     */
    public static final String CLIENT_ID = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + "ClientID");

    private static final String CLIENT_SECRET = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + "ClientSecret");

    private final WebTarget webTarget;

    private MCRORCIDOAuthClient() {
        webTarget = ClientBuilder.newClient().target(ORCID_BASE_URL).path("oauth");
    }

    /**
     * Initializes and returns client instance.
     *
     * @return client instance
     */
    public static MCRORCIDOAuthClient getInstance() {
        return LazyInstanceHelper.INSTANCE;
    }

    /**
     * Revokes given bearer acces token.
     *
     * @param token revoke token
     * @throws MCRORCIDRequestException if request fails
     */
    public void revokeToken(String token) throws MCRORCIDRequestException {
        Form form = new Form();
        form.param("client_id", CLIENT_ID);
        form.param("client_secret", CLIENT_SECRET);
        form.param("token", token);
        final Response response = webTarget.path("revoke").request().post(Entity.form(form));
        if (!Objects.equals(response.getStatusInfo().getFamily(), Response.Status.Family.SUCCESSFUL)) {
            handleError(response);
        }
    }

    /**
     * Exchanges authorization code for an access token.
     * 
     * @param code the orcid auth code
     * @param redirectURI the redirect uri
     * @return response serialized as MCRORCIDCredentials
     * @throws MCRORCIDRequestException if request fails
     * @see MCRORCIDCredentials
     */
    public MCRORCIDCredentials exchangeCode(String code, String redirectURI) throws MCRORCIDRequestException {
        Form form = new Form();
        form.param("client_id", CLIENT_ID);
        form.param("client_secret", CLIENT_SECRET);
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("redirect_uri", redirectURI);
        final Response response = webTarget.path("token").request().post(Entity.form(form));
        if (!Response.Status.Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
            handleError(response);
        }
        return response.readEntity(MCRORCIDCredentials.class);
    }

    private void handleError(Response response) throws MCRORCIDRequestException {
        throw new MCRORCIDRequestException(response);
    }

    private static class LazyInstanceHelper {
        static final MCRORCIDOAuthClient INSTANCE = new MCRORCIDOAuthClient();
    }
}
