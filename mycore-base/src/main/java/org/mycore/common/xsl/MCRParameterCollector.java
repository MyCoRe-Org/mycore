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

package org.mycore.common.xsl;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.transform.Transformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Verifier;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Collects parameters used in XSL transformations, by copying them from
 * MCRConfiguration, from the HTTP and MyCoRe session, from request attributes etc.
 *
 * @author Frank Lützenkirchen
 */
public class MCRParameterCollector {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String HEADER_USER_AGENT = "User-Agent";

    private static final String HEADER_REFERER = "Referer";

    /** The collected parameters */
    private final Map<String, Object> parameters = new HashMap<>();

    /** If true (which is default), only those parameters starting with "XSL." are copied from session and request */
    private boolean onlySetXSLParameters = true;

    private boolean modified = true;

    private int hashCode;

    private boolean setPropertiesFromConfiguration;

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * <p>
     * First, all configuration properties from MCRConfiguration are copied.
     * Second, those variables stored in the HTTP session, that start with "XSL." are copied.
     * Next, variables stored in the MCRSession are copied.
     * Next, HTTP request parameters are copied.
     * <p>
     * Only those parameters starting with "XSL." are copied from session and request,
     *
     * @param request the HttpRequest causing the XSL transformation, must NOT be null
     */
    public MCRParameterCollector(HttpServletRequest request) {
        this(request, true);
    }

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * <p>
     * First, all configuration properties from MCRConfiguration are copied.
     * Second, those variables stored in the HTTP session, that start with "XSL." are copied.
     * Next, variables stored in the MCRSession are copied.
     * Next, HTTP request parameters are copied.
     * Next, HTTP request attributes are copied.
     *
     * @param request the HttpRequest causing the XSL transformation, must NOT be null
     * @param onlySetXSLParameters if true, only those parameters starting with "XSL."
     *                            are copied from session and request
     */
    public MCRParameterCollector(HttpServletRequest request, boolean onlySetXSLParameters) {
        this.onlySetXSLParameters = onlySetXSLParameters;

        setFromConfiguration();

        HttpSession session = request.getSession(false);
        if (session != null) {
            setFromSession(session);
        }

        if (!MCRSessionMgr.isLocked()) {
            MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
            setFromSession(mcrSession);
            setUnmodifyableParameters(mcrSession, request);
        }
        setFromRequestParameters(request);
        setFromRequestAttributes(request);
        setFromRequestHeader(request);

        debugSessionParameters();
    }

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * <p>
     * First, all configuration properties from MCRConfiguration are copied.
     * Next, those variables stored in the MCRSession that start with "XSL." are copied.
     */
    public MCRParameterCollector() {
        this(true);
    }

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * <p>
     * First, all configuration properties from MCRConfiguration are copied.
     * Next, those variables stored in the MCRSession are copied.
     *
     * @param onlySetXSLParameters if true, only those parameters starting with "XSL." are copied from session
     */
    public MCRParameterCollector(boolean onlySetXSLParameters) {
        this.onlySetXSLParameters = onlySetXSLParameters;
        setFromConfiguration();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        setFromSession(mcrSession);
        setUnmodifyableParameters(mcrSession, null);
        debugSessionParameters();
    }

    /**
     * Sets the parameter with the given name
     */
    public void setParameter(String name, Object value) {
        parameters.put(name, value);
        modified = true;
    }

    /**
     * Sets all parameters from the given map
     */
    public void setParameters(Map<String, String> param) {
        parameters.putAll(param);
        modified = true;
    }

    /**
     * Sets the parameter only if it is not empty and starts with "XSL." or onlySetXSLParameters is false
     */
    private void setXSLParameter(String name, String value) {
        if ((value == null) || value.isEmpty()) {
            return;
        }
        if (name.startsWith("XSL.")) {
            parameters.put(name.substring(4), value);
        } else if (!onlySetXSLParameters) {
            parameters.put(name, value);
        }
    }

    /**
     * Returns the parameter with the given name
     */
    public String getParameter(String name, String defaultValue) {
        return Optional.ofNullable(parameters.get(name))
            .map(Object::toString)
            .or(() -> Optional.ofNullable(SavePropertiesCacheHolder.getSafePropertiesCache().get(name)))
            .orElse(defaultValue);
    }

    /**
     * Returns the parameter map.
     */
    public Map<String, Object> getParameterMap() {
        Map<String, Object> mergedMap = new HashMap<>(SavePropertiesCacheHolder.getSafePropertiesCache());
        mergedMap.putAll(parameters);
        return Collections.unmodifiableMap(mergedMap);
    }

    /**
     * Sets a marker so that all MCRConfiguration properties will be copied to XSL parameters, when
     * {@link #setParametersTo(Transformer)} is called.
     * Characters that are valid in property names but invalid in XML names are replaced with an underscore.
     * Colons are replaced with underscores as well, because this character is used as a namespace separater in
     * namespace-aware XML.
     */
    private void setFromConfiguration() {
        setPropertiesFromConfiguration = true;
    }

    /**
     * Sets those session variables as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromSession(HttpSession session) {
        for (Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            setXSLParameter(name, session.getAttribute(name).toString());
        }
    }

    /**
     * Sets those session variables as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromSession(MCRSession session) {
        Objects.requireNonNull(session, "Session needs to be not null!");
        for (Map.Entry<Object, Object> entry : session.getMapEntries()) {
            String key = entry.getKey().toString();
            if (entry.getValue() != null) {
                setXSLParameter(key, entry.getValue().toString());
            }
        }
    }

    /**
     * Sets those request attributes as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromRequestParameters(HttpServletRequest request) {
        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (!(name.endsWith(".SESSION"))) {
                setXSLParameter(name, request.getParameter(name));
            }
        }
    }

    /**
     * Sets those request parameters as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromRequestAttributes(HttpServletRequest request) {
        for (Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (!(name.endsWith(".SESSION"))) {
                final Object attributeValue = request.getAttribute(name);
                if (attributeValue != null) {
                    setXSLParameter(name, attributeValue.toString());
                }
            }
        }
    }

    /**
     * Sets some parameters that must not be overwritten by the request, for example
     * the user ID and the URL of the web application.
     *
     */
    private void setUnmodifyableParameters(MCRSession session, HttpServletRequest request) {
        parameters.put("CurrentUser", session.getUserInformation().getUserID());
        parameters.put("CurrentLang", session.getCurrentLanguage());
        parameters.put("WebApplicationBaseURL", MCRFrontendUtil.getBaseURL());
        parameters.put("ServletsBaseURL", MCRServlet.getServletBaseURL());
        String defaultLang = MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse(MCRConstants.DEFAULT_LANG);
        parameters.put("DefaultLang", defaultLang);

        String userAgent = request != null ? request.getHeader(HEADER_USER_AGENT) : null;
        if (userAgent != null) {
            parameters.put("UserAgent", userAgent);
        }
    }

    private void debugSessionParameters() {
        LOGGER.debug("XSL.CurrentUser ={}", () -> parameters.get("CurrentUser"));
        LOGGER.debug("XSL.Referer ={}", () -> parameters.get(HEADER_REFERER));
    }

    /** Sets the request and referer URL */
    private void setFromRequestHeader(HttpServletRequest request) {
        parameters.put("RequestURL", getCompleteURL(request));
        String referer = request.getHeader(HEADER_REFERER);
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        parameters.put("Referer", referer != null ? referer : "");
        parameters.put("UserAgent", userAgent != null ? userAgent : "");
    }

    /**
     * Calculates the complete request URL, so that mod_proxy is supported
     */
    private String getCompleteURL(HttpServletRequest request) {
        StringBuilder buffer = getBaseURLUpToHostName();

        //when called by MCRErrorServlet
        String errorURI = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        buffer.append(errorURI != null ? errorURI : request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            buffer.append('?').append(queryString);
        }

        String url = buffer.toString();
        LOGGER.debug("Complete request URL : {}", url);
        return url;
    }

    private StringBuilder getBaseURLUpToHostName() {
        int schemeLength = "https://".length();
        String baseURL = MCRFrontendUtil.getBaseURL();
        StringBuilder buffer = new StringBuilder(baseURL);
        if (baseURL.length() < schemeLength) {
            return buffer;
        }
        int pos = buffer.indexOf("/", schemeLength);
        buffer.delete(pos, buffer.length());
        return buffer;
    }

    /**
     * Sets XSL parameters for the given transformer by taking them from the
     * properties object provided.
     *
     * @param transformer
     *            the Transformer object that parameters should be set
     */
    public void setParametersTo(Transformer transformer) {
        if (setPropertiesFromConfiguration) {
            SavePropertiesCacheHolder.getSafePropertiesCache().forEach(transformer::setParameter);
        }
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            transformer.setParameter(entry.getKey(), entry.getValue());
        }
    }

    public static MCRParameterCollector ofCurrentSession() {
        return ofSession(MCRSessionMgr.getCurrentSession());
    }

    public static MCRParameterCollector ofSession(MCRSession session) {
        MCRServletJob job = (MCRServletJob) session.get("MCRServletJob");
        return job == null ? new MCRParameterCollector() : new MCRParameterCollector(job.getRequest());
    }

    /**
     * This method is used to clear the cache of the properties that are used in the XSL transformations.
     * This should only be used in tests.
     */
    public static void clearCache() {
        SavePropertiesCacheHolder.clear();
    }

    @Override
    public int hashCode() {
        if (modified) {
            int result = 1;
            //order of map should not harm result
            result = 31 * result + SavePropertiesCacheHolder.getSafePropertiesHashCode();
            result = 31 * result + parameters.entrySet().stream().mapToInt(Map.Entry::hashCode).sum();
            hashCode = result;
            modified = false;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MCRParameterCollector other = (MCRParameterCollector) obj;
        return hashCode() == other.hashCode()
            && onlySetXSLParameters == other.onlySetXSLParameters
            && setPropertiesFromConfiguration == other.setPropertiesFromConfiguration
            && Objects.equals(parameters, other.parameters);

    }

    private static final class SavePropertiesCacheHolder {
        private final static AtomicInteger COMPUTED_HASH_CODE = new AtomicInteger(0);
        private final static AtomicReference<UUID> PROPERTIES_CHANGE_LISTENER_ID = new AtomicReference<>();
        private static volatile Map<String, String> safePropertiesCache;

        static synchronized void clear() {
            safePropertiesCache = null;
            COMPUTED_HASH_CODE.set(0);
            UUID listenerID = PROPERTIES_CHANGE_LISTENER_ID.get();
            if (listenerID != null) {
                MCRConfiguration2.removePropertyChangeEventListener(listenerID);
            }
        }

        static Map<String, String> getSafePropertiesCache() {
            if (safePropertiesCache == null) {
                synchronized (SavePropertiesCacheHolder.class) {
                    if (safePropertiesCache == null) {
                        safePropertiesCache = initializeSafeProperties();
                    }
                }
            }
            return safePropertiesCache;
        }

        static Map<String, String> initializeSafeProperties() {
            Map<String, String> safeProperties = new ConcurrentHashMap<>();

            UUID uuid = MCRConfiguration2.addPropertyChangeEventLister((k) -> true, (k, old, current) -> {
                if (current.isEmpty() && old.isPresent()) {
                    safeProperties.remove(xmlSafe(k));
                    COMPUTED_HASH_CODE.set(computeHashCode(safeProperties));
                } else if (current.isPresent()) {
                    safeProperties.put(xmlSafe(k), current.get());
                    COMPUTED_HASH_CODE.set(computeHashCode(safeProperties));
                }
            });

            PROPERTIES_CHANGE_LISTENER_ID.set(uuid);

            MCRConfiguration2.getPropertiesMap().forEach((key, value) -> {
                safeProperties.put(xmlSafe(key), value);
            });

            COMPUTED_HASH_CODE.set(computeHashCode(safeProperties));

            return safeProperties;
        }

        private static int computeHashCode(Map<String, String> map) {
            return map.entrySet().stream().mapToInt(Map.Entry::hashCode).sum();
        }

        private static String xmlSafe(String key) {
            StringBuilder builder = new StringBuilder();
            if (!key.isEmpty()) {
                char first = key.charAt(0);
                builder.append(first == ':' || !Verifier.isXMLNameStartCharacter(first) ? "_" : first);
                for (int i = 1; i < key.length(); i++) {
                    char following = key.charAt(i);
                    builder.append(following == ':' || !Verifier.isXMLNameCharacter(following) ? "_" : following);
                }
            }
            return builder.toString();
        }

        public static int getSafePropertiesHashCode() {
            return COMPUTED_HASH_CODE.get();
        }
    }
}
