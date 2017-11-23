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

package org.mycore.common.xsl;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Transformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Collects parameters used in XSL transformations, by copying them from
 * MCRConfiguration, from the HTTP and MyCoRe session, from request attributes etc. 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRParameterCollector {

    private static final Logger LOGGER = LogManager.getLogger(MCRParameterCollector.class);

    /** The collected parameters */
    private Map<String, Object> parameters = new HashMap<>();

    /** If true (which is default), only those parameters starting with "XSL." are copied from session and request */
    private boolean onlySetXSLParameters = true;

    private boolean modified = true;

    private int hashCode;

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * 
     * First, all configuration properties from MCRConfiguration are copied.
     * Second, those variables stored in the HTTP session, that start with "XSL." are copied.
     * Next, variables stored in the MCRSession are copied.
     * Next, HTTP request parameters are copied.
     * 
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
     * 
     * First, all configuration properties from MCRConfiguration are copied.
     * Second, those variables stored in the HTTP session, that start with "XSL." are copied.
     * Next, variables stored in the MCRSession are copied.
     * Next, HTTP request parameters are copied.
     * Next, HTTP request attributes are copied.
     * 
     * @param request the HttpRequest causing the XSL transformation, must NOT be null
     * @param onlySetXSLParameters if true, only those parameters starting with "XSL." are copied from session and request
     */
    public MCRParameterCollector(HttpServletRequest request, boolean onlySetXSLParameters) {
        this.onlySetXSLParameters = onlySetXSLParameters;

        setFromConfiguration();

        HttpSession session = request.getSession(false);
        if (session != null) {
            setFromSession(session);
        }

        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        setFromSession(mcrSession);
        setFromRequestParameters(request);
        setFromRequestAttributes(request);
        setFromRequestHeader(request);

        if (session != null) {
            setSessionID(session, request.isRequestedSessionIdFromCookie());
        }

        setUnmodifyableParameters(mcrSession, request);
        debugSessionParameters();
    }

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * 
     * First, all configuration properties from MCRConfiguration are copied.
     * Next, those variables stored in the MCRSession that start with "XSL." are copied.
     */
    public MCRParameterCollector() {
        this(true);
    }

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * 
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
        if ((value == null) || value.isEmpty())
            return;
        if (name.startsWith("XSL."))
            parameters.put(name.substring(4), value);
        else if (!onlySetXSLParameters)
            parameters.put(name, value);
    }

    /**
     * Returns the parameter with the given name
     */
    public String getParameter(String name, String defaultValue) {
        Object val = parameters.get(name);
        return (val == null) ? defaultValue : val.toString();
    }

    /**
     * Returns the parameter map.  
     */
    public Map<String, Object> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Copies all MCRConfiguration properties as XSL parameters.
     */
    private void setFromConfiguration() {
        for (Map.Entry<String, String> property : MCRConfiguration.instance().getPropertiesMap().entrySet()) {
            parameters.put(property.getKey(), property.getValue());
        }
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
            if (!(name.endsWith(".SESSION")))
                setXSLParameter(name, request.getParameter(name));
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
     * Sets the ID of the current session as parameter
     */
    private void setSessionID(HttpSession session, boolean isFromCookie) {
        String sessionParam = MCRConfiguration.instance().getString("MCR.Session.Param", ";jsessionid=");
        String jSessionID = sessionParam + session.getId();
        parameters.put("JSessionID", jSessionID);
        if (!isFromCookie) {
            parameters.put("HttpSession", jSessionID);
        }
    }

    /**
     * Sets some parameters that must not be overwritten by the request, for example
     * the user ID and the URL of the web application.
     * 
     * @param session
     * @param request 
     */
    private void setUnmodifyableParameters(MCRSession session, HttpServletRequest request) {
        parameters.put("CurrentUser", session.getUserInformation().getUserID());
        parameters.put("CurrentLang", session.getCurrentLanguage());
        parameters.put("WebApplicationBaseURL", MCRFrontendUtil.getBaseURL());
        parameters.put("ServletsBaseURL", MCRServlet.getServletBaseURL());
        String defaultLang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang",
            MCRConstants.DEFAULT_LANG);
        parameters.put("DefaultLang", defaultLang);

        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        if (userAgent != null) {
            parameters.put("User-Agent", userAgent);
        }

    }

    private void debugSessionParameters() {
        LOGGER.debug("XSL.HttpSession ={}", parameters.get("HttpSession"));
        LOGGER.debug("XSL.JSessionID ={}", parameters.get("JSessionID"));
        LOGGER.debug("XSL.CurrentUser ={}", parameters.get("CurrentUser"));
        LOGGER.debug("XSL.Referer ={}", parameters.get("Referer"));
    }

    /** Sets the request and referer URL */
    private void setFromRequestHeader(HttpServletRequest request) {
        parameters.put("RequestURL", getCompleteURL(request));
        parameters.put("Referer", request.getHeader("Referer") != null ? request.getHeader("Referer") : "");
        parameters.put("UserAgent", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "");
    }

    /** 
     * Calculates the complete request URL, so that mod_proxy is supported 
     */
    private String getCompleteURL(HttpServletRequest request) {
        StringBuilder buffer = getBaseURLUpToHostName();

        //when called by MCRErrorServlet
        String errorURI = (String) request.getAttribute("javax.servlet.error.request_uri");
        buffer.append(errorURI != null ? errorURI : request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            buffer.append("?").append(queryString);
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
     *            the Transformer object thats parameters should be set
     */
    public void setParametersTo(Transformer transformer) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            transformer.setParameter(entry.getKey(), entry.getValue());
        }
    }

    public static MCRParameterCollector getInstanceFromUserSession() {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        MCRServletJob job = (MCRServletJob) mcrSession.get("MCRServletJob");
        return job == null ? new MCRParameterCollector() : new MCRParameterCollector(job.getRequest());
    }

    public int hashCode() {
        if (modified) {
            int result = LOGGER.hashCode();
            //order of map should not harm result
            result += parameters.entrySet().stream().mapToInt(Map.Entry::hashCode).sum();
            hashCode = result;
            modified = false;
        }
        return hashCode;
    }
}
