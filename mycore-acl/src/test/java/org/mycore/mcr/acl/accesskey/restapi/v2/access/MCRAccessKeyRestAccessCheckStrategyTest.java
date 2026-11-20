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

package org.mycore.mcr.acl.accesskey.restapi.v2.access;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.mcr.acl.accesskey.access.MCRAccessKeyPermissionChecker;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.restapi.v2.MCRAccessKeyRestConstants;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyService;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class MCRAccessKeyRestAccessCheckStrategyTest {

    private MCRAccessKeyService accessKeyService;

    private MCRAccessKeyPermissionChecker permissionChecker;

    private MCRAccessKeyRestAccessCheckStrategy strategy;

    private ContainerRequestContext requestContext;

    private MultivaluedMap<String, String> pathParameters;

    @BeforeEach
    public void setUp() {
        accessKeyService = mock(MCRAccessKeyService.class);
        permissionChecker = mock(MCRAccessKeyPermissionChecker.class);
        strategy = new MCRAccessKeyRestAccessCheckStrategy(accessKeyService, permissionChecker);
        requestContext = mock(ContainerRequestContext.class);
        pathParameters = new MultivaluedHashMap<>();
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters()).thenReturn(pathParameters);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test
    public void testUnknownAccessKey() {
        final UUID id = UUID.randomUUID();
        final MCRAccessKeyNotFoundException exception = new MCRAccessKeyNotFoundException("not found");
        when(accessKeyService.findAccessKey(id)).thenThrow(exception);
        prepareRequest(HttpMethod.DELETE, id.toString());
        final WebApplicationException thrown = assertThrows(WebApplicationException.class,
            () -> strategy.checkPermission(mock(ResourceInfo.class), requestContext));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), thrown.getResponse().getStatus());
        assertEquals(exception.getErrorCode(),
            ((MCRErrorResponse) thrown.getResponse().getEntity()).getErrorCode());
    }

    @Test
    public void testInvalidAccessKeyId() {
        prepareRequest(HttpMethod.GET, "invalid");
        final WebApplicationException thrown = assertThrows(WebApplicationException.class,
            () -> strategy.checkPermission(mock(ResourceInfo.class), requestContext));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), thrown.getResponse().getStatus());
        verify(accessKeyService, never()).findAccessKey(any());
    }

    @Test
    public void testPermittedAccessKey() {
        final UUID id = UUID.randomUUID();
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        when(accessKeyService.findAccessKey(id)).thenReturn(accessKeyDto);
        when(permissionChecker.canManageAccessKey(accessKeyDto)).thenReturn(true);
        prepareRequest(HttpMethod.DELETE, id.toString());
        assertDoesNotThrow(() -> strategy.checkPermission(mock(ResourceInfo.class), requestContext));
    }

    @Test
    public void testForbiddenAccessKey() {
        final UUID id = UUID.randomUUID();
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        when(accessKeyService.findAccessKey(id)).thenReturn(accessKeyDto);
        when(permissionChecker.canManageAccessKey(accessKeyDto)).thenReturn(false);
        prepareRequest(HttpMethod.DELETE, id.toString());
        assertThrows(ForbiddenException.class,
            () -> strategy.checkPermission(mock(ResourceInfo.class), requestContext));
    }

    @Test
    public void testPoolPrivilegeSkipsLookup() {
        when(permissionChecker.checkPoolPrivilege()).thenReturn(true);
        prepareRequest(HttpMethod.DELETE, "invalid");
        assertDoesNotThrow(() -> strategy.checkPermission(mock(ResourceInfo.class), requestContext));
        verify(accessKeyService, never()).findAccessKey(any());
    }

    private void prepareRequest(String method, String id) {
        when(requestContext.getMethod()).thenReturn(method);
        pathParameters.putSingle(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID, id);
    }
}
