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

package org.mycore.orcid2.client;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.client.filter.MCRORCIDAuthenticationFilter;
import org.mycore.orcid2.client.filter.MCRORCIDXMLReader;
import org.mycore.orcid2.client.filter.MCRORCIDXMLWriter;

/**
 * Basic ORCID client.
 * Defines basic setup and reading methods.
 */
abstract class MCRORCIDBaseClient {

    /**
     * ORCID XML media type.
     */
    protected static final MediaType ORCID_XML_MEDIA_TYPE
        = MediaType.valueOf(MCRORCIDClientConstants.ORCID_XML_MEDIA_TYPE);

    private final WebTarget baseTarget;

    private MCRORCIDClientErrorHandler errorHandler;

    /**
     * Creates MCRORCIDBaseClient with rest url and token.
     * 
     * @param restURL rest url
     * @param token the token
     */
    protected MCRORCIDBaseClient(String restURL, String token) {
        final Client client = ClientBuilder.newClient();
        client.register(new MCRORCIDXMLReader<Object>());
        client.register(new MCRORCIDXMLWriter());
        Optional.ofNullable(token).ifPresent(t -> client.register(new MCRORCIDAuthenticationFilter(t)));
        this.baseTarget = client.target(restURL.endsWith("/") ? restURL : restURL + "/");
    }

    /**
     * Registers error handler.
     * 
     * @param errorHandler the error handler
     */
    public void registerErrorHandler(MCRORCIDClientErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Fetches ORCID with given query params.
     * 
     * @param path the path
     * @param queryMap the query params
     * @return the Response
     */
    protected Response fetch(String path, Map<String, Object> queryMap) {
        WebTarget target = baseTarget.path(path);
        for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }
        return target.request(ORCID_XML_MEDIA_TYPE).get();
    }

    /**
     * Fetches section/object of orcid profile and wraps response into type.
     * 
     * @param orcid the ORCID iD
     * @param section the ORCID section
     * @param <T> the result class
     * @param valueType the result class
     * @param putCodes optional put code(s)
     * @return transformed section/object
     * @throws MCRORCIDRequestException if request fails
     */
    protected <T> T doFetch(String orcid, MCRORCIDSection section, Class<T> valueType, long... putCodes) {
        final String putCodeString = (putCodes == null || putCodes.length == 0) ? "" : StringUtils.join(putCodes, ',');
        final Response response
            = fetch(String.format(Locale.ROOT, "%s/%s/%s", orcid, section.getPath(), putCodeString));
        if (!Objects.equals(response.getStatusInfo().getFamily(), Response.Status.Family.SUCCESSFUL)) {
            handleError(response);
        }
        return response.readEntity(valueType); // may ProcessingException
    }

    /**
     * Handles error response.
     * 
     * @param response the error response
     * @throws MCRORCIDRequestException always
     */
    protected void handleError(Response response) {
        if (errorHandler != null) {
            errorHandler.handleErrorResponse(response);
        }
        throw new MCRORCIDRequestException(response);
    }

    /**
     * Returns the base WebTarget of client.
     * 
     * @return base WebTarget
     */
    protected WebTarget getBaseTarget() {
        return baseTarget;
    }

    private Response fetch(String path) {
        return fetch(path, Collections.emptyMap());
    }
}
