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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

@Priority(Priorities.AUTHENTICATION - 1)
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

    @Context
    ResourceInfo resourceInfo;

    private static boolean handlePreFlight(ContainerRequestContext requestContext,
        MultivaluedMap<String, Object> responseHeaders) {
        if (!requestContext.getMethod().equals(HttpMethod.OPTIONS)) {
            return false;
        }
        //allow all methods
        responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_METHODS, responseHeaders.getFirst(HttpHeaders.ALLOW));
        String requestHeaders = requestContext.getHeaderString(ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestHeaders != null) {
            //todo: may be restricted?
            responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_HEADERS, requestHeaders);
        }
        long cacheSeconds = TimeUnit.DAYS.toSeconds(1);
        responseHeaders.putSingle(ACCESS_CONTROL_MAX_AGE, cacheSeconds);
        return true;
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
        if (!handlePreFlight(requestContext, responseHeaders)) {
            //not a CORS preflight request
            ArrayList<String> exposedHeaders = new ArrayList<>();
            if (authenticatedRequest && responseHeaders.getFirst(HttpHeaders.AUTHORIZATION) != null) {
                exposedHeaders.add(HttpHeaders.AUTHORIZATION);
            }
            Optional.ofNullable(resourceInfo)
                    .map(ResourceInfo::getResourceMethod)
                    .map(method->method.getAnnotation(MCRAccessControlExposeHeaders.class))
                    .map(MCRAccessControlExposeHeaders::value)
                    .map(Stream::of)
                    .orElse(Stream.empty())
                    .forEach(exposedHeaders::add);
            if (!exposedHeaders.isEmpty()) {
                responseHeaders.putSingle(ACCESS_CONTROL_EXPOSE_HEADERS,
                    exposedHeaders.stream().collect(Collectors.joining(",")));
            }
        }
        if (!"*".equals(responseHeaders.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN))) {
            String vary = Stream
                .concat(Stream.of(ORIGIN),
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
