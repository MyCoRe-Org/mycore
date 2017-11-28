/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
*/

package org.mycore.orcid.oauth;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import javax.ws.rs.core.Response.StatusType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the response on a token request against the OAuth2 API of orcid.org.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTokenResponse {

    private StatusType status;

    private JsonNode responseData;

    MCRTokenResponse(Response response) throws JsonProcessingException, IOException {
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

    /**
     * Returns the access token, in case the request wasSuccessful()
     */
    public String getAccessToken() {
        return responseData.get("access_token").asText();
    }
}
