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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.restapi.converter.MCRDetailLevel;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;

public class MCRDetailLevelResolverTest {

    @Test
    void resolveShouldReturnNormalWhenNoDetailParameterGiven() {
        ContainerRequestContext request = requestWithAccept(MediaType.APPLICATION_JSON_TYPE);

        assertEquals(MCRDetailLevel.NORMAL, MCRDetailLevelResolver.resolve(request));
    }

    @Test
    void resolveShouldMatchUppercaseValue() {
        ContainerRequestContext request = requestWithAccept(mediaType("SUMMARY"));

        assertEquals(MCRDetailLevel.SUMMARY, MCRDetailLevelResolver.resolve(request));
    }

    @Test
    void resolveShouldMatchLowercaseValue() {
        ContainerRequestContext request = requestWithAccept(mediaType("summary"));

        assertEquals(MCRDetailLevel.SUMMARY, MCRDetailLevelResolver.resolve(request));
    }

    @Test
    void resolveShouldMatchMixedCaseValue() {
        ContainerRequestContext request = requestWithAccept(mediaType("DeTaIlEd"));

        assertEquals(MCRDetailLevel.DETAILED, MCRDetailLevelResolver.resolve(request));
    }

    @Test
    void resolveShouldThrowBadRequestForUnknownValue() {
        ContainerRequestContext request = requestWithAccept(mediaType("foo"));

        assertThrows(BadRequestException.class, () -> MCRDetailLevelResolver.resolve(request));
    }

    private MediaType mediaType(String detailLevel) {
        return new MediaType("application", "json", Map.of("detail", detailLevel));
    }

    private ContainerRequestContext requestWithAccept(MediaType mediaType) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(mediaType));
        return request;
    }
}
