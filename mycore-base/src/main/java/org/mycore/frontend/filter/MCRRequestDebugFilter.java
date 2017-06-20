/**
 * 
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRStreamUtils;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRRequestDebugFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger();

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
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
        StringBuilder sb = new StringBuilder("REQUEST (" + req.getMethod() + ") URI: " + req.getRequestURI() + " \n");
        logCookies(req, sb);
        logRequestParameters(request, sb);
        logSessionAttributes(req, sb);
        logHeader(MCRStreamUtils.asStream(req.getHeaderNames()), s -> MCRStreamUtils.asStream(req.getHeaders(s)), sb);
        return sb.append("\n\n").toString();
    }

    private String getLogMsg(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        StringBuilder sb = new StringBuilder("RESPONSE (" + req.getMethod() + ") URI: " + req.getRequestURI() + " \n");
        HttpServletResponse res = (HttpServletResponse) response;
        sb.append("Status: " + res.getStatus() + "\n");
        logHeader(res.getHeaderNames().stream(), s -> res.getHeaders(s).stream(), sb);
        return sb.append("\n\n").toString();
    }

    private void logHeader(Stream<String> headerNames, Function<String, Stream<String>> headerValues,
        StringBuilder sb) {
        sb.append("Header: \n");
        headerNames
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .forEachOrdered(header -> {
                headerValues
                    .apply(header)
                    .forEachOrdered(value -> {
                        sb
                            .append(header)
                            .append(": ")
                            .append(value)
                            .append("\n");
                    });
            });
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
                sb.append(" " + description + "\n");
            }
        }
        sb.append("COOKIES END \n\n");
    }

    private void logSessionAttributes(HttpServletRequest request, StringBuilder sb) {
        sb.append(
            "Session " + (request.isRequestedSessionIdFromCookie() ? "is" : "is not") + " requested by cookie.\n");
        sb.append("Session " + (request.isRequestedSessionIdFromURL() ? "is" : "is not") + " requested by URL.\n");
        sb.append("Session " + (request.isRequestedSessionIdValid() ? "is" : "is not") + " valid.\n");
        HttpSession session = request.getSession(false);
        if (session != null) {
            sb.append("SESSION " + request.getSession().getId() + " created at: " + LocalDateTime
                .ofInstant(Instant.ofEpochMilli(request.getSession().getCreationTime()), ZoneId.systemDefault())
                .toString() + "\n");
            sb.append("SESSION ATTRIBUTES: \n");
            MCRStreamUtils
                .asStream(session.getAttributeNames())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEachOrdered(attrName -> {
                    sb.append(" " + attrName + ": "
                        + getValue(attrName, Optional.ofNullable(session.getAttribute(attrName))) + "\n");
                });
            sb.append("SESSION ATTRIBUTES END \n\n");
        }
    }

    private String getValue(String key, Optional<Object> value) {
        String description = value.map(o -> {
            if (!hasSafeToString(o)) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> beanDescription = BeanUtils.describe(value);
                    if (!beanDescription.isEmpty()) {
                        return beanDescription.toString();
                    }
                } catch (Exception e) {
                    LOGGER.error("BeanUtils Exception describing attribute " + key, e);
                }
            }
            return o.toString();
        }).orElse("<null>");

        return description;
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
            .sorted((o1, o2) -> {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getKey(), o2.getKey());
            })
            .forEachOrdered(entry -> {
                sb.append(" " + entry.getKey() + ": ");
                for (String s : entry.getValue()) {
                    sb.append(s);
                    sb.append(", ");
                }
                sb.append("\n");
            });
        sb.append("REQUEST PARAMETERS END \n\n");
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }

}
