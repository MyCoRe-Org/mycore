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

package org.mycore.restapi.v2.access;

import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;

/**
 * Interface for defining authorization strategies in a REST API.
 * <p>
 * Implementations of this interface are used to perform access check
 * based on the incoming request context, the resource path, and required permissions.
 * </p>
 */
public interface MCRRestAccessCheckStrategy {

    /**
     * Checks if the access is granted based on the provided request context, resource path,
     * and required permissions.
     * <p>
     * If access is not permitted, a {@link ForbiddenException} is expected.
     * An {@link MCRErrorResponse} is recommended for creating this.
     * </p>
     *
     * @param resourceInfo the information about the requested resource
     * @param requestContext the context of the incoming request
     * @throws ForbiddenException if the user is not allowed
     *
     * @see MCRErrorResponse
     */
    void checkPermission(ResourceInfo resourceInfo, ContainerRequestContext requestContext) throws ForbiddenException;

}
