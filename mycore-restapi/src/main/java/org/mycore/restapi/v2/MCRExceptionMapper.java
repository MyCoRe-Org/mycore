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

package org.mycore.restapi.v2;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.ExceptionMapper;

public class MCRExceptionMapper implements ExceptionMapper<Exception> {

    private final static List<Variant> SUPPORTED_VARIANTS = Variant
        .mediaTypes(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE).build();

    private final static Logger LOGGER = LogManager.getLogger();

    @Context
    Request request;

    /**
     * Maps the given exception to a response.
     * <p>
     * If the client accepts neither JSON nor XML, a {@link WebApplicationException} keeps its own
     * response and any other exception results in a plain {@code 500 Internal Server Error}.
     *
     * @param exception the exception to map
     * @return the response for the exception
     */
    @Override
    public Response toResponse(Exception exception) {
        return Optional.ofNullable(request.selectVariant(SUPPORTED_VARIANTS))
            .map(v -> fromException(exception))
            .orElseGet(() -> fromExceptionWithoutEntity(exception));
    }

    private static Response fromExceptionWithoutEntity(Exception exception) {
        if (exception instanceof WebApplicationException wae) {
            return wae.getResponse();
        }
        // returning null would make Jersey answer with 204 No Content
        LOGGER.error("Error while processing request", exception);
        return Response.serverError().build();
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
        MCRErrorResponse response = MCRErrorResponse.fromStatus(statusCode)
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
