/*
 * $Id$
 * $Revision: 5697 $ $Date: 04.10.2010 $
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
import java.text.MessageFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRLayoutService;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRErrorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRErrorServlet.class);

    private static MCRLayoutService LAYOUT_SERVICE = MCRLayoutService.instance();

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Retrieve the possible error attributes, some may be null
        Integer statusCode = (Integer) req.getAttribute("javax.servlet.error.status_code");
        String message = (String) req.getAttribute("javax.servlet.error.message");
        @SuppressWarnings("unchecked")
        Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) req.getAttribute("javax.servlet.error.exception_type");
        Throwable exception = (Throwable) req.getAttribute("javax.servlet.error.exception");
        String requestURI = (String) req.getAttribute("javax.servlet.error.request_uri");
        String servletName = (String) req.getAttribute("javax.servlet.error.servletName");
        MCRSession session = getMCRSession(req, servletName);
        if (session != null && !MCRSessionMgr.hasCurrentSession()) {
            MCRSessionMgr.setCurrentSession(session);
        }
        try {
            generateErrorPage(req, resp, message, exception, statusCode, exceptionType, requestURI, servletName);
        } finally {
            if (session != null) {
                MCRSessionMgr.releaseCurrentSession();
            }
        }
    }

    private MCRSession getMCRSession(HttpServletRequest req, String servletName) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return MCRServlet.getSession(req, servletName);
    }

    protected void generateErrorPage(HttpServletRequest request, HttpServletResponse response, String msg, Throwable ex, Integer statusCode,
        Class<? extends Throwable> exceptionType, String requestURI, String servletName) throws IOException {
        LOGGER.log(ex == null ? Level.WARN : Level.ERROR,
            MessageFormat.format("{0}: Error {1} occured. The following message was given: {2}", requestURI, statusCode, msg), ex);

        String rootname = "mcr_error";
        String style = MCRServlet.getProperty(request, "XSL.Style");
        if (style == null || !style.equals("xml")) {
            style = "default";
        }
        Element root = new Element(rootname);
        root.setAttribute("errorServlet", Boolean.TRUE.toString());
        root.setAttribute("space", "preserve", Namespace.XML_NAMESPACE);
        if (msg != null) {
            root.setText(msg);
        }
        if (statusCode != null) {
            root.setAttribute("HttpError", statusCode.toString());
        }
        if (requestURI != null) {
            root.setAttribute("requestURI", requestURI);
        }
        if (exceptionType != null) {
            root.setAttribute("exceptionType", exceptionType.getName());
        }
        if (servletName != null) {
            root.setAttribute("servletName", servletName);
        }

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
            ex = ex.getCause();
        }

        request.setAttribute("XSL.Style", style);

        final String requestAttr = "MCRErrorServlet.generateErrorPage";
        if (!response.isCommitted() && request.getAttribute(requestAttr) == null) {
            if (statusCode != null) {
                response.setStatus(statusCode);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            request.setAttribute(requestAttr, msg);
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            boolean openTransaction = currentSession.isTransactionActive();
            if (!openTransaction) {
                currentSession.beginTransaction();
            }
            LAYOUT_SERVICE.doLayout(request, response, errorDoc);
            if (!openTransaction)
                currentSession.commitTransaction();
            return;
        } else {
            if (request.getAttribute(requestAttr) != null) {
                LOGGER.warn("Could not send error page. Generating error page failed. The original message:\n" + request.getAttribute(requestAttr));
            } else {
                LOGGER.warn("Could not send error page. Response allready commited. The following message was given:\n" + msg);
            }
        }
    }
}
