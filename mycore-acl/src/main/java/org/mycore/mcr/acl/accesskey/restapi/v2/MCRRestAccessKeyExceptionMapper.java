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

package org.mycore.mcr.acl.accesskey.restapi.v2;

import java.util.Optional;

import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MCRRestAccessKeyExceptionMapper implements ExceptionMapper<MCRAccessKeyException> {

    @Context
    Request request;

    @Override
    public Response toResponse(MCRAccessKeyException exception) {
            return fromException(exception);
    }

    public static Response fromException(MCRAccessKeyException e) {
        if (e instanceof MCRAccessKeyNotFoundException) {
            return getResponse(e, Response.Status.NOT_FOUND.getStatusCode(),
                e.getErrorCode());
        }
        return getResponse(e, Response.Status.BAD_REQUEST.getStatusCode(),
            e.getErrorCode());
    }

    private static Response getResponse(Exception e, int statusCode, String errorCode) {
        MCRErrorResponse response = MCRErrorResponse.fromStatus(statusCode)
            .withCause(e)
            .withMessage(e.getMessage())
            .withDetail(Optional.of(e)
                .map(ex -> (ex instanceof WebApplicationException) ? ex.getCause() : ex)
                .map(Object::getClass)
                .map(Class::getName)
                .orElse(null))
            .withErrorCode(errorCode);
        //LogManager.getLogger().error(response::getLogMessage, e);
        return Response.status(response.getStatus())
            .entity(response)
            .build();
    }
}
