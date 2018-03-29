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

package org.mycore.restapi;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.errors.MCRRestAPIExceptionMapper;

@Priority(Priorities.AUTHORIZATION)
public class MCRRestAuthorizationFilter implements ContainerRequestFilter {

    public static final String PARAM_MCRID = "mcrid";

    public static final String PARAM_DERID = "derid";

    @Context
    ResourceInfo resourceInfo;

    @Inject
    private MCRRequestScopeACL aclProvider;

    /**
     * checks if the given REST API operation is allowed
     * @param permission "read" or "write"
     * @param path - the REST API path, e.g. /v1/messages
     *
     * @throws MCRRestAPIException if access is restricted
     */
    private void checkRestAPIAccess(MCRRestAPIACLPermission permission, String path)
        throws MCRRestAPIException {
        LogManager.getLogger().warn(path + ": Checking API access: " + permission);
        String thePath = path.startsWith("/") ? path : "/" + path;

        MCRAccessInterface acl = MCRAccessControlSystem.instance();
        String permStr = permission.toString();
        MCRRequestScopeACL requestScopeACL = aclProvider;
        boolean hasAPIAccess = requestScopeACL.checkPermission("restapi://",
            permStr);
        if (hasAPIAccess) {
            String objId = "restapi:" + thePath;
            if (acl.hasRule(objId, permStr)) {
                if (requestScopeACL.checkPermission(objId, permStr)) {
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

    private void checkBaseAccess(MCRRestAPIACLPermission permission, String objectId, String derId)
        throws MCRRestAPIException {
        Optional<String> checkable = Optional.ofNullable(derId)
            .map(Optional::of)
            .orElseGet(() -> Optional.ofNullable(objectId));
        checkable.ifPresent(id -> LogManager.getLogger().warn("Checking " + permission + " access on " + id));
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
                    checkRestAPIAccess(permission, path);
                    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
                    checkBaseAccess(permission, pathParameters.getFirst(PARAM_MCRID),
                        pathParameters.getFirst(PARAM_DERID));
                } catch (MCRRestAPIException e) {
                    LogManager.getLogger().warn("API Access denied!");
                    requestContext.abortWith(new MCRRestAPIExceptionMapper().toResponse(e));
                }
            });
    }

    /**
     * The REST API access permissions (read, write)
     */
    public enum MCRRestAPIACLPermission {
        READ {
            public String toString() {
                return "read";
            }
        },

        WRITE {
            public String toString() {
                return "write";
            }
        },

        DELETE {
            public String toString() {
                return "delete";
            }
        }
    }
}
