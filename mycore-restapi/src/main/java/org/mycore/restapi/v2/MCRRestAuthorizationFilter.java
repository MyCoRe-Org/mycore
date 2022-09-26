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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;
import org.mycore.restapi.converter.MCRDetailLevel;
import org.mycore.restapi.v2.access.MCRRestAPIACLPermission;
import org.mycore.restapi.v2.access.MCRRestAccessManager;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Priority(Priorities.AUTHORIZATION)
public class MCRRestAuthorizationFilter implements ContainerRequestFilter {

    public static final String PARAM_CLASSID = "classid";

    public static final String PARAM_MCRID = "mcrid";

    public static final String PARAM_DERID = "derid";

    public static final String PARAM_DER_PATH = "path";

    @Context
    ResourceInfo resourceInfo;

    /**
     * checks if the given REST API operation is allowed
     * @param permission "read" or "write"
     * @param path - the REST API path, e.g. /v1/messages
     *
     * @throws jakarta.ws.rs.ForbiddenException if access is restricted
     */
    private void checkRestAPIAccess(final ContainerRequestContext requestContext,
        final MCRRestAPIACLPermission permission, final String path) throws ForbiddenException {
        LogManager.getLogger().warn(path + ": Checking API access: " + permission);
        final MCRRequestScopeACL aclProvider = MCRRequestScopeACL.getInstance(requestContext);
        if (MCRRestAccessManager.checkRestAPIAccess(aclProvider, permission, path)) {
            return;
        }
        throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
            .withErrorCode(MCRErrorCodeConstants.API_NO_PERMISSION)
            .withMessage("REST-API action is not allowed.")
            .withDetail("Check access right '" + permission + "' on ACLs 'restapi:/' and 'restapi:" + path + "'!")
            .toException();
    }

    private void checkBaseAccess(ContainerRequestContext requestContext, MCRRestAPIACLPermission permission,
        String objectId, String derId, String path)
        throws ForbiddenException {
        LogManager.getLogger().debug("Permission: {}, Object: {}, Derivate: {}, Path: {}", permission, objectId, derId,
            path);
        Optional<String> checkable = Optional.ofNullable(derId)
            .filter(d -> path != null) //only check for derId if path is given
            .map(Optional::of)
            .orElseGet(() -> Optional.ofNullable(objectId));
        checkable.ifPresent(id -> LogManager.getLogger().info("Checking " + permission + " access on " + id));
        MCRRequestScopeACL aclProvider = MCRRequestScopeACL.getInstance(requestContext);
        boolean allowed = checkable
            .map(id -> aclProvider.checkPermission(id, permission.toString()))
            .orElse(true);
        if (allowed) {
            return;
        }
        if (checkable.get().equals(objectId)) {
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NO_PERMISSION)
                .withMessage("You do not have " + permission + " permission on MCRObject " + objectId + ".")
                .toException();
        }
        throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
            .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NO_PERMISSION)
            .withMessage("You do not have " + permission + " permission on MCRDerivate " + derId + ".")
            .toException();
    }

    private void checkDetailLevel(ContainerRequestContext requestContext, String... detail) throws ForbiddenException {
        MCRRequestScopeACL aclProvider = MCRRequestScopeACL.getInstance(requestContext);
        List<String> missedPermissions = Stream.of(detail)
            .map(d -> "rest-detail-" + d)
            .filter(d -> MCRAccessManager.hasRule(MCRAccessControlSystem.POOL_PRIVILEGE_ID, d))
            .filter(d -> !aclProvider.checkPermission(d))
            .collect(Collectors.toList());
        if (!missedPermissions.isEmpty()) {
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.API_NO_PERMISSION)
                .withMessage("REST-API action is not allowed.")
                .withDetail("Check access right(s) '" + missedPermissions + "' on "
                    + MCRAccessControlSystem.POOL_PRIVILEGE_ID + "'!")
                .toException();
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final String method = requestContext.getMethod();
        if (HttpMethod.OPTIONS.equals(method)) {
            return;
        }
        final MCRRestRequiredPermission annotation
            = resourceInfo.getResourceMethod().getAnnotation(MCRRestRequiredPermission.class);
        final MCRRestAPIACLPermission permission = Optional.ofNullable(annotation)
            .map(a -> a.value())
            .orElseGet(() -> MCRRestAPIACLPermission.fromMethod(method));
        Optional.ofNullable(resourceInfo.getResourceClass().getAnnotation(Path.class))
            .map(Path::value)
            .ifPresent(path -> {
                checkRestAPIAccess(requestContext, permission, path);
                MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
                checkBaseAccess(requestContext, permission, pathParameters.getFirst(PARAM_MCRID),
                    pathParameters.getFirst(PARAM_DERID), pathParameters.getFirst(PARAM_DER_PATH));
            });
        checkDetailLevel(requestContext,
            requestContext.getAcceptableMediaTypes()
                .stream()
                .map(m -> m.getParameters().get(MCRDetailLevel.MEDIA_TYPE_PARAMETER))
                .filter(Objects::nonNull)
                .toArray(String[]::new));
    }
}
