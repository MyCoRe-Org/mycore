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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;

public class MCRExceptionMapperTest {

    private MCRExceptionMapper mapper;

    @Before
    public void setUp() {
        mapper = new MCRExceptionMapper();
        mapper.request = mock(Request.class);
    }

    @Test
    public void toResponseShouldReturnErrorEntityForSupportedVariant() {
        when(mapper.request.selectVariant(any()))
            .thenReturn(new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, null));

        Response response = mapper.toResponse(new IllegalStateException("boom"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.hasEntity());
    }

    @Test
    public void toResponseShouldKeepWebApplicationExceptionResponseWithoutSupportedVariant() {
        when(mapper.request.selectVariant(any())).thenReturn(null);

        Response response = mapper.toResponse(new NotFoundException("missing"));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void toResponseShouldReturnServerErrorWithoutSupportedVariant() {
        when(mapper.request.selectVariant(any())).thenReturn(null);

        // a null response would be turned into 204 No Content by Jersey
        Response response = mapper.toResponse(new IllegalStateException("boom"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertFalse(response.hasEntity());
    }
}
