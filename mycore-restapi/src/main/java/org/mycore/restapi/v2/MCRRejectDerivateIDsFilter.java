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

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

/**
 * Rejects requests whose {@value MCRRestAuthorizationFilter#PARAM_MCRID} path parameter is a derivate ID.
 * <p>
 * All v2 resources use {@value MCRRestAuthorizationFilter#PARAM_MCRID} for the ID of an {@code MCRObject}.
 * Without this filter a derivate ID is accepted there, so e.g. {@code GET /objects/{derid}} returns the derivate
 * XML, {@code PUT} stores an object document under the derivate ID and {@code DELETE} runs the object delete path.
 * Derivates are addressed via {@value MCRRestAuthorizationFilter#PARAM_DERID} below their owner object, which is
 * not checked here.
 * <p>
 * The response is {@code 404} with {@link MCRErrorCodeConstants#MCROBJECT_NOT_FOUND}, as for a missing object.
 * The filter runs after resource matching, so the path parameter is resolved exactly as for the resource method,
 * and before {@link MCRRestAuthorizationFilter}, so the answer does not depend on permissions on the derivate.
 */
@Priority(Priorities.AUTHORIZATION - 1)
public class MCRRejectDerivateIDsFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String mcrId = requestContext.getUriInfo().getPathParameters().getFirst(PARAM_MCRID);
        if (mcrId == null || !MCRObjectID.isValid(mcrId)
            || !"derivate".equals(MCRObjectID.getInstance(mcrId).getTypeId())) {
            return;
        }
        throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
            .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
            .withMessage("MCRObject " + mcrId + " not found")
            .toException();
    }
}
