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

import jakarta.ws.rs.client.WebTarget;

import org.mycore.common.config.MCRConfigurationException;

/**
 * Represents a token request against the OAuth2 API of orcid.org.
 *
 * @author Frank L\u00FCtzenkirchen
 * @author Kai Brandhorst
 */
public class MCRRevokeRequest extends MCRORCIDRequest {

    MCRRevokeRequest(WebTarget baseTarget) {
        super(baseTarget);
    }

    /**
     * Posts the request and returns the response.
     *
     * @throws MCRConfigurationException if request fails, e.g. because of misconfigured client ID and secret
     */
    public MCRRevokeResponse post() throws MCRConfigurationException, IOException {
        MCRRevokeResponse response = new MCRRevokeResponse(post("revoke"));
        if (!response.wasSuccessful()) {
            throw new MCRConfigurationException(response.getStatusMessage());
        }
        return response;
    }
}
