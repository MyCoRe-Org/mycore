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

package org.mycore.orcid;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the error information returned from ORCID's REST API in case request was not successful
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRORCIDException extends IOException {

    private static final long serialVersionUID = 1846541932326084816L;

    private String message;

    public MCRORCIDException(String message) {
        super(message);
    }

    public MCRORCIDException(Response response) throws JsonProcessingException, IOException {
        StatusType status = response.getStatusInfo();
        String responseBody = response.readEntity(String.class);

        StringBuilder sb = new StringBuilder();
        if (responseBody.startsWith("{")) {
            JsonNode json = new ObjectMapper().readTree(responseBody);
            addJSONField(json, "error-code", sb, ": ");
            addJSONField(json, "developer-message", sb, "");
            addJSONField(json, "error_description", sb, "");
        } else {
            sb.append(status.getStatusCode()).append(": ").append(status.getReasonPhrase());
        }
        this.message = sb.toString();
    }

    private void addJSONField(JsonNode json, String field, StringBuilder sb, String delimiter) {
        if (json.has(field)) {
            sb.append(json.get(field).asText()).append(delimiter);
        }
    }

    @Override
    public String getMessage() {
        return message;
    }
}
