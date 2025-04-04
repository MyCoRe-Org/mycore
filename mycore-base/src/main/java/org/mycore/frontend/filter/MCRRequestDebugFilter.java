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

package org.mycore.frontend.filter;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRStreamUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRRequestDebugFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger();

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig)
     */

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        LOGGER.debug(() -> getLogMsg(request));
        chain.doFilter(request, response);
        LOGGER.debug(() -> getLogMsg(request, response));
    }

    private String getLogMsg(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        StringBuilder sb = new StringBuilder();
        sb.append("REQUEST (").append(req.getMethod()).append(") URI: ").append(req.getRequestURI()).append('\n');
        logCookies(req, sb);
        logRequestParameters(request, sb);
        logSessionAttributes(req, sb);
        logHeader(MCRStreamUtils.asStream(req.getHeaderNames()), s -> MCRStreamUtils.asStream(req.getHeaders(s)), sb);
        return sb.append("\n\n").toString();
    }

    private String getLogMsg(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        StringBuilder sb = new StringBuilder();
        sb.append("RESPONSE (").append(req.getMethod()).append(") URI: ").append(req.getRequestURI()).append('\n');
        HttpServletResponse res = (HttpServletResponse) response;
        sb.append("Status: ").append(res.getStatus()).append('\n');
        logHeader(res.getHeaderNames().stream(), s -> res.getHeaders(s).stream(), sb);
        return sb.append("\n\n").toString();
    }

    private void logHeader(Stream<String> headerNames, Function<String, Stream<String>> headerValues,
        StringBuilder sb) {
        sb.append("Header: \n");
        headerNames
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .forEachOrdered(header -> headerValues
                .apply(header)
                .forEachOrdered(value -> sb
                    .append(header)
                    .append(": ")
                    .append(value)
                    .append('\n')));
        sb.append("HEADERS END \n\n");
    }

    private void logCookies(HttpServletRequest request, StringBuilder sb) {
        sb.append("Cookies: \n");
        if (null != request.getCookies()) {
            for (Cookie cookie : request.getCookies()) {
                String description = "";
                try {
                    description = BeanUtils.describe(cookie).toString();
                } catch (Exception e) {
                    LOGGER.error("BeanUtils Exception describing cookie", e);
                }
                sb.append(' ').append(description).append('\n');
            }
        }
        sb.append("COOKIES END \n\n");
    }

    private void logSessionAttributes(HttpServletRequest request, StringBuilder sb) {
        sb.append("Session ")
            .append(request.isRequestedSessionIdFromCookie() ? "is" : "is not")
            .append(" requested by cookie.\n");
        sb.append("Session ")
            .append(request.isRequestedSessionIdFromURL() ? "is" : "is not")
            .append(" requested by URL.\n");
        sb.append("Session ").append(request.isRequestedSessionIdValid() ? "is" : "is not").append(" valid.\n");
        HttpSession session = request.getSession(false);
        if (session != null) {
            sb.append("SESSION ")
                .append(request.getSession().getId())
                .append(" created at: ")
                .append(LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getSession().getCreationTime()),
                    ZoneId.systemDefault()))
                .append('\n');
            sb.append("SESSION ATTRIBUTES: \n");
            MCRStreamUtils
                .asStream(session.getAttributeNames())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEachOrdered(attrName -> sb.append(' ')
                    .append(attrName)
                    .append(": ")
                    .append(getValue(attrName,
                        Optional.ofNullable(session.getAttribute(attrName))))
                    .append('\n'));
            sb.append("SESSION ATTRIBUTES END \n\n");
        }
    }

    private String getValue(String key, Optional<Object> value) {

        return value.map(o -> {
            if (!hasSafeToString(o)) {
                try {
                    Map<String, String> beanDescription = BeanUtils.describe(value);
                    if (!beanDescription.isEmpty()) {
                        return beanDescription.toString();
                    }
                } catch (Exception e) {
                    LOGGER.error("BeanUtils Exception describing attribute {}", key, e);
                }
            }
            return o.toString();
        }).orElse("<null>");
    }

    private static boolean hasSafeToString(Object o) {
        return o != null && Stream
            .of(CharSequence.class, Number.class, Boolean.class, Map.class, Collection.class, URI.class, URL.class,
                InetAddress.class, Throwable.class,
                Path.class, File.class, Class.class)
            .parallel()
            .filter(c -> c.isAssignableFrom(o.getClass()))
            .findAny()
            .isPresent();
    }

    private void logRequestParameters(ServletRequest request, StringBuilder sb) {
        sb.append("REQUEST PARAMETERS:\n");
        request.getParameterMap()
            .entrySet()
            .stream()
            .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getKey(), o2.getKey()))
            .forEachOrdered(entry -> {
                sb.append(' ').append(entry.getKey()).append(": ");
                for (String s : entry.getValue()) {
                    sb.append(s);
                    sb.append(", ");
                }
                sb.append('\n');
            });
        sb.append("REQUEST PARAMETERS END \n\n");
    }
}
