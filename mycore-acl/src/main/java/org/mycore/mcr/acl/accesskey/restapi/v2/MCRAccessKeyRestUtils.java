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

package org.mycore.mcr.acl.accesskey.restapi.v2;

import java.util.function.Supplier;

import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Utility methods for the access key REST API.
 */
public final class MCRAccessKeyRestUtils {

    private MCRAccessKeyRestUtils() {
    }

    /**
     * Executes the given action and converts known access key client errors to
     * {@link MCRErrorResponse} exceptions, passing the error code through.
     * <ul>
     *   <li>{@link MCRAccessKeyNotFoundException} to 404 Not Found</li>
     *   <li>{@link MCRAccessKeyCollisionException} to 409 Conflict</li>
     *   <li>{@link MCRAccessKeyValidationException} to 400 Bad Request</li>
     * </ul>
     * All other exceptions are rethrown unchanged.
     *
     * @param action the action to execute
     * @param <T> the result type
     * @return the result of the action
     * @throws WebApplicationException if a known client error occurs
     */
    public static <T> T handleClientErrors(Supplier<T> action) {
        try {
            return action.get();
        } catch (MCRAccessKeyNotFoundException e) {
            throw toWebApplicationException(Response.Status.NOT_FOUND, e);
        } catch (MCRAccessKeyCollisionException e) {
            throw toWebApplicationException(Response.Status.CONFLICT, e);
        } catch (MCRAccessKeyValidationException e) {
            throw toWebApplicationException(Response.Status.BAD_REQUEST, e);
        }
    }

    /**
     * Executes the given action and converts known access key client errors.
     *
     * @param action the action to execute
     * @throws WebApplicationException if a known client error occurs
     * @see #handleClientErrors(Supplier)
     */
    public static void handleClientErrors(Runnable action) {
        handleClientErrors(() -> {
            action.run();
            return null;
        });
    }

    private static WebApplicationException toWebApplicationException(Response.Status status,
        MCRAccessKeyException exception) {
        return MCRErrorResponse.ofStatusCode(status.getStatusCode())
            .withCause(exception)
            .withMessage(exception.getMessage())
            .withDetail(exception.getClass().getName())
            .withErrorCode(exception.getErrorCode())
            .toException();
    }

}
