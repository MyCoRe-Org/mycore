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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;

import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Variant;

public class MCRErrorResponseMediaTypeFilterTest {

    private final MCRErrorResponseMediaTypeFilter filter = new MCRErrorResponseMediaTypeFilter();

    private final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

    private final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

    private final Request request = mock(Request.class);

    private final Annotation[] annotations = new Annotation[0];

    @Before
    public void setUp() {
        when(requestContext.getRequest()).thenReturn(request);
        when(responseContext.getEntityAnnotations()).thenReturn(annotations);
    }

    @Test
    public void errorResponseShouldBecomeJsonIfNeitherJsonNorXmlIsAccepted() {
        MCRErrorResponse error = MCRErrorResponse.fromStatus(404);
        when(responseContext.getEntity()).thenReturn(error);
        when(request.selectVariant(any())).thenReturn(null);

        filter.filter(requestContext, responseContext);

        verify(responseContext).setEntity(error, annotations, MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8"));
    }

    @Test
    public void errorResponseShouldKeepNegotiatedTypeIfXmlIsAccepted() {
        when(responseContext.getEntity()).thenReturn(MCRErrorResponse.fromStatus(404));
        when(request.selectVariant(any())).thenReturn(new Variant(MediaType.APPLICATION_XML_TYPE, (String) null, null));

        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).setEntity(any(), any(), any());
    }

    @Test
    public void otherEntitiesShouldNotBeTouched() {
        when(responseContext.getEntity()).thenReturn("file content");
        when(request.selectVariant(any())).thenReturn(null);

        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).setEntity(any(), any(), any());
    }
}
