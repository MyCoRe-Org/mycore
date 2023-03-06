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

package org.mycore.orcid2.v3;

import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.orcid2.client.MCRORCIDClientFactory;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.user.MCRORCIDCredentials;

/**
 * Provides utilty methods for v3 client.
 */
public class MCRORCIDClientHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Fetches API with best credentials.
     * If credentials exist for an ORCID iD, the Member API is requested with the token. 
     * If a problem occurs during the request or no credentials exist, 
     * the general Member/Public API is requested as a fallback.
     * 
     * @param orcid the ORCID iD
     * @param section the section
     * @param <T> the result class
     * @param valueType type of the response
     * @param putCodes optional put codes 
     * @return the result as specified type
     * @throws MCRORCIDRequestException if the request fails
     */
    public static <T> T fetchWithBestCredentials(String orcid, MCRORCIDSectionImpl section, Class<T> valueType,
        long... putCodes) throws MCRORCIDRequestException {
        final MCRORCIDCredentials credentials = MCRORCIDUserUtils.getCredentialsByORCID(orcid);
        if (credentials != null) {
            try {
                return getClientFactory().createUserClient(credentials).fetch(section, valueType, putCodes);
            } catch (MCRORCIDRequestException e) {
                final Response response = e.getErrorResponse();
                if (Objects.equals(response.getStatusInfo().getFamily(), Response.Status.Family.CLIENT_ERROR)) {
                    LOGGER.info(
                        "Request with credentials for orcid {} has failed with status code {}."
                            + " Token has probably expired.",
                        orcid, response.getStatus());
                    return getClientFactory().createReadClient().fetch(orcid, section, valueType, putCodes);
                } else {
                    throw e;
                }
            }
        } else {
            return getClientFactory().createReadClient().fetch(orcid, section, valueType, putCodes);
        }
    }

    /**
     * Returns v3 MCRORCIDClientFactory.
     *
     * @return MCRORCIDClientFactory
     */
    public static MCRORCIDClientFactory getClientFactory() {
        return MCRORCIDClientFactory.getInstance("V3");
    }
}
