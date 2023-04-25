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

import java.util.Locale;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.orcid2.client.MCRORCIDClientFactory;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.orcid.jaxb.model.v3.release.error.OrcidError;

/**
 * Provides utilty methods for v3 client.
 */
public class MCRORCIDClientHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Fetches API with best credential.
     * If credential exist for an ORCID iD, the Member API is requested with the token. 
     * If a problem occurs during the request or no credential exist, 
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
        long... putCodes) {
        if (getClientFactory().checkMemberMode()) {
            try {
                final MCRORCIDCredential credential = MCRORCIDUserUtils.getCredentialByORCID(orcid);
                if (credential != null) {
                    return getClientFactory().createUserClient(orcid, credential).fetch(section, valueType, putCodes);
                }
            } catch (MCRORCIDRequestException e) {
                final Response response = e.getResponse();
                if (Objects.equals(response.getStatusInfo().getFamily(), Response.Status.Family.CLIENT_ERROR)) {
                    LOGGER.info(
                        "Request with credential for orcid {} has failed with status code {} and error: {}\n"
                            + "Token has probably expired. Trying to use read client as fallback...",
                        orcid, response.getStatus(), e.getMessage());
                } else {
                    throw e;
                }
            } catch (MCRORCIDException e) {
                LOGGER.warn("Error with credential for {}. Trying to use read client as fallback...", orcid, e);
            }
        }
        return getClientFactory().createReadClient().fetch(orcid, section, valueType, putCodes);
    }

    /**
     * Returns debug string for OrcidError.
     * 
     * @param error the OrcidError
     * @return the debug message
     */
    public static String createDebugMessageFromORCIDError(OrcidError error) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.ROOT, "response code: %d\n", error.getResponseCode()));
        builder.append(String.format(Locale.ROOT, "developer message: %s\n", error.getDeveloperMessage()));
        builder.append(String.format(Locale.ROOT, "user message: %s\n", error.getUserMessage()));
        builder.append(String.format(Locale.ROOT, "error code: %s", error.getErrorCode()));
        return builder.toString();
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
