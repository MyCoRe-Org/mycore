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
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;
import org.mycore.restapi.converter.MCRDetailLevel;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;
import org.mycore.restapi.v2.access.MCRRestAccessCheckStrategy;
import org.mycore.restapi.v2.access.MCRRestAccessManager;
import org.mycore.restapi.v2.annotation.MCRRestAccessCheck;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;

@Priority(Priorities.AUTHORIZATION)
public class MCRRestAuthorizationFilter implements ContainerRequestFilter {

    public static final String PARAM_CLASSID = "classid";

    public static final String PARAM_MCRID = "mcrid";

    public static final String PARAM_DERID = "derid";

    public static final String PARAM_DER_PATH = "path";

    public static final ParamConverter<MCRObjectID> OBJECT_ID_PARAM_CONVERTER = new MCRObjectIDParamConverterProvider()
        .getConverter(MCRObjectID.class, null, null);

    @Context
    ResourceInfo resourceInfo;

    /**
     * checks if the given REST API operation is allowed
     * @param permission "read" or "write"
     * @param path - the REST API path, e.g. /v1/messages
     *
     * @throws jakarta.ws.rs.ForbiddenException if access is restricted
     */
    private void checkRestAPIAccess(ContainerRequestContext requestContext, String permission, String path)
        throws ForbiddenException {
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

    private void checkBaseAccess(ContainerRequestContext requestContext, String permission, String path)
        throws ForbiddenException {
        final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        final String objectId = pathParameters.getFirst(PARAM_MCRID);
        final String derId = pathParameters.getFirst(PARAM_DERID);
        LogManager.getLogger().debug("Permission: {}, Object: {}, Derivate: {}, Path: {}", permission, objectId, derId,
            path);
        Optional<String> checkable = Optional.ofNullable(derId)
            .filter(d -> path != null) //only check for derId if path is given
            .map(Optional::of)
            .orElseGet(() -> Optional.ofNullable(objectId))
            .map(OBJECT_ID_PARAM_CONVERTER::fromString) //MCR-3041 check for Bad Request
            .map(MCRObjectID::toString);
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
        final String permission
            = Optional.ofNullable(resourceInfo.getResourceMethod().getAnnotation(MCRRestRequiredPermission.class))
                .map(MCRRestRequiredPermission::value).orElseGet(() -> getPermissionFromHttpMethod(method));
        Optional.ofNullable(resourceInfo.getResourceClass().getAnnotation(Path.class)).map(Path::value)
            .ifPresent(path -> {
                checkRestAPIAccess(requestContext, permission, path);
                final MCRRestAccessCheck accessCheckAnnotation
                    = resourceInfo.getResourceMethod().getAnnotation(MCRRestAccessCheck.class);
                if (accessCheckAnnotation != null) {
                    final MCRRestAccessCheckStrategy strategy = resolveAccessCheckStrategy(accessCheckAnnotation);
                    strategy.checkPermission(resourceInfo, requestContext);
                } else {
                    checkBaseAccess(requestContext, permission, path);
                }
            });
        checkDetailLevel(requestContext,
            requestContext.getAcceptableMediaTypes()
                .stream()
                .map(m -> m.getParameters().get(MCRDetailLevel.MEDIA_TYPE_PARAMETER))
                .filter(Objects::nonNull)
                .toArray(String[]::new));
    }

    private MCRRestAccessCheckStrategy resolveAccessCheckStrategy(MCRRestAccessCheck annotation) {
        final Class<? extends MCRRestAccessCheckStrategy> strategyClass = annotation.strategy();
        try {
            return strategyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
    }

    private static String getPermissionFromHttpMethod(String httpMethod) {
        return switch (httpMethod) {
            case HttpMethod.GET, HttpMethod.HEAD -> MCRAccessManager.PERMISSION_READ;
            case HttpMethod.DELETE -> MCRAccessManager.PERMISSION_DELETE;
            case HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH -> MCRAccessManager.PERMISSION_WRITE;
            default -> throw new IllegalArgumentException("Unknown HTTP method: " + httpMethod);
        };
    }
}
