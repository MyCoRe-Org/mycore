/*
 * $Revision: 29635 $ $Date: 2014-04-10 10:55:06 +0200 (Do, 10 Apr 2014) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
