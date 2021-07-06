/*
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

package org.mycore.mcr.acl.accesskey.restapi;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.mycore.access.MCRAccessManager;
import org.mycore.mcr.acl.accesskey.restapi.annotation.MCRRequireAccessKeyAuthorization;
import org.mycore.restapi.v2.MCRErrorResponse;

@Provider
@MCRRequireAccessKeyAuthorization
public class MCRAccessKeyAuthorizationFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getMethod().equals(HttpMethod.OPTIONS)) {
            return;
        } else {
            final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
            if (MCRAccessManager.checkPermission(pathParameters.
                getFirst(PARAM_MCRID), MCRAccessManager.PERMISSION_WRITE)) {
                return;
            }
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .toException();
        }
    }
}
