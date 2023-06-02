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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the response on a request against the OAuth2 API of orcid.org.
 *
 * @author Frank Lützenkirchen
 * @author Kai Brandhorst
 */
public class MCRORCIDResponse {

    private StatusType status;

    protected JsonNode responseData;

    MCRORCIDResponse(Response response) throws IOException {
        this.status = response.getStatusInfo();
        String jsonTree = response.readEntity(String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        responseData = objectMapper.readTree(jsonTree);
    }

    public boolean wasSuccessful() {
        return status.getFamily() == Family.SUCCESSFUL;
    }

    public String getStatusMessage() {
        StringBuilder b = new StringBuilder();
        b.append(status.getStatusCode()).append(' ').append(status.getReasonPhrase());
        if (!wasSuccessful()) {
            b.append(", ").append(responseData.get("error").asText());
            b.append(": ").append(responseData.get("error_description").asText());
        }

        return b.toString();
    }

    /**
     * Returns the ORCID given in the response, if any
     */
    public String getORCID() {
        return responseData.get("orcid").asText();
    }
}
