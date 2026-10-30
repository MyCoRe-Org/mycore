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

package org.mycore.common.content.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MCRServletContentHelperTest extends MCRTestCase {

    private static final long IF_MODIFIED_SINCE = 1_000_000_000L;

    @Test
    public void testNotModifiedWithOlderContent() throws IOException {
        HttpServletResponse response = serveIfModifiedSince(IF_MODIFIED_SINCE - 5000);
        verify(response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    public void testServedWithNewerContent() throws IOException {
        HttpServletResponse response = serveIfModifiedSince(IF_MODIFIED_SINCE + 5000);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    public void testServedWithUnknownLastModified() throws IOException {
        HttpServletResponse response = serveIfModifiedSince(-1);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }

    private static HttpServletResponse serveIfModifiedSince(long lastModified) throws IOException {
        MCRContent content = new MCRByteContent("<root />".getBytes(StandardCharsets.UTF_8), lastModified);
        content.setName("test.xml");
        content.setMimeType("text/xml");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getDateHeader(anyString())).thenReturn(-1L);
        when(request.getDateHeader("If-Modified-Since")).thenReturn(IF_MODIFIED_SINCE);
        when(request.getMethod()).thenReturn("GET");
        HttpServletResponse response = mock(HttpServletResponse.class);

        MCRServletContentHelper.serveContent(content, request, response, null, new MCRServletContentHelper.Config(),
            false);
        return response;
    }
}
