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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyTransformationException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyService;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class MCRAccessKeyRestResourceTest {

    private static final UUID ID = UUID.randomUUID();

    private MCRAccessKeyService accessKeyService;

    private MCRAccessKeyRestResource resource;

    @BeforeEach
    public void setUp() {
        accessKeyService = mock(MCRAccessKeyService.class);
        resource = new MCRAccessKeyRestResource(accessKeyService);
    }

    @Test
    public void testFindUnknownAccessKey() {
        final MCRAccessKeyNotFoundException exception = new MCRAccessKeyNotFoundException("not found");
        when(accessKeyService.findAccessKey(ID)).thenThrow(exception);
        assertErrorResponse(() -> resource.findAccessKey(ID), Response.Status.NOT_FOUND, exception);
    }

    @Test
    public void testAddInvalidAccessKey() {
        final MCRAccessKeyValidationException exception = new MCRAccessKeyValidationException("invalid");
        when(accessKeyService.addAccessKey(any())).thenThrow(exception);
        assertErrorResponse(() -> resource.addAccessKey(new MCRAccessKeyDto()), Response.Status.BAD_REQUEST,
            exception);
    }

    @Test
    public void testAddCollidingAccessKey() {
        final MCRAccessKeyCollisionException exception = new MCRAccessKeyCollisionException("collision");
        when(accessKeyService.addAccessKey(any())).thenThrow(exception);
        assertErrorResponse(() -> resource.addAccessKey(new MCRAccessKeyDto()), Response.Status.CONFLICT,
            exception);
    }

    @Test
    public void testUpdateUnknownAccessKey() {
        final MCRAccessKeyNotFoundException exception = new MCRAccessKeyNotFoundException("not found");
        when(accessKeyService.updateAccessKey(eq(ID), any())).thenThrow(exception);
        assertErrorResponse(() -> resource.updateAccessKey(ID, new MCRAccessKeyDto()), Response.Status.NOT_FOUND,
            exception);
    }

    @Test
    public void testUpdateCollidingAccessKey() {
        final MCRAccessKeyCollisionException exception = new MCRAccessKeyCollisionException("collision");
        when(accessKeyService.updateAccessKey(eq(ID), any())).thenThrow(exception);
        assertErrorResponse(() -> resource.updateAccessKey(ID, new MCRAccessKeyDto()), Response.Status.CONFLICT,
            exception);
    }

    @Test
    public void testPartialUpdateInvalidAccessKey() {
        final MCRAccessKeyValidationException exception = new MCRAccessKeyValidationException("invalid");
        when(accessKeyService.partialUpdateAccessKey(eq(ID), any())).thenThrow(exception);
        assertErrorResponse(() -> resource.partialUpdateAccessKey(ID, new MCRAccessKeyPartialUpdateDto()),
            Response.Status.BAD_REQUEST, exception);
    }

    @Test
    public void testRemoveUnknownAccessKey() {
        final MCRAccessKeyNotFoundException exception = new MCRAccessKeyNotFoundException("not found");
        doThrow(exception).when(accessKeyService).removeAccessKey(ID);
        assertErrorResponse(() -> resource.removeAccessKey(ID), Response.Status.NOT_FOUND, exception);
    }

    @Test
    public void testOtherExceptionIsNotConverted() {
        final MCRAccessKeyTransformationException exception = new MCRAccessKeyTransformationException("failed");
        when(accessKeyService.findAccessKey(ID)).thenThrow(exception);
        assertSame(exception, assertThrows(MCRAccessKeyTransformationException.class,
            () -> resource.findAccessKey(ID)));
    }

    private static void assertErrorResponse(Executable executable, Response.Status status,
        MCRAccessKeyException exception) {
        final WebApplicationException thrown = assertThrows(WebApplicationException.class, executable);
        assertEquals(status.getStatusCode(), thrown.getResponse().getStatus());
        final MCRErrorResponse errorResponse = (MCRErrorResponse) thrown.getResponse().getEntity();
        assertEquals(exception.getMessage(), errorResponse.getMessage());
        assertSame(exception, errorResponse.getCause());
        if (exception.getErrorCode() != null) {
            assertEquals(exception.getErrorCode(), errorResponse.getErrorCode());
        }
    }
}
