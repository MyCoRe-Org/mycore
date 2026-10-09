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

package org.mycore.restapi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mycore.common.MCRSessionMgr;
import org.mycore.test.MyCoReTest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;

@MyCoReTest
public class MCRSessionFilterTest {

    @Test
    public void shouldCloseSessionWhenWritingEntityStreamFails() throws IOException {
        MCRSessionMgr.getCurrentSession();
        IOException clientAbort = new IOException("Client disconnected");
        OutputStream failingStream = new OutputStream() {
            @Override
            public void write(int data) throws IOException {
                throw clientAbort;
            }
        };

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        when(responseContext.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(responseContext.hasEntity()).thenReturn(true);
        when(responseContext.getEntityStream()).thenReturn(failingStream);

        new MCRSessionFilter().filter(requestContext, responseContext);

        ArgumentCaptor<OutputStream> streamCaptor = ArgumentCaptor.forClass(OutputStream.class);
        verify(responseContext).setEntityStream(streamCaptor.capture());

        IOException thrown = assertThrows(IOException.class, () -> streamCaptor.getValue().write(1));
        assertSame(clientAbort, thrown);
        assertFalse(MCRSessionMgr.hasCurrentSession(), "MCRSession must be detached after a client abort");
    }
}
