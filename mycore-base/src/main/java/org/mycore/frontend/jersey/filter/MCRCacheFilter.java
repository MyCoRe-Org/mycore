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

package org.mycore.frontend.jersey.filter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;
import org.mycore.services.http.MCRHttpStatus;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.RuntimeDelegate;

public class MCRCacheFilter implements ContainerResponseFilter {
    private static final RuntimeDelegate.HeaderDelegate<CacheControl> HEADER_DELEGATE = RuntimeDelegate.getInstance()
        .createHeaderDelegate(CacheControl.class);

    @Context
    private ResourceInfo resourceInfo;

    private CacheControl getCacheConrol(MCRCacheControl cacheControlAnnotation) {
        CacheControl cc = new CacheControl();
        if (cacheControlAnnotation != null) {
            cc.setMaxAge(
                (int) cacheControlAnnotation.maxAge().unit().toSeconds(cacheControlAnnotation.maxAge().time()));
            cc.setSMaxAge(
                (int) cacheControlAnnotation.sMaxAge().unit().toSeconds(cacheControlAnnotation.sMaxAge().time()));
            Optional.ofNullable(cacheControlAnnotation.codeIsPrivate())
                .filter(MCRCacheControl.FieldArgument::active)
                .map(MCRCacheControl.FieldArgument::fields)
                .map(Stream::of)
                .ifPresent(s -> {
                    cc.setPrivate(true);
                    cc.getPrivateFields().addAll(s.collect(Collectors.toList()));
                });
            if (cacheControlAnnotation.codeIsPublic()) {
                cc.getCacheExtension().put("public", null);
            }
            cc.setNoTransform(cacheControlAnnotation.noTransform());
            cc.setNoStore(cacheControlAnnotation.noStore());
            Optional.ofNullable(cacheControlAnnotation.noCache())
                .filter(MCRCacheControl.FieldArgument::active)
                .map(MCRCacheControl.FieldArgument::fields)
                .map(Stream::of)
                .ifPresent(s -> {
                    cc.setNoCache(true);
                    cc.getNoCacheFields().addAll(s.collect(Collectors.toList()));
                });
            cc.setMustRevalidate(cacheControlAnnotation.mustRevalidate());
            cc.setProxyRevalidate(cacheControlAnnotation.proxyRevalidate());
        } else {
            cc.setNoTransform(false); //should have been default
        }
        return cc;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        CacheControl cc;
        String currentCacheControl = requestContext.getHeaderString(HttpHeaders.CACHE_CONTROL);
        if (currentCacheControl != null) {
            if (responseContext.getHeaderString(HttpHeaders.AUTHORIZATION) == null) {
                return;
            }
            cc = RuntimeDelegate.getInstance()
                .createHeaderDelegate(CacheControl.class)
                .fromString(currentCacheControl);
        } else {
            //from https://developer.mozilla.org/en-US/docs/Glossary/cacheable
            if (!requestContext.getMethod().equals(HttpMethod.GET)
                && !requestContext.getMethod().equals(HttpMethod.HEAD)) {
                return;
            }
            boolean statusCacheable = IntStream
                .of(MCRHttpStatus.OK.value(), MCRHttpStatus.NON_AUTHORITATIVE_INFORMATION.value(),
                    MCRHttpStatus.NO_CONTENT.value(), MCRHttpStatus.PARTIAL_CONTENT.value(),
                    MCRHttpStatus.MULTIPLE_CHOICES.value(), MCRHttpStatus.MOVED_PERMANENTLY.value(),
                    MCRHttpStatus.NOT_FOUND.value(), MCRHttpStatus.METHOD_NOT_ALLOWED.value(),
                    MCRHttpStatus.GONE.value(), MCRHttpStatus.PAYLOAD_TOO_LARGE.value(),
                    MCRHttpStatus.NOT_IMPLEMENTED.value())
                .anyMatch(i -> i == responseContext.getStatus());
            if (!statusCacheable) {
                return;
            }
            cc = getCacheConrol(resourceInfo.getResourceMethod().getAnnotation(MCRCacheControl.class));
        }

        MCRRequestScopeACL aclProvider = MCRRequestScopeACL.extractFromRequestContext(requestContext);
        if (aclProvider.isPrivate()) {
            cc.setPrivate(true);
            cc.getPrivateFields().clear();
        }

        boolean isPrivate = cc.isPrivate() && cc.getPrivateFields().isEmpty();
        boolean isNoCache = cc.isNoCache() && cc.getNoCacheFields().isEmpty();
        if (responseContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null) {
            addAuthorizationHeaderException(cc, isPrivate, isNoCache);
        }
        String headerValue = HEADER_DELEGATE.toString(cc);
        LogManager.getLogger()
            .debug(() -> "Cache-Control filter: " + requestContext.getUriInfo().getPath() + " " + headerValue);
        responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, headerValue);
        if (Stream.of(resourceInfo.getResourceClass(), resourceInfo.getResourceMethod())
            .map(t -> t.getAnnotation(Produces.class))
            .filter(Objects::nonNull)
            .map(Produces::value)
            .flatMap(Stream::of)
            .distinct()
            .count() > 1) {
            //resource may produce differenct MediaTypes, we have to set Vary header
            List<String> varyHeaders = Optional.ofNullable(responseContext.getHeaderString(HttpHeaders.VARY))
                .map(Object::toString)
                .map(s -> s.split(","))
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .collect(Collectors.toList());
            if (!varyHeaders.contains(HttpHeaders.ACCEPT)) {
                varyHeaders.add(HttpHeaders.ACCEPT);
            }
            responseContext.getHeaders().putSingle(HttpHeaders.VARY,
                varyHeaders.stream().collect(Collectors.joining(",")));
        }
    }

    private void addAuthorizationHeaderException(CacheControl cc, boolean isPrivate, boolean isNoCache) {
        cc.setPrivate(true);
        if (!cc.getPrivateFields().contains(HttpHeaders.AUTHORIZATION) && !isPrivate) {
            cc.getPrivateFields().add(HttpHeaders.AUTHORIZATION);
        }
        cc.setNoCache(true);
        if (!cc.getNoCacheFields().contains(HttpHeaders.AUTHORIZATION) && !isNoCache) {
            cc.getNoCacheFields().add(HttpHeaders.AUTHORIZATION);
        }
    }

}
