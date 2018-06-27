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

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Removed request message body if {@link #IGNORE_MESSAGE_BODY_HEADER} is set.
 *
 * Some user agent may set a {@link HttpHeaders#CONTENT_LENGTH} header, even if no message body was send.
 * This filter removes Entity and the <code>Content-Length</code> and <code>Transfer-Encoding</code> header.
 */
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public class MCRRemoveMsgBodyFilter implements ContainerRequestFilter {

    public static final String IGNORE_MESSAGE_BODY_HEADER = "X-MCR-Ignore-Message-Body";

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        if (headers.containsKey(IGNORE_MESSAGE_BODY_HEADER)) {
            LOGGER.info("Found {} header. Remove request message body.", IGNORE_MESSAGE_BODY_HEADER);
            headers.remove(IGNORE_MESSAGE_BODY_HEADER);
            headers.remove(HttpHeaders.CONTENT_LENGTH);
            headers.remove("Transfer-Encoding");
            requestContext.setEntityStream(null);
        }
    }
}
