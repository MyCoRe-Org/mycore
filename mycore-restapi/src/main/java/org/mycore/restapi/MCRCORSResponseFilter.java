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
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MCRCORSResponseFilter implements ContainerResponseFilter {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ORIGIN = "Origin";

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    private static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    private static void handlePreFlight(ContainerRequestContext requestContext,
        MultivaluedMap<String, Object> responseHeaders,
        boolean authenticatedRequest) {
        if (!requestContext.getMethod().equals(HttpMethod.OPTIONS)) {
            return;
        }
        //allow all methods
        responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_METHODS, responseHeaders.getFirst(HttpHeaders.ALLOW));
        ArrayList<String> exposedHeaders = new ArrayList<>(); //todo: to be extended
        if (authenticatedRequest && responseHeaders.getFirst(HttpHeaders.AUTHORIZATION) != null) {
            exposedHeaders.add(HttpHeaders.AUTHORIZATION);
        }
        if (!exposedHeaders.isEmpty()) {
            responseHeaders.putSingle(ACCESS_CONTROL_EXPOSE_HEADERS,
                exposedHeaders.stream().collect(Collectors.joining(",")));
        }
        String requestHeaders = requestContext.getHeaderString(ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestHeaders != null) {
            //todo: may be restricted?
            responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_HEADERS, requestHeaders);
        }
        long cacheSeconds = TimeUnit.DAYS.toSeconds(1);
        responseHeaders.putSingle(ACCESS_CONTROL_MAX_AGE, cacheSeconds);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
        LOGGER.debug("Request-Header: {}", requestContext.getHeaders());
        String origin = requestContext.getHeaderString(ORIGIN);
        if (origin == null) {
            return; //No CORS Request
        }
        boolean authenticatedRequest = requestContext.getSecurityContext().getAuthenticationScheme() != null;
        MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();
        if (authenticatedRequest) {
            responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        }
        responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_ORIGIN, authenticatedRequest ? origin : "*");
        //todo: Access-Control-Expose-Headers
        handlePreFlight(requestContext, responseHeaders, authenticatedRequest);
        if (!"*".equals(responseHeaders.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN))) {
            String vary = Stream
                .concat(Stream.of(ACCESS_CONTROL_ALLOW_ORIGIN),
                    responseHeaders.getOrDefault(HttpHeaders.VARY, Collections.emptyList())
                        .stream()
                        .map(Object::toString)
                        .flatMap(s -> Stream.of(s.split(",")))
                        .map(String::trim))
                .distinct()
                .collect(Collectors.joining(","));
            responseHeaders.putSingle(HttpHeaders.VARY, vary);
        }
        LOGGER.debug("Response-Header: {}", responseHeaders);
    }

}
