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

package org.mycore.user.restapi.v2.resource;

import org.mycore.user.restapi.exception.MCRUserAlreadyExistsException;
import org.mycore.user.restapi.exception.MCRUserException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ExceptionMapper} that translates {@link MCRUserException} subclasses
 * into appropriate HTTP responses.
 */
@Provider
public class MCRUsersExceptionMapper implements ExceptionMapper<MCRUserException> {

    @Override
    public Response toResponse(MCRUserException e) {
        return switch (e) {
            case MCRUserNotFoundException ex ->
                Response.status(Response.Status.NOT_FOUND).entity(ex.getMessage()).build();
            case MCRUserAlreadyExistsException ex ->
                Response.status(Response.Status.CONFLICT).entity(ex.getMessage()).build();
            case MCRUserValidationException ex ->
                Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
            default -> Response.serverError().entity(e.getMessage()).build();
        };
    }
}
