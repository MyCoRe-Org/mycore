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

package org.mycore.frontend;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Servlet/Jersey Resource utility class.
 */
public class MCRFrontendUtil {

    private static final String PROXY_HEADER_HOST = "X-Forwarded-Host";

    private static final String PROXY_HEADER_SCHEME = "X-Forwarded-Proto";

    private static final String PROXY_HEADER_PORT = "X-Forwarded-Port";

    private static final String PROXY_HEADER_PATH = "X-Forwarded-Path";

    private static final String PROXY_HEADER_REMOTE_IP = "X-Forwarded-For";

    public static final String BASE_URL_ATTRIBUTE = "org.mycore.base.url";

    private static String BASE_URL;

    private static String BASE_HOST_IP;

    private static Logger LOGGER = LogManager.getLogger();

    static {
        prepareBaseURLs(""); // getBaseURL() etc. may be called before any HTTP Request    
    }

    /** The IP addresses of trusted web proxies */
    protected static final Set<String> TRUSTED_PROXIES = getTrustedProxies();

    /** returns the base URL of the mycore system */
    public static String getBaseURL() {
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession session = MCRSessionMgr.getCurrentSession();
            Object value = session.get(BASE_URL_ATTRIBUTE);
            if (value != null) {
                LOGGER.debug("Returning BaseURL {} from user session.", value);
                return value.toString();
            }
        }
        return BASE_URL;
    }

    public static String getHostIP() {
        return BASE_HOST_IP;
    }

    /**
     * returns the base URL of the mycore system. This method uses the request to 'calculate' the right baseURL.
     * Generally it is sufficent to use {@link #getBaseURL()} instead.
     */
    public static String getBaseURL(ServletRequest req) {
        HttpServletRequest request = (HttpServletRequest) req;
        String scheme = req.getScheme();
        String host = req.getServerName();
        int serverPort = req.getServerPort();
        String path = request.getContextPath() + "/";

        if (TRUSTED_PROXIES.contains(req.getRemoteAddr())) {
            scheme = Optional.ofNullable(request.getHeader(PROXY_HEADER_SCHEME)).orElse(scheme);
            host = Optional.ofNullable(request.getHeader(PROXY_HEADER_HOST)).orElse(host);
            serverPort = Optional.ofNullable(request.getHeader(PROXY_HEADER_PORT))
                .map(Integer::parseInt)
                .orElse(serverPort);
            path = Optional.ofNullable(request.getHeader(PROXY_HEADER_PATH)).orElse(path);
            if (!path.endsWith("/")) {
                path += "/";
            }
        }
        StringBuilder webappBase = new StringBuilder(scheme);
        webappBase.append("://");
        webappBase.append(host);
        if (!("http".equals(scheme) && serverPort == 80 || "https".equals(scheme) && serverPort == 443)) {
            webappBase.append(':').append(serverPort);
        }
        webappBase.append(path);
        return webappBase.toString();
    }

    public static synchronized void prepareBaseURLs(String baseURL) {
        BASE_URL = MCRConfiguration.instance().getString("MCR.baseurl", baseURL);
        if (!BASE_URL.endsWith("/")) {
            BASE_URL = BASE_URL + "/";
        }
        try {
            URL url = new URL(BASE_URL);
            InetAddress BASE_HOST = InetAddress.getByName(url.getHost());
            BASE_HOST_IP = BASE_HOST.getHostAddress();
        } catch (MalformedURLException e) {
            LOGGER.error("Can't create URL from String {}", BASE_URL);
        } catch (UnknownHostException e) {
            LOGGER.error("Can't find host IP for URL {}", BASE_URL);
        }
    }

    public static void configureSession(MCRSession session, HttpServletRequest request, HttpServletResponse response) {
        session.setServletJob(new MCRServletJob(request, response));
        // language
        getProperty(request, "lang").ifPresent(session::setCurrentLanguage);

        // Set the IP of the current session
        if (session.getCurrentIP().length() == 0) {
            session.setCurrentIP(getRemoteAddr(request));
        }

        // set BASE_URL_ATTRIBUTE to MCRSession
        if (request.getAttribute(BASE_URL_ATTRIBUTE) != null) {
            session.put(BASE_URL_ATTRIBUTE, request.getAttribute(BASE_URL_ATTRIBUTE));
        }

        // Store XSL.*.SESSION parameters to MCRSession
        putParamsToSession(request);
    }

    /**
     * @param request current request to get property from
     * @param name of request {@link HttpServletRequest#getAttribute(String) attribute} or {@link HttpServletRequest#getParameter(String) parameter}
     * @return an Optional that is either empty or contains a trimmed non-empty String that is either
     *  the value of the request attribute or a parameter (in that order) with the given <code>name</code>.
     */
    public static Optional<String> getProperty(HttpServletRequest request, String name) {
        return Stream.<Supplier<Object>> of(
            () -> request.getAttribute(name),
            () -> request.getParameter(name))
            .map(Supplier::get)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .findFirst();
    }

    /**
     * Returns the IP address of the client that made the request. When a trusted proxy server was used, e. g. a local
     * Apache mod_proxy in front of Tomcat, the value of the last entry in the HTTP header X_FORWARDED_FOR is returned,
     * otherwise the REMOTE_ADDR is returned. The list of trusted proxy IPs can be configured using the property
     * MCR.Request.TrustedProxies, which is a List of IP addresses separated by blanks and/or comma.
     */
    public static String getRemoteAddr(HttpServletRequest req) {
        String remoteAddress = req.getRemoteAddr();
        if (TRUSTED_PROXIES.contains(remoteAddress)) {
            String xff = getXForwardedFor(req);
            if (xff != null)
                remoteAddress = xff;
        }
        return remoteAddress;
    }

    /**
     * Get header to check if request comes in via a proxy. There are two possible header names
     */
    private static String getXForwardedFor(HttpServletRequest req) {
        String xff = req.getHeader(PROXY_HEADER_REMOTE_IP);
        if ((xff == null) || xff.trim().isEmpty()) {
            xff = req.getHeader(PROXY_HEADER_REMOTE_IP);
        }
        if ((xff == null) || xff.trim().isEmpty())
            return null;

        // X_FORWARDED_FOR can be comma separated list of hosts,
        // if so, take last entry, all others are not reliable because
        // any client may have set the header to any value.

        LOGGER.debug("{} complete: {}", PROXY_HEADER_REMOTE_IP, xff);
        StringTokenizer st = new StringTokenizer(xff, " ,;");
        while (st.hasMoreTokens()) {
            xff = st.nextToken().trim();
        }
        LOGGER.debug("{} last: {}", PROXY_HEADER_REMOTE_IP, xff);
        return xff;
    }

    private static void putParamsToSession(HttpServletRequest request) {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.startsWith("XSL.") && name.endsWith(".SESSION")) {
                String key = name.substring(0, name.length() - 8);
                // parameter is not empty -> store
                if (!request.getParameter(name).trim().equals("")) {
                    mcrSession.put(key, request.getParameter(name));
                    LOGGER.debug("Found HTTP-Req.-Parameter {}={} that should be saved in session, safed {}={}", name,
                        request.getParameter(name), key, request.getParameter(name));
                }
                // paramter is empty -> do not store and if contained in
                // session, remove from it
                else {
                    if (mcrSession.get(key) != null) {
                        mcrSession.deleteObject(key);
                    }
                }
            }
        }
        for (Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.startsWith("XSL.") && name.endsWith(".SESSION")) {
                String key = name.substring(0, name.length() - 8);
                // attribute is not empty -> store
                if (!request.getAttribute(name).toString().trim().equals("")) {
                    mcrSession.put(key, request.getAttribute(name));
                    LOGGER.debug("Found HTTP-Req.-Attribute {}={} that should be saved in session, safed {}={}", name,
                        request.getParameter(name), key, request.getParameter(name));
                }
                // attribute is empty -> do not store and if contained in
                // session, remove from it
                else {
                    if (mcrSession.get(key) != null) {
                        mcrSession.deleteObject(key);
                    }
                }
            }
        }
    }

    /**
     * Builds a list of trusted proxy IPs from MCR.Request.TrustedProxies. The IP address of the local host is
     * automatically added to this list.
     * 
     * @return
     */
    private static TreeSet<String> getTrustedProxies() {
        // Always trust the local host
        return Stream
            .concat(Stream.of("localhost", URI.create(getBaseURL()).getHost()), MCRConfiguration2
                .getString("MCR.Request.TrustedProxies").map(MCRConfiguration2::splitValue).orElse(Stream.empty()))
            .distinct()
            .peek(proxy -> LOGGER.debug("Trusted proxy: {}", proxy))
            .map(host -> {
                try {
                    return InetAddress.getAllByName(host);
                } catch (UnknownHostException e) {
                    LOGGER.warn("Unknown host: {}", host);
                    return null;
                }
            }).filter(Objects::nonNull)
            .flatMap(Stream::of)
            .map(InetAddress::getHostAddress)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Sets cache-control, last-modified and expires parameter to the response header.
     * Use this method when the client should cache the response data.
     * 
     * @param response the response data to cache
     * @param CACHE_TIME how long to cache
     * @param lastModified when the data was last modified
     * @param useExpire true if 'Expire' header should be set
     */
    public static void writeCacheHeaders(HttpServletResponse response, long CACHE_TIME, long lastModified,
        boolean useExpire) {
        response.setHeader("Cache-Control", "public, max-age=" + CACHE_TIME);
        response.setDateHeader("Last-Modified", lastModified);
        if (useExpire) {
            Date expires = new Date(System.currentTimeMillis() + CACHE_TIME * 1000);
            LOGGER.info("Last-Modified: {}, expire on: {}", new Date(lastModified), expires);
            response.setDateHeader("Expires", expires.getTime());
        }
    }

    public static Optional<File> getWebAppBaseDir(ServletContext ctx) {
        return Optional.ofNullable(ctx.getRealPath("/")).map(File::new);
    }

}
