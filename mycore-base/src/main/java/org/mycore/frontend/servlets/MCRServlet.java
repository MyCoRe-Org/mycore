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

package org.mycore.frontend.servlets;

import static org.mycore.frontend.MCRFrontendUtil.BASE_URL_ATTRIBUTE;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSessionResolver;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationDirSetup;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xsl.MCRErrorListener;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * This is the superclass of all MyCoRe servlets. It provides helper methods for logging and managing the current
 * session data. Part of the code has been taken from MilessServlet.java written by Frank Lützenkirchen.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRServlet extends HttpServlet {
    public static final String ATTR_MYCORE_SESSION = "mycore.session";

    public static final String CURRENT_THREAD_NAME_KEY = "currentThreadName";

    public static final String INITIAL_SERVLET_NAME_KEY = "currentServletName";

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private static String servletUrl;

    private static final boolean ENABLE_BROWSER_CACHE = MCRConfiguration2.getBoolean("MCR.Servlet.BrowserCache.enable")
        .orElse(false);

    private static MCRLayoutService layoutService;

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static MCRLayoutService getLayoutService() {
        return layoutService;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        if (layoutService == null) {
            layoutService = MCRLayoutService.obtainInstance();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String servletName = config.getServletName();
        boolean disabled = MCRConfiguration2.getBoolean("MCR.Servlet." + servletName + ".Disabled")
            .orElse(false);

        if (disabled) {
            throw new UnavailableException("Servlet " + servletName + " is disabled in configuration.");
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
        return servletUrl != null ? servletUrl : MCRFrontendUtil.getBaseURL() + "servlets/";
    }

    /**
     * Initialisation of the static values for the base URL and servlet URL of the mycore system.
     */
    private static synchronized void prepareBaseURLs(HttpServletRequest req) {
        String contextPath = req.getContextPath() + "/";

        String requestURL = req.getRequestURL().toString();
        int pos = requestURL.indexOf(contextPath, 9);
        String baseURLofRequest = requestURL.substring(0, pos) + contextPath;

        prepareBaseURLs(baseURLofRequest);
    }

    private static void prepareBaseURLs(String baseURLofRequest) {
        MCRFrontendUtil.prepareBaseURLs(baseURLofRequest);
        servletUrl = MCRFrontendUtil.getBaseURL() + "servlets/";
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
        if (e instanceof IOException ioe) {
            throw ioe;
        }
        if (e instanceof TransformerException tfe) {
            TransformerException te = MCRErrorListener.unwrapException(tfe);
            String myMessageAndLocation = MCRErrorListener.getMyMessageAndLocation(te);
            throw new IOException("Error while XSL Transformation: " + myMessageAndLocation, e);
        }
        if (e instanceof SAXParseException spe) {
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

    @SuppressWarnings("PMD.UnusedAssignment")
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

            try {
                if (!MCRFrontendUtil.isIPAddrAllowed(lastIP, newIP)) {
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
            //for MCRTranslation.getAvailableLanguages()
            MCRTransactionManager.beginTransactions();
            try {
                String acceptLanguage = req.getHeader("Accept-Language");
                if (acceptLanguage != null) {
                    List<Locale.LanguageRange> languageRanges = Locale.LanguageRange.parse(acceptLanguage);
                    LOGGER.debug("accept languages: {}", languageRanges);
                    MCRSession finalSession = session;
                    Optional
                        .ofNullable(Locale.lookupTag(languageRanges, MCRTranslation.getAvailableLanguages()))
                        .ifPresent(selectedLanguage -> {
                            LOGGER.debug("selected language: {}", selectedLanguage);
                            finalSession.setCurrentLanguage(selectedLanguage);
                        });
                }
            } catch (IllegalArgumentException e) {
                //from Locale.LanguageRange.parse(...)
                //error example (found in tomcat logs): "range=no;en-us"
                //do nothing = same behaviour as with no Accept-Language-Header set
            } finally {
                if (MCRTransactionManager.hasRollbackOnlyTransactions()) {
                    MCRTransactionManager.rollbackTransactions();
                }
                MCRTransactionManager.commitTransactions();
            }
        }
        // Forward MCRSessionID to XSL Stylesheets
        req.setAttribute("XSL.MCRSessionID", session.getID());

        return session;
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
        initializeMCRSession(req, getServletName());

        if (servletUrl == null) {
            prepareBaseURLs(req);
        }

        MCRServletJob job = new MCRServletJob(req, res);

        @SuppressWarnings("unused")
        MCRSession session = MCRSessionMgr.getCurrentSession();

        try {
            // transaction around 1st phase of request
            Exception thinkException = processThinkPhase(job);
            // first phase completed, start rendering phase
            processRenderingPhase(job, thinkException);
        } catch (Error error) {
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                MCRTransactionManager.rollbackTransactions();
            }
            throw error;
        } catch (ServletException | IOException | SAXException | TransformerException | RuntimeException ex) {
            if (isHandleExceptionComplete(req, ex)) {
                return;
            }
            throw ex;
        } catch (Exception ex) {
            if (isHandleExceptionComplete(req, ex)) {
                return;
            }
            throw new MCRException(ex);
        } finally {
            cleanupMCRSession(req, getServletName());
        }
    }

    private boolean isHandleExceptionComplete(HttpServletRequest req, Exception ex) {
        if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
            // current Servlet not called via RequestDispatcher
            MCRTransactionManager.rollbackTransactions();
        }
        if (isBrokenPipe(ex)) {
            LOGGER.info("Ignore broken pipe.");
            return true;
        }
        if (ex.getMessage() == null) {
            LOGGER.error("Exception while in rendering phase.", ex);
        } else {
            LOGGER.error("Exception while in rendering phase: {}", ex::getMessage);
        }
        return false;
    }

    /**
     * Code to initialize a MyCoRe Session
     * may be reused in ServletFilter, MVC controller, etc.
     *
     * @param req - the HTTP request
     * @param servletName - the servletName
     */
    public static void initializeMCRSession(HttpServletRequest req, String servletName) throws IOException {
        // Try to set encoding of form values
        String reqCharEncoding = req.getCharacterEncoding();

        if (reqCharEncoding == null) {
            // Set default to UTF-8
            reqCharEncoding = MCRConfiguration2.getString("MCR.Request.CharEncoding").orElse("UTF-8");
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
        boolean result;
        if (message != null && throwable instanceof IOException && message.contains("Broken pipe")) {
            result = true;
        } else {
            result = throwable.getCause() != null && isBrokenPipe(throwable.getCause());
        }
        return result;
    }

    private void configureSession(MCRServletJob job) {
        MCRSession session = MCRSessionMgr.getCurrentSession();

        String longName = getClass().getName();
        final String shortName = longName.substring(longName.lastIndexOf('.') + 1);

        LOGGER.info(() -> String
            .format(Locale.ROOT, "%s ip=%s mcr=%s path=%s", shortName, MCRFrontendUtil.getRemoteAddr(job.getRequest()),
                session.getID(), job.getRequest().getPathInfo()));

        MCRFrontendUtil.configureSession(session, job.getRequest(), job.getResponse());
    }

    private Exception processThinkPhase(MCRServletJob job) {
        @SuppressWarnings("unused")
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                MCRTransactionManager.beginTransactions();
            }
            configureSession(job);
            think(job);
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                MCRTransactionManager.commitTransactions();
            }
        } catch (Exception ex) {
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                LOGGER.warn("Exception occurred, performing database rollback.");
                MCRTransactionManager.rollbackTransactions();
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
        @SuppressWarnings("unused")
        MCRSession session = MCRSessionMgr.getCurrentSession();
        if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
            // current Servlet not called via RequestDispatcher
            MCRTransactionManager.beginTransactions();
        }
        render(job, thinkException);
        if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
            // current Servlet not called via RequestDispatcher
            MCRTransactionManager.commitTransactions();
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
        if (ex != null) {
            throw ex;
        }
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
    protected void reportException(Exception ex) {
        String cname = this.getClass().getName();
        String servlet = cname.substring(cname.lastIndexOf('.') + 1);

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
                redirectURL.append('?');
                first = false;
            } else {
                redirectURL.append('&');
            }

            String name = (String) e.nextElement();
            String value = URLEncoder.encode(parameters.getProperty(name), StandardCharsets.UTF_8);

            redirectURL.append(name).append('=').append(value);
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
            long lastModified = MCRSessionMgr.getCurrentSession().getLoginTime() > MCRConfigurationBase
                .getSystemLastModified() ? MCRSessionMgr.getCurrentSession().getLoginTime()
                    : MCRConfigurationBase.getSystemLastModified();
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
    protected URI getReferer(HttpServletRequest request) {
        String referer;
        referer = request.getHeader("Referer");
        if (referer == null) {
            return null;
        }
        try {
            return new URI(referer);
        } catch (URISyntaxException e) {
            //should not happen
            LOGGER.error("Referer is not a valid URI: {}", referer, e);
            return null;
        }
    }

    /**
     * If a referrer is available this method redirects to the url given by the referrer otherwise method redirects to
     * the application base url.
     */
    protected void redirectToReferrer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        URI referrer = getReferer(request);
        if (referrer != null && MCRFrontendUtil.isSafeRedirect(referrer.toString())) {
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
    protected void redirectToReferrer(HttpServletRequest request, HttpServletResponse response, String altURL)
        throws IOException {
        URI referrer = getReferer(request);
        if (referrer != null && MCRFrontendUtil.isSafeRedirect(referrer.toString())) {
            response.sendRedirect(response.encodeRedirectURL(referrer.toString()));
        } else {
            LOGGER.warn("Could not get referrer, returning to {}", altURL);
            response.sendRedirect(response.encodeRedirectURL(altURL));
        }
    }
}
