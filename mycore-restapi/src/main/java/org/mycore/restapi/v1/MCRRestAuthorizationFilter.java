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

package org.mycore.restapi.v1;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;
import org.mycore.restapi.converter.MCRDetailLevel;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.errors.MCRRestAPIExceptionMapper;

import jakarta.annotation.Priority;
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
     * @throws MCRRestAPIException if access is restricted
     */
    private void checkRestAPIAccess(ContainerRequestContext requestContext, MCRRestAPIACLPermission permission,
        String path)
        throws MCRRestAPIException {
        MCRRequestScopeACL aclProvider = MCRRequestScopeACL.getInstance(requestContext);
        LogManager.getLogger().warn(path + ": Checking API access: " + permission);
        String thePath = path.startsWith("/") ? path : "/" + path;

        MCRAccessInterface acl = MCRAccessManager.getAccessImpl();
        String permStr = permission.toString();
        boolean hasAPIAccess = aclProvider.checkPermission("restapi:/", permStr);
        if (hasAPIAccess) {
            String objId = "restapi:" + thePath;
            boolean isRuleInterface = acl instanceof MCRRuleAccessInterface;
            if (!isRuleInterface || ((MCRRuleAccessInterface) acl).hasRule(objId, permStr)) {
                if (aclProvider.checkPermission(objId, permStr)) {
                    return;
                }
            } else {
                return;
            }
        }
        throw new MCRRestAPIException(Response.Status.FORBIDDEN,
            new MCRRestAPIError(MCRRestAPIError.CODE_ACCESS_DENIED, "REST-API action is not allowed.",
                "Check access right '" + permission + "' on ACLs 'restapi:/' and 'restapi:" + path + "'!"));
    }

    private void checkBaseAccess(ContainerRequestContext requestContext, MCRRestAPIACLPermission permission,
        String objectId, String derId, String path)
        throws MCRRestAPIException {
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
        throw new MCRRestAPIException(Response.Status.FORBIDDEN,
            new MCRRestAPIError(MCRRestAPIError.CODE_ACCESS_DENIED, "REST-API action is not allowed.",
                "Check access right '" + permission + "' on '" + checkable.orElse(null) + "'!"));
    }

    private void checkDetailLevel(ContainerRequestContext requestContext, String... detail) throws MCRRestAPIException {
        MCRRequestScopeACL aclProvider = MCRRequestScopeACL.getInstance(requestContext);
        List<String> missedPermissions = Stream.of(detail)
            .map(d -> "rest-detail-" + d)
            .filter(d -> MCRAccessManager.hasRule(MCRAccessControlSystem.POOL_PRIVILEGE_ID, d))
            .filter(d -> !aclProvider.checkPermission(d))
            .collect(Collectors.toList());
        if (!missedPermissions.isEmpty()) {
            throw new MCRRestAPIException(Response.Status.FORBIDDEN,
                new MCRRestAPIError(MCRRestAPIError.CODE_ACCESS_DENIED, "REST-API action is not allowed.",
                    "Check permission(s) " + missedPermissions + "!"));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MCRRestAPIACLPermission permission;
        switch (requestContext.getMethod()) {
            case HttpMethod.OPTIONS:
                return;
            case HttpMethod.GET:
            case HttpMethod.HEAD:
                permission = MCRRestAPIACLPermission.READ;
                break;
            case HttpMethod.DELETE:
                permission = MCRRestAPIACLPermission.DELETE;
                break;
            default:
                permission = MCRRestAPIACLPermission.WRITE;
        }
        Optional.ofNullable(resourceInfo.getResourceClass().getAnnotation(Path.class))
            .map(Path::value)
            .ifPresent(path -> {
                try {
                    checkRestAPIAccess(requestContext, permission, path);
                    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
                    checkBaseAccess(requestContext, permission, pathParameters.getFirst(PARAM_MCRID),
                        pathParameters.getFirst(PARAM_DERID), pathParameters.getFirst(PARAM_DER_PATH));
                } catch (MCRRestAPIException e) {
                    LogManager.getLogger().warn("API Access denied!");
                    requestContext.abortWith(new MCRRestAPIExceptionMapper().toResponse(e));
                }
            });
        try {
            checkDetailLevel(requestContext,
                requestContext.getAcceptableMediaTypes()
                    .stream()
                    .map(m -> m.getParameters().get(MCRDetailLevel.MEDIA_TYPE_PARAMETER))
                    .filter(Objects::nonNull)
                    .toArray(String[]::new));
        } catch (MCRRestAPIException e) {
            LogManager.getLogger().warn("API Access denied!");
            requestContext.abortWith(new MCRRestAPIExceptionMapper().toResponse(e));
        }

    }

    /**
     * The REST API access permissions (read, write, delete)
     */
    public enum MCRRestAPIACLPermission {
        READ {
            public String toString() {
                return MCRAccessManager.PERMISSION_READ;
            }
        },

        WRITE {
            public String toString() {
                return MCRAccessManager.PERMISSION_WRITE;
            }
        },

        DELETE {
            public String toString() {
                return MCRAccessManager.PERMISSION_DELETE;
            }
        }
    }
}
