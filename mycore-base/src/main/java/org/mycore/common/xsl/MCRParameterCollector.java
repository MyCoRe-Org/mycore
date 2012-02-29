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
import java.util.Map;
import java.util.Properties;

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
    private Properties parameters = new Properties();

    /**
     * Collects parameters. When the request given is not null, parameters are copied from 
     * request parameters and attributes, too. The collecting of parameters is done in steps,
     * each step may overwrite parameters that already have been set.
     * 
     * First, all configuration properties from MCRConfiguration are copied.
     * Second, those variables stored in the HTTP session, that start with "XSL." are copied.
     * Next, those variables stored in the MCRSession, that start with "XSL." are copied.
     * Next, those HTTP request parameters that start with "XSL." are copied.
     * Next, those HTTP request attributes that start with "XSL." are copied.
     * 
     * @param request the HttpRequest causing the XSL transformation, optionally null
     */
    public MCRParameterCollector(HttpServletRequest request) {
        setFromConfiguration();

        HttpSession session = request.getSession(false);
        if (session != null) {
            setFromSession(session);
        }

        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        setFromSession(mcrSession);

        if (request != null) {
            setFromRequestParameters(request);
            setFromRequestAttributes(request);
            setFromRequestHeader(request);
        }

        if (session != null) {
            setSessionID(session, request.isRequestedSessionIdFromCookie());
        }

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
     * Returns the parameter with the given name
     */
    public String getParameter(String name, String defaultValue) {
        return parameters.getProperty(name, defaultValue);
    }

    /**
     * Copies all MCRConfiguration properties as XSL parameters.
     */
    private void setFromConfiguration() {
        parameters.putAll(MCRConfiguration.instance().getProperties());
    }

    /**
     * Sets those session variables as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromSession(HttpSession session) {
        for (@SuppressWarnings("unchecked")
        Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.startsWith("XSL.")) {
                parameters.put(name.substring(4), session.getAttribute(name));
            }
        }
    }

    /**
     * Sets those session variables as XSL parameters that start with "XSL.",
     * others will be ignored. The "XSL." prefix is cut off from the name.
     */
    private void setFromSession(MCRSession session) {
        for (Map.Entry<Object, Object> entry : session.getMapEntries()) {
            String key = entry.getKey().toString();
            if (key.startsWith("XSL."))
                parameters.put(key.substring(4), entry.getValue().toString());
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
            if (name.startsWith("XSL.") && !name.endsWith(".SESSION")) {
                parameters.put(name.substring(4), request.getParameter(name));
            }
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
            if (name.startsWith("XSL.") && !name.endsWith(".SESSION")) {
                final Object attributeValue = request.getAttribute(name);
                if (attributeValue != null) {
                    parameters.put(name.substring(4), attributeValue.toString());
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
        LOGGER.debug("XSL.HttpSession =" + parameters.getProperty("HttpSession"));
        LOGGER.debug("XSL.JSessionID =" + parameters.getProperty("JSessionID"));
        LOGGER.debug("XSL.CurrentUser =" + parameters.getProperty("CurrentUser"));
        LOGGER.debug("XSL.Referer =" + parameters.getProperty("Referer"));
    }

    /** Sets the request and referer URL */
    private void setFromRequestHeader(HttpServletRequest request) {
        parameters.put("RequestURL", getCompleteURL(request));
        parameters.put("Referer", request.getHeader("Referer") != null ? request.getHeader("Referer") : "");
    }

    /** Calculates the complete request URL */
    private String getCompleteURL(HttpServletRequest request) {
        //when called by MCRErrorServlet
        String errorURI = (String) request.getAttribute("javax.servlet.error.request_uri");
        //assemble URL with baseUrl so that mod_proxy request are supported
        StringBuilder buffer = new StringBuilder(MCRServlet.getBaseURL());
        int pos = buffer.indexOf("/", "https://".length());
        buffer.delete(pos, buffer.length()); //get baseUrl up to hostname
        buffer.append(errorURI != null ? errorURI : request.getRequestURI());
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            buffer.append("?").append(queryString);
        }
        LOGGER.debug("Complete request URL : " + buffer.toString());
        return buffer.toString();
    }

    /**
     * Sets XSL parameters for the given transformer by taking them from the
     * properties object provided.
     * 
     * @param transformer
     *            the Transformer object thats parameters should be set
     */
    public void setParametersTo(Transformer transformer) {
        Enumeration<?> names = parameters.propertyNames();

        while (names.hasMoreElements()) {
            String name = names.nextElement().toString();
            String value = parameters.getProperty(name);

            transformer.setParameter(name, value);
        }
    }
}
