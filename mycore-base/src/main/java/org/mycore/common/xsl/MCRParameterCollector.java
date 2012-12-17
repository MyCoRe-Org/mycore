/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.xsl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Transformer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Collects parameters used in XSL transformations, by copying them from
 * MCRConfiguration, from the HTTP and MyCoRe session, from request attributes etc. 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRParameterCollector {

    private final static Logger LOGGER = Logger.getLogger(MCRParameterCollector.class);

    /** The collected parameters */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * 
     * First, all configuration properties from MCRConfiguration are copied.
     * Second, those variables stored in the HTTP session, that start with "XSL." are copied.
     * Next, those variables stored in the MCRSession, that start with "XSL." are copied.
     * Next, those HTTP request parameters that start with "XSL." are copied.
     * Next, those HTTP request attributes that start with "XSL." are copied.
     * 
     * @param request the HttpRequest causing the XSL transformation, must NOT be null
     */
    public MCRParameterCollector(HttpServletRequest request) {
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

        setUnmodifyableParameters(mcrSession);
        debugSessionParameters();
    }

    /**
     * Collects parameters The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * 
     * First, all configuration properties from MCRConfiguration are copied.
     * Next, those variables stored in the MCRSession, that start with "XSL." are copied.
     * 
     * @param request the HttpRequest causing the XSL transformation, must NOT be null
     */
    public MCRParameterCollector() {
        setFromConfiguration();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        setFromSession(mcrSession);
        setUnmodifyableParameters(mcrSession);
        debugSessionParameters();
    }

    /**
     * Sets the parameter with the given name
     */
    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    /**
     * Sets the parameter only if it starts with "XSL." and is not empty
     */
    private void setXSLParameter(String name, String value) {
        if (name.startsWith("XSL.") && (value != null) && (!value.toString().isEmpty()))
            parameters.put(name.substring(4), value);
    }

    /**
     * Returns the parameter with the given name
     */
    public String getParameter(String name, String defaultValue) {
        String val = parameters.get(name);
        return (val == null) ? defaultValue : val;
    }

    /**
     * Copies all MCRConfiguration properties as XSL parameters.
     */
    private void setFromConfiguration() {
        for (Map.Entry<Object, Object> property : MCRConfiguration.instance().getProperties().entrySet()) {
            parameters.put(property.getKey().toString(), property.getValue().toString());
        }
    }

    /**
     * Sets those session variables as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromSession(HttpSession session) {
        for (@SuppressWarnings("unchecked")
        Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements();) {
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
            try {
                setXSLParameter(key, entry.getValue().toString());
            } catch (NullPointerException e) {
                LOGGER.warn("NPE " + key + ":" + entry.getValue(), e);
            }
        }
    }

    /**
     * Sets those request attributes as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromRequestAttributes(HttpServletRequest request) {
        for (@SuppressWarnings("unchecked")
        Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (!(name.endsWith(".SESSION")))
                setXSLParameter(name, request.getParameter(name));
        }
    }

    /**
     * Sets those request parameters as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromRequestParameters(HttpServletRequest request) {
        for (@SuppressWarnings("unchecked")
        Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements();) {
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
     */
    private void setUnmodifyableParameters(MCRSession session) {
        parameters.put("CurrentUser", session.getUserInformation().getUserID());
        parameters.put("CurrentLang", session.getCurrentLanguage());
        parameters.put("WebApplicationBaseURL", MCRServlet.getBaseURL());
        parameters.put("ServletsBaseURL", MCRServlet.getServletBaseURL());

        String defaultLang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", MCRConstants.DEFAULT_LANG);
        parameters.put("DefaultLang", defaultLang);
    }

    private void debugSessionParameters() {
        LOGGER.debug("XSL.HttpSession =" + parameters.get("HttpSession"));
        LOGGER.debug("XSL.JSessionID =" + parameters.get("JSessionID"));
        LOGGER.debug("XSL.CurrentUser =" + parameters.get("CurrentUser"));
        LOGGER.debug("XSL.Referer =" + parameters.get("Referer"));
    }

    /** Sets the request and referer URL */
    private void setFromRequestHeader(HttpServletRequest request) {
        parameters.put("RequestURL", getCompleteURL(request));
        parameters.put("Referer", request.getHeader("Referer") != null ? request.getHeader("Referer") : "");
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
        LOGGER.debug("Complete request URL : " + url);
        return url;
    }

    private StringBuilder getBaseURLUpToHostName() {
        StringBuilder buffer = new StringBuilder(MCRServlet.getBaseURL());
        int pos = buffer.indexOf("/", "https://".length());
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
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            transformer.setParameter(entry.getKey(), entry.getValue());
        }
    }
}
