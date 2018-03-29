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

package org.mycore.frontend.jersey.filter;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;

public class MCRCacheFilter implements ContainerResponseFilter {
    private static final RuntimeDelegate.HeaderDelegate<CacheControl> HEADER_DELEGATE = RuntimeDelegate.getInstance()
        .createHeaderDelegate(CacheControl.class);

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    private MCRRequestScopeACL aclProvider;

    private CacheControl getCacheConrol(MCRCacheControl cacheControlAnnotation) {
        CacheControl cc = new CacheControl();
        if (cacheControlAnnotation != null) {
            cc.setMaxAge(
                (int) cacheControlAnnotation.maxAge().unit().toSeconds(cacheControlAnnotation.maxAge().time()));
            cc.setSMaxAge(
                (int) cacheControlAnnotation.sMaxAge().unit().toSeconds(cacheControlAnnotation.sMaxAge().time()));
            Optional.ofNullable(cacheControlAnnotation.private_())
                .filter(MCRCacheControl.FieldArgument::active)
                .map(MCRCacheControl.FieldArgument::fields)
                .map(Stream::of)
                .ifPresent(s -> {
                    cc.setPrivate(true);
                    cc.getPrivateFields().addAll(s.collect(Collectors.toList()));
                });
            if (cacheControlAnnotation.public_()) {
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
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
        CacheControl cc;
        String currentCacheControl = requestContext.getHeaderString(HttpHeaders.CACHE_CONTROL);
        if (currentCacheControl != null) {
            if (responseContext.getHeaderString(HttpHeaders.AUTHORIZATION) == null) {
                return;
            }
            cc = CacheControl.valueOf(currentCacheControl);
        } else {
            //from https://developer.mozilla.org/en-US/docs/Glossary/cacheable
            if (!requestContext.getMethod().equals(HttpMethod.GET)
                && !requestContext.getMethod().equals(HttpMethod.HEAD)) {
                return;
            }
            boolean statusCacheable = IntStream
                .of(HttpStatus.SC_OK, HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, HttpStatus.SC_NO_CONTENT,
                    HttpStatus.SC_PARTIAL_CONTENT, HttpStatus.SC_MULTIPLE_CHOICES, HttpStatus.SC_MOVED_PERMANENTLY,
                    HttpStatus.SC_NOT_FOUND, HttpStatus.SC_METHOD_NOT_ALLOWED, HttpStatus.SC_GONE,
                    HttpStatus.SC_REQUEST_URI_TOO_LONG, HttpStatus.SC_NOT_IMPLEMENTED)
                .anyMatch(i -> i == responseContext.getStatus());
            if (!statusCacheable) {
                return;
            }
            cc = getCacheConrol(resourceInfo.getResourceMethod().getAnnotation(MCRCacheControl.class));
        }

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
