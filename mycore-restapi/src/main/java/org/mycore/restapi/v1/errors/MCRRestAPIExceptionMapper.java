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
package org.mycore.restapi.v1.errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * maps a REST API exception to a proper response with message as JSON output
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 *
 */
public class MCRRestAPIExceptionMapper implements ExceptionMapper<MCRRestAPIException> {
    public Response toResponse(MCRRestAPIException ex) {
        return Response.status(ex.getStatus()).entity(MCRRestAPIError.convertErrorListToJSONString(ex.getErrors()))
            .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
