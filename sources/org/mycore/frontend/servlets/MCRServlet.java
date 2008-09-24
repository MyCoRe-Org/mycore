/*
 * 
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

package org.mycore.frontend.servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRActiveLinkException;

/**
 * This is the superclass of all MyCoRe servlets. It provides helper methods for
 * logging and managing the current session data. Part of the code has been
 * taken from MilessServlet.java written by Frank Lützenkirchen.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date: 2008-02-06 17:27:24 +0000 (Mi, 06 Feb
 *          2008) $
 */
public class MCRServlet extends HttpServlet {
    private static final String INITIAL_SERVLET_NAME_KEY = "currentServletName";

    private static final long serialVersionUID = 1L;

    // Some configuration details
    protected static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    protected static final MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    private static Logger LOGGER = Logger.getLogger(MCRServlet.class);;

    private static String BASE_URL;

    private static String SERVLET_URL;

    private static final boolean ENABLE_BROWSER_CACHE = CONFIG.getBoolean("MCR.Servlet.BrowserCache.enable", false);

    // These values serve to remember if we have a GET or POST request
    private final static boolean GET = true;

    private final static boolean POST = false;

    private static MCRLayoutService LAYOUT_SERVICE;

    public static final String BASE_URL_ATTRIBUTE = "org.mycore.base.url";

    public static MCRLayoutService getLayoutService() {
        return LAYOUT_SERVICE;
    }

    public void init() throws ServletException {
        super.init();
        if (LAYOUT_SERVICE == null) {
        	LAYOUT_SERVICE = MCRLayoutService.instance();
        }
    }

    /** returns the base URL of the mycore system */
    public static String getBaseURL() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object value = session.get(BASE_URL_ATTRIBUTE);
        if (value != null) {
            LOGGER.debug("Returning BaseURL from user session.");
            return value.toString();
        }
        return BASE_URL;
    }

    /** returns the servlet base URL of the mycore system */
    public static String getServletBaseURL() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object value = session.get(BASE_URL_ATTRIBUTE);
        if (value != null) {
            return value.toString() + "servlets/";
        }
        return SERVLET_URL;
    }

    /**
     * Initialisation of the static values for the base URL and servlet URL of
     * the mycore system.
     */
    private static synchronized void prepareURLs(ServletContext context, HttpServletRequest req) {
        String contextPath = req.getContextPath() + "/";

        String requestURL = req.getRequestURL().toString();
        int pos = requestURL.indexOf(contextPath, 9);

        BASE_URL = CONFIG.getString("MCR.baseurl", requestURL.substring(0, pos) + contextPath);
        SERVLET_URL = BASE_URL + "servlets/";
        MCRURIResolver.init(context, getBaseURL());
    }

    // The methods doGet() and doPost() simply call the private method
    // doGetPost(),
    // i.e. GET- and POST requests are handled by one method only.
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGetPost(req, res, GET);
    }

    protected void doGet(MCRServletJob job) throws Exception {
        doGetPost(job);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGetPost(req, res, POST);
    }

    protected void doPost(MCRServletJob job) throws Exception {
        doGetPost(job);
    }

    public static MCRSession getSession(HttpServletRequest req, String servletName) {
        HttpSession theSession = req.getSession(true);
        MCRSession session = null;

        MCRSession fromHttpSession = (MCRSession) theSession.getAttribute("mycore.session");

        if (fromHttpSession != null) {
            // Take session from HttpSession with servlets
            session = fromHttpSession;
        } else {
            // Create a new session
            session = MCRSessionMgr.getCurrentSession();
        }

        // Store current session in HttpSession
        theSession.setAttribute("mycore.session", session);
        // store the HttpSession ID in MCRSession
        session.put("http.session", theSession.getId());

        String currentThread = getProperty(req, "currentThreadName");
        // check if this is request passed the same thread before
        // (RequestDispatcher)
        if (currentThread == null || !currentThread.equals(Thread.currentThread().getName())) {
            // Bind current session to this thread:
            MCRSessionMgr.setCurrentSession(session);
            req.setAttribute("currentThreadName", Thread.currentThread().getName());
            req.setAttribute(INITIAL_SERVLET_NAME_KEY, servletName);
        }

        // Forward MCRSessionID to XSL Stylesheets
        req.setAttribute("XSL.MCRSessionID", session.getID());

        return session;
    }

    /**
     * This private method handles both GET and POST requests and is invoked by
     * doGet() and doPost().
     * 
     * @param req
     *            the HTTP request instance
     * @param res
     *            the HTTP response instance
     * @param GETorPOST
     *            boolean value to remember if we have a GET or POST request
     * 
     * @exception IOException
     *                for java I/O errors.
     * @exception ServletException
     *                for errors from the servlet engine.
     */
    private void doGetPost(HttpServletRequest req, HttpServletResponse res, boolean GETorPOST) throws ServletException, IOException {
        if (CONFIG == null) {
            // removes NullPointerException below, if somehow Servlet is not yet
            // intialized
            init();
        }

        // Try to set encoding of form values
        String ReqCharEncoding = req.getCharacterEncoding();

        if (ReqCharEncoding == null) {
            // Set default to UTF-8
            ReqCharEncoding = CONFIG.getString("MCR.Request.CharEncoding", "UTF-8");
            req.setCharacterEncoding(ReqCharEncoding);
            LOGGER.debug("Setting ReqCharEncoding to: " + ReqCharEncoding);
        }

        if ("true".equals(req.getParameter("reload.properties"))) {
            MCRConfiguration.instance().reload(true);
        }

        if (BASE_URL == null) {
            prepareURLs(getServletContext(), req);
        }

        MCRServletJob job = new MCRServletJob(req, res);

        try {
            MCRSession session = getSession(req, getServletName());
            session.put("MCRServletJob", job);

            String c = getClass().getName();
            c = c.substring(c.lastIndexOf(".") + 1);

            StringBuffer msg = new StringBuffer();
            msg.append(c);
            msg.append(" ip=");
            msg.append(getRemoteAddr(req));

            /*
             * msg.append(theSession.isNew() ? " new" : " old"); msg.append("
             * http=").append(theSession.getId());
             */
            msg.append(" mcr=").append(session.getID());
            msg.append(" user=").append(session.getCurrentUserID());
            LOGGER.info(msg.toString());

            String lang = getProperty(req, "lang");

            if ((lang != null) && (lang.trim().length() != 0)) {
                session.setCurrentLanguage(lang.trim());
            }

            // Set the IP of the current session
            if (session.getCurrentIP().length() == 0) {
                session.setCurrentIP(getRemoteAddr(req));
            }

            // set BASE_URL_ATTRIBUTE to MCRSession
            if (req.getAttribute(BASE_URL_ATTRIBUTE) != null) {
                session.put(BASE_URL_ATTRIBUTE, req.getAttribute(BASE_URL_ATTRIBUTE));
            }

            // Store XSL.*.SESSION parameters to MCRSession
            putParamsToSession(req);
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                job.beginTransaction();
            }
            if (GETorPOST == GET) {
                doGet(job);
            } else {
                doPost(job);
            }
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                try {
                    job.commitTransaction();
                } catch (RuntimeException e) {
                    MCRHIBConnection.instance().getSession().close();
                    job.beginTransaction();
                    generateErrorPage(req, res, 500, e.getMessage(), e, false);
                    job.commitTransaction();
                }
            }
        } catch (Exception ex) {
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                job.rollbackTransaction();
            }
            if (ex instanceof ServletException) {
                throw (ServletException) ex;
            } else if (ex instanceof IOException) {
                throw (IOException) ex;
            } else {
                handleException(ex);
                job.beginTransaction();
                generateErrorPage(req, res, 500, ex.getMessage(), ex, false);
                job.commitTransaction();
            }
        } finally {
        	MCRSessionMgr.getCurrentSession().deleteObject("MCRServletJob");
        	job.beginTransaction();
        	MCRHIBConnection.instance().getSession().clear();
        	job.commitTransaction();
            // Release current MCRSession from current Thread,
            // in case that Thread pooling will be used by servlet engine
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                MCRSessionMgr.releaseCurrentSession();
            }
        }
    }

    /**
     * This method should be overwritten by other servlets. As a default
     * response we indicate the HTTP 1.1 status code 501 (Not Implemented).
     */
    protected void doGetPost(MCRServletJob job) throws Exception {
        job.getResponse().sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    /** Handles an exception by reporting it and its embedded exception */
    protected void handleException(Exception ex) {
        try {
            reportException(ex);

            if (ex instanceof MCRException) {
                ex = ((MCRException) ex).getException();

                if (ex != null) {
                    handleException(ex);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** Reports an exception to the log */
    protected void reportException(Exception ex) throws Exception {
        String msg = ((ex.getMessage() == null) ? "" : ex.getMessage());
        String type = ex.getClass().getName();
        String cname = this.getClass().getName();
        String servlet = cname.substring(cname.lastIndexOf(".") + 1);
        String trace = MCRException.getStackTraceAsString(ex);

        LOGGER.warn("Exception caught in : " + servlet);
        LOGGER.warn("Exception type      : " + type);
        LOGGER.warn("Exception message   : " + msg);
        LOGGER.debug(trace);
    }

    protected void generateErrorPage(HttpServletRequest request, HttpServletResponse response, int error, String msg, Exception ex, boolean xmlstyle)
            throws IOException {
        LOGGER.error(getClass().getName() + ": Error " + error + " occured. The following message was given: " + msg, ex);

        String rootname = "mcr_error";
        String style = getProperty(request, "XSL.Style");
        if ((style == null) || !(style.equals("xml"))) {
            style = "default";
        }
        Element root = new Element(rootname);
        root.setAttribute("HttpError", Integer.toString(error)).setText(msg);

        Document errorDoc = new Document(root, new DocType(rootname));

        while (ex != null) {
            Element exception = new Element("exception");
            exception.setAttribute("type", ex.getClass().getName());
            Element trace = new Element("trace");
            Element message = new Element("message");
            trace.setText(MCRException.getStackTraceAsString(ex));
            message.setText(ex.getMessage());
            exception.addContent(message).addContent(trace);
            root.addContent(exception);

            if (ex instanceof MCRException) {
                ex = ((MCRException) ex).getException();
            } else {
                ex = null;
            }
        }

        request.setAttribute("XSL.Style", style);

        final String requestAttr = "MCRServlet.generateErrorPage";
        if ((!response.isCommitted()) && (request.getAttribute(requestAttr) == null)) {
            response.setStatus(error);
            request.setAttribute(requestAttr, msg);
            LAYOUT_SERVICE.doLayout(request, response, errorDoc);
            return;
        } else {
            if (request.getAttribute(requestAttr) != null) {
                LOGGER.warn("Could not send error page. Generating error page failed. The original message:\n" + request.getAttribute(requestAttr));
            } else {
                LOGGER.warn("Could not send error page. Response allready commited. The following message was given:\n" + msg);
            }
        }
    }

    /**
     * This method builds a URL that can be used to redirect the client browser
     * to another page, thereby including http request parameters. The request
     * parameters will be encoded as http get request.
     * 
     * @param baseURL
     *            the base url of the target webpage
     * @param parameters
     *            the http request parameters
     */
    protected String buildRedirectURL(String baseURL, Properties parameters) {
        StringBuilder redirectURL = new StringBuilder(baseURL);
        boolean first = true;
        for (Enumeration e = parameters.keys(); e.hasMoreElements();) {
            if (first) {
                redirectURL.append("?");
                first = false;
            } else
                redirectURL.append("&");

            String name = (String) (e.nextElement());
            String value = null;
            try {
                value = URLEncoder.encode(parameters.getProperty(name), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                value = parameters.getProperty(name);
            }
            redirectURL.append(name).append("=").append(value);
        }
        LOGGER.debug("Sending redirect to " + redirectURL.toString());
        return redirectURL.toString();
    }

    protected void generateActiveLinkErrorpage(HttpServletRequest request, HttpServletResponse response, String msg, MCRActiveLinkException activeLinks)
            throws IOException {
        StringBuffer msgBuf = new StringBuffer(msg);
        msgBuf.append("\nThere are links active preventing the commit of work, see error message for details. The following links where affected:");
        Map<String, Collection<String>> links = activeLinks.getActiveLinks();
        Iterator<Map.Entry<String, Collection<String>>> entryIt = links.entrySet().iterator();
        while (entryIt.hasNext()) {
            Map.Entry<String, Collection<String>> entry = entryIt.next();
            for (String source : entry.getValue()) {
                msgBuf.append('\n').append(source).append("==>").append(entry.getKey());
            }
        }
        generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msgBuf.toString(), activeLinks, false);
    }

    /**
     * allows browser to cache requests.
     * 
     * This method is usefull as it allows browsers to cache content that is not
     * changed.
     * 
     * Please overwrite this method in every Servlet that depends on "remote"
     * data.
     * 
     */
    protected long getLastModified(HttpServletRequest request) {
        if (ENABLE_BROWSER_CACHE) {
            // we can cache every (local) request
            long lastModified = (MCRSessionMgr.getCurrentSession().getLoginTime() > CONFIG.getSystemLastModified()) ? MCRSessionMgr.getCurrentSession()
                    .getLoginTime() : CONFIG.getSystemLastModified();
            LOGGER.info("LastModified: " + lastModified);
            return lastModified;
        }
        return -1; // time is not known
    }

    public static String getProperty(HttpServletRequest request, String name) {
        String value = (String) request.getAttribute(name);

        // if Attribute not given try Parameter
        if ((value == null) || (value.length() == 0)) {
            value = request.getParameter(name);
        }

        return value;
    }

    /** The IP addresses of trusted web proxies */
    protected static Set<String> trustedProxies = new HashSet<String>();

    /**
     * Builds a list of trusted proxy IPs from MCR.Request.TrustedProxies. The
     * IP address of the local host is automatically added to this list.
     */
    protected static synchronized void initTrustedProxies() {
        String sTrustedProxies = MCRConfiguration.instance().getString("MCR.Request.TrustedProxies", "");
        StringTokenizer st = new StringTokenizer(sTrustedProxies, " ,;");
        while (st.hasMoreTokens())
            trustedProxies.add(st.nextToken());

        // Always trust the local host
        trustedProxies.add("127.0.0.1");

        try {
            String host = new java.net.URL(getBaseURL()).getHost();
            trustedProxies.add(InetAddress.getByName(host).getHostAddress());
        } catch (Exception ex) {
            LOGGER.warn("Could not determine IP of local host", ex);
        }

        for (String proxy : trustedProxies)
            LOGGER.debug("Trusted proxy: " + proxy);
    }

    /**
     * Returns the IP address of the client that made the request. When a
     * trusted proxy server was used, e. g. a local Apache mod_proxy in front of
     * Tomcat, the value of the last entry in the HTTP header X_FORWARDED_FOR is
     * returned, otherwise the REMOTE_ADDR is returned. The list of trusted
     * proxy IPs can be configured using the property
     * MCR.Request.TrustedProxies, which is a List of IP addresses separated by
     * blanks and/or comma.
     */
    public static String getRemoteAddr(HttpServletRequest req) {
        if (trustedProxies.isEmpty())
            initTrustedProxies();

        // Check if request comes in via a proxy
        // There are two possible header names
        String xForwardedFor = req.getHeader("X_FORWARDED_FOR");
        if ((xForwardedFor == null) || (xForwardedFor.trim().length() == 0)) {
            xForwardedFor = req.getHeader("x-forwarded-for");
        }

        // If no proxy is used, use client IP from HTTP request
        if ((xForwardedFor == null) || (xForwardedFor.trim().length() == 0))
            return req.getRemoteAddr();

        // X_FORWARDED_FOR can be comma separated list of hosts,
        // if so, take last entry, all others are not reliable because
        // any client may have set the header to any value.
        StringTokenizer st = new StringTokenizer(xForwardedFor, " ,;");
        while (st.hasMoreTokens())
            xForwardedFor = st.nextToken();

        // If request comes from a trusted proxy,
        // the best IP is the last entry in xForwardedFor
        if (trustedProxies.contains(req.getRemoteAddr()))
            return xForwardedFor;

        // Otherwise, use client IP from HTTP request
        return req.getRemoteAddr();
    }

    private static void putParamsToSession(HttpServletRequest request) {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement().toString();
            if (name.startsWith("XSL.") && name.endsWith(".SESSION")) {
                String key = name.substring(0, name.length() - 8);
                // parameter is not empty -> store
                if (!request.getParameter(name).trim().equals("")) {
                    mcrSession.put(key, request.getParameter(name));
                    LOGGER.debug("Found HTTP-Req.-Parameter " + name + "=" + request.getParameter(name) + " that should be saved in session, safed " + key
                            + "=" + request.getParameter(name));
                }
                // paramter is empty -> do not store and if contained in
                // session, remove from it
                else {
                    if (mcrSession.get(key) != null)
                        mcrSession.deleteObject(key);
                }
            }
        }
        for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement().toString();
            if (name.startsWith("XSL.") && name.endsWith(".SESSION")) {
                String key = name.substring(0, name.length() - 8);
                // attribute is not empty -> store
                if (!request.getAttribute(name).toString().trim().equals("")) {
                    mcrSession.put(key, request.getAttribute(name));
                    LOGGER.debug("Found HTTP-Req.-Attribute " + name + "=" + request.getParameter(name) + " that should be saved in session, safed " + key
                            + "=" + request.getParameter(name));
                }
                // attribute is empty -> do not store and if contained in
                // session, remove from it
                else {
                    if (mcrSession.get(key) != null)
                        mcrSession.deleteObject(key);
                }
            }
        }
    }
}
