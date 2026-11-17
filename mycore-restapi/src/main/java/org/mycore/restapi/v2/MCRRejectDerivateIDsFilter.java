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

import java.util.List;

import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;

@Priority(Priorities.AUTHORIZATION - 1)
public class MCRRejectDerivateIDsFilter implements ContainerRequestFilter {

    private static final List<Variant> ERROR_VARIANTS = Variant
        .mediaTypes(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE).build();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String mcrId = requestContext.getUriInfo().getPathParameters().getFirst(PARAM_MCRID);
        if (mcrId == null || !MCRObjectID.isValid(mcrId)
            || !"derivate".equals(MCRObjectID.getInstance(mcrId).getTypeId())) {
            return;
        }
        Response response = MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
            .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
            .withMessage("MCRObject " + mcrId + " not found")
            .toException()
            .getResponse();
        Variant variant = requestContext.getRequest().selectVariant(ERROR_VARIANTS);
        requestContext.abortWith(Response.fromResponse(response)
            .type(variant != null ? variant.getMediaType() : MediaType.APPLICATION_JSON_TYPE)
            .build());
    }
}
