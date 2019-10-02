/*
 * 
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.frontend.servlets;

import static org.mycore.frontend.MCRFrontendUtil.BASE_URL_ATTRIBUTE;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSessionResolver;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDirSetup;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xsl.MCRErrorListener;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This is the superclass of all MyCoRe servlets. It provides helper methods for logging and managing the current
 * session data. Part of the code has been taken from MilessServlet.java written by Frank Lützenkirchen.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRServlet extends HttpServlet {
    public static final String ATTR_MYCORE_SESSION = "mycore.session";

    private static final String CURRENT_THREAD_NAME_KEY = "currentThreadName";

    private static final String INITIAL_SERVLET_NAME_KEY = "currentServletName";

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LogManager.getLogger();

    private static String SERVLET_URL;

    private static final boolean ENABLE_BROWSER_CACHE = MCRConfiguration.instance().getBoolean(
        "MCR.Servlet.BrowserCache.enable", false);

    private static final String SESSION_NETMASK_IPV4_STRING = MCRConfiguration.instance()
        .getString("MCR.Servlet.Session.NetMask.IPv4", "255.255.255.255");

    private static final String SESSION_NETMASK_IPV6_STRING = MCRConfiguration.instance()
        .getString("MCR.Servlet.Session.NetMask.IPv6", "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF");

    private static byte[] SESSION_NETMASK_IPV4;

    private static byte[] SESSION_NETMASK_IPV6;

    private static MCRLayoutService LAYOUT_SERVICE;

    private static String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static MCRLayoutService getLayoutService() {
        return LAYOUT_SERVICE;
    }

    static {
        try {
            SESSION_NETMASK_IPV4 = InetAddress.getByName(SESSION_NETMASK_IPV4_STRING).getAddress();
        } catch (UnknownHostException e) {
            throw new MCRConfigurationException("MCR.Servlet.Session.NetMask.IPv4 is not a correct IPv4 network mask.",
                e);
        }
        try {
            SESSION_NETMASK_IPV6 = InetAddress.getByName(SESSION_NETMASK_IPV6_STRING).getAddress();
        } catch (UnknownHostException e) {
            throw new MCRConfigurationException("MCR.Servlet.Session.NetMask.IPv6 is not a correct IPv6 network mask.",
                e);
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        if (LAYOUT_SERVICE == null) {
            LAYOUT_SERVICE = MCRLayoutService.instance();
        }
    }

    /**
     * Returns the servlet base URL of the mycore system
     **/
    public static String getServletBaseURL() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object value = session.get(BASE_URL_ATTRIBUTE);
        if (value != null) {
            LOGGER.debug("Returning BaseURL {}servlets/ from user session.", value);
            return value + "servlets/";
        }
        return SERVLET_URL != null ? SERVLET_URL : MCRFrontendUtil.getBaseURL() + "servlets/";
    }

    /**
     * Initialisation of the static values for the base URL and servlet URL of the mycore system.
     */
    private static synchronized void prepareBaseURLs(ServletContext context, HttpServletRequest req) {
        String contextPath = req.getContextPath() + "/";

        String requestURL = req.getRequestURL().toString();
        int pos = requestURL.indexOf(contextPath, 9);
        String baseURLofRequest = requestURL.substring(0, pos) + contextPath;

        prepareBaseURLs(baseURLofRequest);
    }

    private static void prepareBaseURLs(String baseURLofRequest) {
        MCRFrontendUtil.prepareBaseURLs(baseURLofRequest);
        SERVLET_URL = MCRFrontendUtil.getBaseURL() + "servlets/";
    }

    // The methods doGet() and doPost() simply call the private method
    // doGetPost(),
    // i.e. GET- and POST requests are handled by one method only.
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            doGetPost(req, res);
        } catch (SAXException | TransformerException e) {
            throwIOException(e);
        }
    }

    private void throwIOException(Exception e) throws IOException {
        if (e instanceof IOException) {
            throw (IOException) e;
        }
        if (e instanceof TransformerException) {
            TransformerException te = MCRErrorListener.unwrapException((TransformerException) e);
            String myMessageAndLocation = MCRErrorListener.getMyMessageAndLocation(te);
            throw new IOException("Error while XSL Transformation: " + myMessageAndLocation, e);
        }
        if (e instanceof SAXParseException) {
            SAXParseException spe = (SAXParseException) e;
            String id = spe.getSystemId() != null ? spe.getSystemId() : spe.getPublicId();
            int line = spe.getLineNumber();
            int column = spe.getColumnNumber();
            String msg = new MessageFormat("Error on {0}:{1} while parsing {2}", Locale.ROOT)
                .format(new Object[] { line, column, id });
            throw new IOException(msg, e);
        }
        throw new IOException(e);
    }

    protected void doGet(MCRServletJob job) throws Exception {
        doGetPost(job);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            doGetPost(req, res);
        } catch (SAXException | TransformerException e) {
            throwIOException(e);
        }
    }

    protected void doPost(MCRServletJob job) throws Exception {
        doGetPost(job);
    }

    public static MCRSession getSession(HttpServletRequest req) {
        boolean reusedSession = req.isRequestedSessionIdValid();
        HttpSession theSession = req.getSession(true);
        if (reusedSession) {
            LOGGER.debug(() -> "Reused HTTP session: " + theSession.getId() + ", created: " + LocalDateTime
                .ofInstant(Instant.ofEpochMilli(theSession.getCreationTime()), ZoneId.systemDefault()));
        } else {
            LOGGER.info(() -> "Created new HTTP session: " + theSession.getId());
        }
        MCRSession session = null;

        MCRSession fromHttpSession = Optional
            .ofNullable((MCRSessionResolver) theSession.getAttribute(ATTR_MYCORE_SESSION))
            .flatMap(MCRSessionResolver::resolveSession)
            .orElse(null);

        MCRSessionMgr.unlock();
        if (fromHttpSession != null && fromHttpSession.getID() != null) {
            // Take session from HttpSession with servlets
            session = fromHttpSession;

            String lastIP = session.getCurrentIP();
            String newIP = MCRFrontendUtil.getRemoteAddr(req);
            String hostIP = MCRFrontendUtil.getHostIP();

            try {
                InetAddress lastIPAddress = InetAddress.getByName(lastIP);
                InetAddress newIPAddress = InetAddress.getByName(newIP);
                InetAddress hostIPAddress = InetAddress.getByName(hostIP);

                byte[] lastIPMask = decideNetmask(lastIPAddress);
                byte[] newIPMask = decideNetmask(newIPAddress);
                byte[] hostIPMask = decideNetmask(hostIPAddress);

                lastIPAddress = InetAddress.getByAddress(filterIPByNetmask(lastIPAddress.getAddress(), lastIPMask));
                newIPAddress = InetAddress.getByAddress(filterIPByNetmask(newIPAddress.getAddress(), newIPMask));
                hostIPAddress = InetAddress.getByAddress(filterIPByNetmask(hostIPAddress.getAddress(), hostIPMask));

                if (!lastIPAddress.equals(newIPAddress) && !newIPAddress.equals(hostIPAddress)) {
                    LOGGER.warn("Session steal attempt from IP {}, previous IP was {}. Session: {}", newIP, lastIP,
                        session);
                    MCRSessionMgr.releaseCurrentSession();
                    session.close(); //MCR-1409 do not leak old session
                    MCRSessionMgr.unlock();//due to release above
                    session = MCRSessionMgr.getCurrentSession();
                    session.setCurrentIP(newIP);
                }
            } catch (UnknownHostException e) {
                throw new MCRException("Wrong transformation of IP address for this session.", e);
            } catch (IOException e) {
                throw new MCRException(e);
            }
        } else {
            // Create a new session
            session = MCRSessionMgr.getCurrentSession();
        }

        // Store current session in HttpSession
        theSession.setAttribute(ATTR_MYCORE_SESSION, new MCRSessionResolver(session));
        // store the HttpSession ID in MCRSession
        if (session.put("http.session", theSession.getId()) == null) {
            //first request
            MCRStreamUtils.asStream(req.getLocales())
                .map(Locale::toString)
                .filter(MCRTranslation.getAvailableLanguages()::contains)
                .findFirst()
                .ifPresent(session::setCurrentLanguage);
        }
        // Forward MCRSessionID to XSL Stylesheets
        req.setAttribute("XSL.MCRSessionID", session.getID());

        return session;
    }

    private static byte[] filterIPByNetmask(final byte[] ip_array, final byte[] mask_array) {
        for (int i = 0; i < ip_array.length; i++) {
            ip_array[i] = (byte) (ip_array[i] & mask_array[i]);
        }
        return ip_array;
    }

    private static byte[] decideNetmask(InetAddress IP) throws IOException {
        if (hasIPVersion(IP, 4)) {
            return SESSION_NETMASK_IPV4;
        } else if (hasIPVersion(IP, 6)) {
            return SESSION_NETMASK_IPV6;
        } else {
            throw new IOException("Unknown or unidentifiable version of IP: " + IP);
        }
    }

    private static Boolean hasIPVersion(InetAddress IP, int version) {
        int byteLength;
        switch (version) {
            case 4:
                byteLength = 4;
                break;
            case 6:
                byteLength = 16;
                break;
            default:
                throw new IndexOutOfBoundsException("Unknown IP version: " + version);
        }
        return IP.getAddress().length == byteLength;
    }

    private static void bindSessionToRequest(HttpServletRequest req, String servletName, MCRSession session) {
        if (!isSessionBoundToRequest(req)) {
            // Bind current session to this thread:
            MCRSessionMgr.setCurrentSession(session);
            req.setAttribute(CURRENT_THREAD_NAME_KEY, Thread.currentThread().getName());
            req.setAttribute(INITIAL_SERVLET_NAME_KEY, servletName);
        }
    }

    private static boolean isSessionBoundToRequest(HttpServletRequest req) {
        String currentThread = getProperty(req, CURRENT_THREAD_NAME_KEY);
        // check if this is request passed the same thread before
        // (RequestDispatcher)
        return currentThread != null && currentThread.equals(Thread.currentThread().getName());
    }

    /**
     * This private method handles both GET and POST requests and is invoked by doGet() and doPost().
     * 
     * @param req
     *            the HTTP request instance
     * @param res
     *            the HTTP response instance
     * @exception IOException
     *                for java I/O errors.
     * @exception ServletException
     *                for errors from the servlet engine.
     */
    private void doGetPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException,
        SAXException, TransformerException {
        if (MCRConfiguration.instance() == null) {
            // removes NullPointerException below, if somehow Servlet is not yet
            // intialized
            init();
        }
        initializeMCRSession(req, getServletName());

        if (SERVLET_URL == null) {
            prepareBaseURLs(getServletContext(), req);
        }

        MCRServletJob job = new MCRServletJob(req, res);
        MCRSession session = MCRSessionMgr.getCurrentSession();

        try {
            // transaction around 1st phase of request
            Exception thinkException = processThinkPhase(job);
            // first phase completed, start rendering phase
            processRenderingPhase(job, thinkException);
        } catch (Error error) {
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                session.rollbackTransaction();
            }
            throw error;
        } catch (Exception ex) {
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                session.rollbackTransaction();
            }
            if (isBrokenPipe(ex)) {
                LOGGER.info("Ignore broken pipe.");
                return;
            }
            if (ex.getMessage() == null) {
                LOGGER.error("Exception while in rendering phase.", ex);
            } else {
                LOGGER.error("Exception while in rendering phase: {}", ex.getMessage());
            }
            if (ex instanceof ServletException) {
                throw (ServletException) ex;
            } else if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof SAXException) {
                throw (SAXException) ex;
            } else if (ex instanceof TransformerException) {
                throw (TransformerException) ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        } finally {
            cleanupMCRSession(req, getServletName());
        }
    }

    /**
     * Code to initialize a MyCoRe Session
     * may be reused in ServletFilter, MVC controller, etc.
     * 
     * @param req - the HTTP request
     * @param servletName - the servletName
     * @throws IOException
     */
    public static void initializeMCRSession(HttpServletRequest req, String servletName) throws IOException {
        // Try to set encoding of form values
        String reqCharEncoding = req.getCharacterEncoding();

        if (reqCharEncoding == null) {
            // Set default to UTF-8
            reqCharEncoding = MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8");
            req.setCharacterEncoding(reqCharEncoding);
            LOGGER.debug("Setting ReqCharEncoding to: {}", reqCharEncoding);
        }

        if ("true".equals(req.getParameter("reload.properties"))) {
            MCRConfigurationDirSetup setup = new MCRConfigurationDirSetup();
            setup.startUp(req.getServletContext());
        }
        if (getProperty(req, INITIAL_SERVLET_NAME_KEY) == null) {
            MCRSession session = getSession(req);
            bindSessionToRequest(req, servletName, session);
        }
    }

    /**
     * Code to cleanup a MyCoRe Session
     * may be reused in ServletFilter, MVC controller, etc. 
     * @param req - the HTTP Request
     * @param servletName - the Servlet name
     */
    public static void cleanupMCRSession(HttpServletRequest req, String servletName) {
        // Release current MCRSession from current Thread,
        // in case that Thread pooling will be used by servlet engine
        if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(servletName)) {
            // current Servlet not called via RequestDispatcher
            MCRSessionMgr.releaseCurrentSession();
        }
    }

    private static boolean isBrokenPipe(Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null && throwable instanceof IOException && message.contains("Broken pipe")) {
            return true;
        }
        return throwable.getCause() != null && isBrokenPipe(throwable.getCause());
    }

    private void configureSession(MCRServletJob job) {
        MCRSession session = MCRSessionMgr.getCurrentSession();

        String longName = getClass().getName();
        final String shortName = longName.substring(longName.lastIndexOf(".") + 1);

        LOGGER.info(() -> String
            .format(Locale.ROOT, "%s ip=%s mcr=%s path=%s", shortName, MCRFrontendUtil.getRemoteAddr(job.getRequest()),
                session.getID(), job.getRequest().getPathInfo()));

        MCRFrontendUtil.configureSession(session, job.getRequest(), job.getResponse());
    }

    private Exception processThinkPhase(MCRServletJob job) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                session.beginTransaction();
            }
            configureSession(job);
            think(job);
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                session.commitTransaction();
            }
        } catch (Exception ex) {
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                LOGGER.warn("Exception occurred, performing database rollback.");
                session.rollbackTransaction();
            } else {
                LOGGER.warn("Exception occurred, cannot rollback database transaction right now.");
            }
            return ex;
        }
        return null;
    }

    /**
     * 1st phase of doGetPost. This method has a seperate transaction. Per default id does nothing as a fallback to the
     * old behaviour.
     * 
     * @see #render(MCRServletJob, Exception)
     */
    protected void think(MCRServletJob job) throws Exception {
        // not implemented by default
    }

    private void processRenderingPhase(MCRServletJob job, Exception thinkException) throws Exception {
        if (allowCrossDomainRequests() && !job.getResponse().containsHeader(ACCESS_CONTROL_ALLOW_ORIGIN)) {
            job.getResponse().setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }
        MCRSession session = MCRSessionMgr.getCurrentSession();
        if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
            // current Servlet not called via RequestDispatcher
            session.beginTransaction();
        }
        render(job, thinkException);
        if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
            // current Servlet not called via RequestDispatcher
            session.commitTransaction();
        }
    }

    /**
     * Returns true if this servlet allows Cross-domain requests. The default value defined by {@link MCRServlet} is
     * <code>false</code>.
     */
    protected boolean allowCrossDomainRequests() {
        return false;
    }

    /**
     * 2nd phase of doGetPost This method has a seperate transaction and gets the same MCRServletJob from the first
     * phase (think) and any exception that occurs at the first phase. By default this method calls
     * doGetPost(MCRServletJob) as a fallback to the old behaviour.
     * 
     * @param job
     *            same instance as of think(MCRServlet job)
     * @param ex
     *            any exception thrown by think(MCRServletJob) or transaction commit
     * @throws Exception
     *             if render could not handle <code>ex</code> to produce a nice user page
     */
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        // no info here how to handle
        if (ex != null)
            throw ex;
        if (job.getRequest().getMethod().equals("POST")) {
            doPost(job);
        } else {
            doGet(job);
        }
    }

    /**
     * This method should be overwritten by other servlets. As a default response we indicate the HTTP 1.1 status code
     * 501 (Not Implemented).
     */
    protected void doGetPost(MCRServletJob job) throws Exception {
        job.getResponse().sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    /** Handles an exception by reporting it and its embedded exception */
    protected void handleException(Exception ex) {
        try {
            reportException(ex);
        } catch (Exception ignored) {
            LOGGER.error(ignored);
        }
    }

    /** Reports an exception to the log */
    protected void reportException(Exception ex) throws Exception {
        String cname = this.getClass().getName();
        String servlet = cname.substring(cname.lastIndexOf(".") + 1);

        LOGGER.warn("Exception caught in : {}", servlet, ex);
    }

    /**
     * This method builds a URL that can be used to redirect the client browser to another page, thereby including http
     * request parameters. The request parameters will be encoded as http get request.
     * 
     * @param baseURL
     *            the base url of the target webpage
     * @param parameters
     *            the http request parameters
     */
    protected String buildRedirectURL(String baseURL, Properties parameters) {
        StringBuilder redirectURL = new StringBuilder(baseURL);
        boolean first = true;
        for (Enumeration<?> e = parameters.keys(); e.hasMoreElements();) {
            if (first) {
                redirectURL.append("?");
                first = false;
            } else {
                redirectURL.append("&");
            }

            String name = (String) e.nextElement();
            String value = null;
            value = URLEncoder.encode(parameters.getProperty(name), StandardCharsets.UTF_8);

            redirectURL.append(name).append("=").append(value);
        }
        LOGGER.debug("Sending redirect to {}", redirectURL);
        return redirectURL.toString();
    }

    /**
     * allows browser to cache requests. This method is usefull as it allows browsers to cache content that is not
     * changed. Please overwrite this method in every Servlet that depends on "remote" data.
     */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        if (ENABLE_BROWSER_CACHE) {
            // we can cache every (local) request
            long lastModified = MCRSessionMgr.getCurrentSession().getLoginTime() > MCRConfiguration.instance()
                .getSystemLastModified() ? MCRSessionMgr.getCurrentSession().getLoginTime()
                    : MCRConfiguration
                        .instance().getSystemLastModified();
            LOGGER.info("LastModified: {}", lastModified);
            return lastModified;
        }
        return -1; // time is not known
    }

    public static String getProperty(HttpServletRequest request, String name) {
        return MCRFrontendUtil.getProperty(request, name).orElse(null);
    }

    /**
     * returns a translated error message for the current Servlet. I18N keys are of form
     * {prefix}'.'{SimpleServletClassName}'.'{subIdentifier}
     * 
     * @param prefix
     *            a prefix of the message property like component.base.error
     * @param subIdentifier
     *            last part of I18n key
     * @param args
     *            any arguments that should be passed to {@link MCRTranslation#translate(String, Object...)}
     */
    protected String getErrorI18N(String prefix, String subIdentifier, Object... args) {
        String key = new MessageFormat("{0}.{1}.{2}", Locale.ROOT)
            .format(new Object[] { prefix, getClass().getSimpleName(), subIdentifier });
        return MCRTranslation.translate(key, args);
    }

    /**
     * Returns the referer of the given request.
     */
    protected URL getReferer(HttpServletRequest request) {
        String referer;
        referer = request.getHeader("Referer");
        if (referer == null) {
            return null;
        }
        try {
            return new URL(referer);
        } catch (MalformedURLException e) {
            //should not happen
            LOGGER.error("Referer is not a valid URL: {}", referer, e);
            return null;
        }
    }

    /**
     * If a referrer is available this method redirects to the url given by the referrer otherwise method redirects to
     * the application base url.
     */
    protected void toReferrer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        URL referrer = getReferer(request);
        if (referrer != null) {
            response.sendRedirect(response.encodeRedirectURL(referrer.toString()));
        } else {
            LOGGER.warn("Could not get referrer, returning to the application's base url");
            response.sendRedirect(response.encodeRedirectURL(MCRFrontendUtil.getBaseURL()));
        }
    }

    /**
     * If a referrer is available this method redirects to the url given by the referrer otherwise method redirects to
     * the alternative-url.
     */
    protected void toReferrer(HttpServletRequest request, HttpServletResponse response, String altURL)
        throws IOException {
        URL referrer = getReferer(request);
        if (referrer != null) {
            response.sendRedirect(response.encodeRedirectURL(referrer.toString()));
        } else {
            LOGGER.warn("Could not get referrer, returning to {}", altURL);
            response.sendRedirect(response.encodeRedirectURL(altURL));
        }
    }
}
