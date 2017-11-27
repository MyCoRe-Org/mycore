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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Returns the read-public access token to read public data from ORCID.org.
 * A token is needed for access to the public API, see
 * https://members.orcid.org/api/tutorial/read-orcid-records#readpub
 *
 * The token can be configured via MCR.ORCID.OAuth.ReadPublicToken.
 * In case that is not set, the token is directly requested from the OAuth2 API and logged.
 *
 * @author Frank L\u00FCtzenkirchen *
 */
public class MCRReadPublicTokenFactory {

    private static final Logger LOGGER = LogManager.getLogger(MCRReadPublicTokenFactory.class);

    private static final String CONFIG_PROPERTY = "MCR.ORCID.OAuth.ReadPublicToken";

    private static String token = MCRConfiguration.instance().getString(CONFIG_PROPERTY, null);

    /**
     * Returns the read-public access token
     */
    public static String getToken() {
        if ((token == null) || token.isEmpty()) {
            requestToken();
        }
        return token;
    }

    /**
     * Requests the token from the OAuth2 API of ORCID.org.
     * Logs a warning, so you better check your logs and directly configure
     * MCR.ORCID.OAuth.ReadPublicToken
     */
    private static void requestToken() {
        LOGGER.info("requesting read-public access token...");

        MCRTokenRequest request = MCROAuthClient.instance().getTokenRequest();
        request.set("grant_type", "client_credentials");
        request.set("scope", "/read-public");

        try {
            MCRTokenResponse response = request.post();
            token = response.getAccessToken();
        } catch (IOException ex) {
            String msg = "Could not get read-public access token from ORCID OAuth API";
            throw new MCRConfigurationException(msg, ex);
        }

        LOGGER.warn("You should set the access token in mycore.properties:");
        LOGGER.warn(CONFIG_PROPERTY + "={}", token);
    }
}
