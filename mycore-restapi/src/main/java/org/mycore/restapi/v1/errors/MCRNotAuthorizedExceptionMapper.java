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

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;

/**
 * Maps response status {@link Response.Status#FORBIDDEN} to
 * {@link Response.Status#UNAUTHORIZED} if current user is guest.
 */
public class MCRNotAuthorizedExceptionMapper implements ExceptionMapper<NotAuthorizedException> {
    public Response toResponse(NotAuthorizedException ex) {
        if (ex.getMessage()==null || ex.getResponse().getEntity() != null){
            return ex.getResponse();
        }
        return Response.fromResponse(ex.getResponse())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN+ ";charset=utf-8")
            .entity(ex.getMessage())
            .build();
    }
}
