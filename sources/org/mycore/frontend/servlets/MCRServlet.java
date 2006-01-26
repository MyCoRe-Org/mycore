/*
 * $RCSfile$
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRLayoutServlet;
import org.mycore.datamodel.metadata.MCRActiveLinkException;

/**
 * This is the superclass of all MyCoRe servlets. It provides helper methods for
 * logging and managing the current session data. Part of the code has been
 * taken from MilessServlet.java written by Frank L?tzenkirchen.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Some configuration details
	protected static MCRConfiguration CONFIG = MCRConfiguration.instance();

	private static Logger LOGGER = Logger.getLogger(MCRServlet.class);;

	private static String BASE_URL;

	private static String SERVLET_URL;

	// These values serve to remember if we have a GET or POST request
	private final static boolean GET = true;

	private final static boolean POST = false;

	protected static MCRCache requestParamCache = new MCRCache(40);

	/** returns the base URL of the mycore system */
	public static String getBaseURL() {
		return BASE_URL;
	}

	/** returns the servlet base URL of the mycore system */
	public static String getServletBaseURL() {
		return SERVLET_URL;
	}

	/**
	 * Initialisation of the static values for the base URL and servlet URL of
	 * the mycore system.
	 */
	private static synchronized void prepareURLs(HttpServletRequest req) {
		String contextPath = req.getContextPath();

		if (contextPath == null) {
			contextPath = "";
		}

		contextPath += "/";

		String requestURL = req.getRequestURL().toString();
		int pos = requestURL.indexOf(contextPath, 9);
		BASE_URL = requestURL.substring(0, pos) + contextPath;

		SERVLET_URL = BASE_URL + "servlets/";
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

	public static MCRSession getSession(HttpServletRequest req) {
		HttpSession theSession = req.getSession(true);
		MCRSession session = null;

		String sessionID = req.getParameter("MCRSessionID");
		MCRSession fromRequest = null;

		if (sessionID != null) {
			fromRequest = MCRSession.getSession(sessionID);
		}

		MCRSession fromHttpSession = (MCRSession) theSession.getAttribute("mycore.session");

		// Choose:
		if (fromRequest != null) {
			session = fromRequest;
		}
		// Take session from http request parameter MCRSessionID
		else if (fromHttpSession != null) {
			session = fromHttpSession;
		}
		// Take session from HttpSession with servlets
		else {
			session = MCRSessionMgr.getCurrentSession();
		}

		// Create a new session
		// Store current session in HttpSession
		theSession.setAttribute("mycore.session", session);

		// Bind current session to this thread:
		MCRSessionMgr.setCurrentSession(session);

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
			ReqCharEncoding = CONFIG.getString("MCR.request_charencoding", "UTF-8");
			req.setCharacterEncoding(ReqCharEncoding);
			LOGGER.debug("Setting ReqCharEncoding to: " + ReqCharEncoding);
		}

		if ("true".equals(req.getParameter("reload.properties"))) {
			MCRConfiguration.instance().reload(true);
		}

		if (BASE_URL == null) {
			prepareURLs(req);
		}

		try {
			MCRSession session = getSession(req);

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

			MCRServletJob job = new MCRServletJob(req, res);

			// Uebernahme der gewuenschten Sprache aus dem Request zunaechst mal
			// nur als Test!!!
			String lang = getProperty(req, "lang");

			if ((lang != null) && (lang.trim().length() != 0)) {
				session.setCurrentLanguage(lang.trim());
			}

			// Set the IP of the current session
			if (session.getCurrentIP().length() == 0) {
				session.setCurrentIP(getRemoteAddr(req));
			}

			if (GETorPOST == GET) {
				doGet(job);
			} else {
				doPost(job);
			}
		} catch (Exception ex) {
			if (ex instanceof ServletException) {
				throw (ServletException) ex;
			} else if (ex instanceof IOException) {
				throw (IOException) ex;
			} else {
				handleException(ex);
				generateErrorPage(req, res, 500, ex.getMessage(), ex, false);
			}
		} finally {
			// Release current MCRSession from current Thread,
			// in case that Thread pooling will be used by servlet engine
			MCRSessionMgr.releaseCurrentSession();
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
			throws IOException, ServletException {
		LOGGER.error(getClass().getName() + ": Error " + error + " occured. The following message was given: " + msg, ex);

		String defaultLang = CONFIG.getString("MCR.metadata_default_lang", "de");
		String lang = (getProperty(request, "lang") != null) ? getProperty(request, "lang") : defaultLang;
		String style = (xmlstyle) ? "xml" : ("query-" + lang);

		String rootname = "mcr_error";
		Element root = new Element(rootname);
		root.setAttribute("HttpError", Integer.toString(error)).setText(msg);

		Document errorDoc = new Document(root, new DocType(rootname));

		while (ex != null) {
			Element exception = new Element("exception");
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

		request.setAttribute(MCRLayoutServlet.JDOM_ATTR, errorDoc);
		request.setAttribute("XSL.Style", style);

        if (!response.isCommitted()){
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(request, response);
        } else {
            LOGGER.warn("Could not send error page. Response allready commited. The following message was given:\n"+msg,ex);
        }
	}

	protected void generateActiveLinkErrorpage(HttpServletRequest request, HttpServletResponse response, String msg, MCRActiveLinkException activeLinks)
			throws IOException, ServletException {
		StringBuffer msgBuf = new StringBuffer(msg);
		msgBuf.append("\nThere are links active preventing the commit of work, see error message for details. The following links where affected:");
		Map links = activeLinks.getActiveLinks();
		Iterator destIt = links.keySet().iterator();
		String curDest;
		while (destIt.hasNext()) {
			curDest = destIt.next().toString();
			List sources = (List) links.get(curDest);
			Iterator sourceIt = sources.iterator();
			while (sourceIt.hasNext()) {
				msgBuf.append('\n').append(sourceIt.next().toString()).append("==>").append(curDest);
			}
		}
		generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msgBuf.toString(), activeLinks, false);
	}

	protected static String getProperty(HttpServletRequest request, String name) {
		String value = (String) request.getAttribute(name);

		// if Attribute not given try Parameter
		if ((value == null) || (value.length() == 0)) {
			value = request.getParameter(name);
		}

		return value;
	}

	/**
	 * Returns the IP address of the client that made the request. When a proxy
	 * server was used, e. g. Apache mod_proxy in front of Tomcat, the value of
	 * the HTTP header X_FORWARDED_FOR is returned, otherwise the REMOTE_ADDR is
	 * returned
	 */
	public static String getRemoteAddr(HttpServletRequest req) {
		String addr = req.getHeader("X_FORWARDED_FOR");

        if ((addr == null) || (addr.trim().length() == 0)) {
            addr = req.getHeader("x-forwarded-for");
        }

        if ((addr == null) || (addr.trim().length() == 0)) {
            addr = req.getRemoteAddr();
        }

		return addr;
	}
}
