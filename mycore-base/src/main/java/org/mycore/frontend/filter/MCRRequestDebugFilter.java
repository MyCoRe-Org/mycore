/**
 * 
 */
package org.mycore.frontend.filter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUtils;

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
        StringBuilder sb = new StringBuilder("REQUEST URI: " + req.getRequestURI() + " \n");
        logCookies(req, sb);
        logRequestParameters(request, sb);
        logSessionAttributes(req, sb);
        logHeader(MCRUtils.asStream(req.getHeaderNames()), s -> MCRUtils.asStream(req.getHeaders(s)), sb);
        return sb.append("\n\n").toString();
    }

    private String getLogMsg(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        StringBuilder sb = new StringBuilder("RESPONSE URI: " + req.getRequestURI() + " \n");
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
        if (request.getSession(false) != null) {
            sb.append("SESSION " + request.getSession().getId() + " created at: " + LocalDateTime
                .ofInstant(Instant.ofEpochMilli(request.getSession().getCreationTime()), ZoneId.systemDefault())
                .toString() + "\n");
            sb.append("SESSION ATTRIBUTES: \n");
            Map<String, Object> sortedAttrs = sortSessionAttributes(request);
            for (Map.Entry<String, Object> entry : sortedAttrs.entrySet()) {
                String description = "";
                try {
                    description = BeanUtils.describe(entry.getValue()).toString();
                } catch (Exception e) {
                    LOGGER.error("BeanUtils Exception describing attribute " + entry.getKey(), e);
                }
                sb.append(" " + entry.getKey() + ": " + description + "\n");
            }
            sb.append("SESSION ATTRIBUTES END \n\n");
        }
    }

    private void logRequestParameters(ServletRequest request, StringBuilder sb) {
        sb.append("REQUEST PARAMETERS:\n");
        Map<String, String[]> sortedParams = sortRequestParameters(request);
        for (Map.Entry<String, String[]> entry : sortedParams.entrySet()) {
            StringBuilder builder = new StringBuilder();
            for (String s : entry.getValue()) {
                builder.append(s);
                builder.append(", ");
            }
            sb.append(" " + entry.getKey() + ": " + builder.toString() + "\n");
        }
        sb.append("REQUEST PARAMETERS END \n\n");
    }

    private Map<String, Object> sortSessionAttributes(HttpServletRequest request) {
        Map<String, Object> sortedAttrs = new TreeMap<String, Object>();
        Enumeration<String> attrEnum = request.getSession().getAttributeNames();
        while (attrEnum.hasMoreElements()) {
            String s = attrEnum.nextElement();
            sortedAttrs.put(s, request.getAttribute(s));
        }
        return sortedAttrs;
    }

    private Map<String, String[]> sortRequestParameters(ServletRequest request) {
        Map<String, String[]> sortedParams = new TreeMap<String, String[]>();
        Set<Map.Entry<String, String[]>> params = request.getParameterMap().entrySet();
        for (Map.Entry<String, String[]> entry : params) {
            sortedParams.put(entry.getKey(), entry.getValue());
        }
        return sortedParams;
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }

}
