/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.restapi.v2;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class MCRExceptionMapper implements ExceptionMapper<Exception> {

    private final static Logger LOGGER = LogManager.getLogger();

    /**
     * Maps the given exception to an error response with an {@link MCRErrorResponse} entity.
     * <p>
     * A {@link WebApplicationException} that already carries an entity keeps its response. If the client accepts
     * neither JSON nor XML, {@link MCRErrorResponseMediaTypeFilter} delivers the error entity as JSON.
     *
     * @param exception the exception to map
     * @return the response for the exception
     */
    @Override
    public Response toResponse(Exception exception) {
        return fromException(exception);
    }

    public static Response fromWebApplicationException(WebApplicationException wae) {
        if (wae.getResponse().hasEntity()) {
            //usually WAEs with entity do not arrive here
            LOGGER.warn("WebApplicationException already has an entity attached, forwarding response");
            return wae.getResponse();
        }
        final Response response = getResponse(wae, wae.getResponse().getStatus());
        response.getHeaders().putAll(wae.getResponse().getHeaders());
        return response;
    }

    public static Response fromException(Exception e) {
        if (e instanceof WebApplicationException wae) {
            return fromWebApplicationException(wae);
        }
        return getResponse(e, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private static Response getResponse(Exception e, int statusCode) {
        MCRErrorResponse response = MCRErrorResponse.ofStatusCode(statusCode)
            .withCause(e)
            .withMessage(e.getMessage())
            .withDetail(Optional.of(e)
                .map(ex -> (ex instanceof WebApplicationException) ? ex.getCause() : ex)
                .map(Object::getClass)
                .map(Class::getName)
                .orElse(null));
        LogManager.getLogger().error(response::getLogMessage, e);
        return Response.status(response.getStatus())
            .entity(response)
            .build();
    }
}
