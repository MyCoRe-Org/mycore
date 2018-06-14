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
import java.io.OutputStream;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.logging.log4j.LogManager;

/**
 * Ignores IOException when writing to client OutputStream
 * @see <a href="https://stackoverflow.com/a/39006980">Stack Overflow</a>
 */
@Priority(1)
public class MCRIgnoreClientAbortInterceptor implements WriterInterceptor {
    @Override
    public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext)
        throws IOException, WebApplicationException {
        writerInterceptorContext
            .setOutputStream(new ClientAbortExceptionOutputStream(writerInterceptorContext.getOutputStream()));
        try {
            writerInterceptorContext.proceed();
        } catch (Exception e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof ClientAbortException) {
                    LogManager.getLogger().info("Client closed response too early.");
                    return;
                }
            }
            throw e;
        }
    }

    private static class ClientAbortExceptionOutputStream extends ProxyOutputStream {
        public ClientAbortExceptionOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        protected void handleIOException(IOException e) throws IOException {
            throw new ClientAbortException(e);
        }
    }

    @SuppressWarnings("serial")
    private static class ClientAbortException extends IOException {
        public ClientAbortException(IOException e) {
            super(e);
        }
    }
}
