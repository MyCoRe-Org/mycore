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

import java.io.IOException;
import java.io.Serial;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.MCRFrontendUtil;
import org.xml.sax.SAXException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRErrorServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRLayoutService LAYOUT_SERVICE = MCRLayoutService.obtainInstance();

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServlet#service(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Retrieve the possible error attributes, some may be null
        Integer statusCode = (Integer) req.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) req.getAttribute("jakarta.servlet.error.message");
        @SuppressWarnings("unchecked")
        Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) req
            .getAttribute("jakarta.servlet.error.exception_type");
        Throwable exception = (Throwable) req.getAttribute("jakarta.servlet.error.exception");
        String requestURI = (String) req.getAttribute("jakarta.servlet.error.request_uri");
        String servletName = (String) req.getAttribute("jakarta.servlet.error.servletName");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling error {} for request ''{}'' message: {}", statusCode, requestURI, message);
            LOGGER.debug("Has current session: {}", MCRSessionMgr.hasCurrentSession());
        }
        if (acceptWebPage(req)) {
            try {
                generateErrorPage(req, resp, message, exception, statusCode, exceptionType, requestURI, servletName);
            } catch (TransformerException | SAXException e) {
                LOGGER.error("Could not generate error page", e);
                resp.sendError(statusCode, message);
            }
        } else {
            LOGGER.info("Client does not accept HTML pages: {}", () -> req.getHeader("Accept"));
            resp.sendError(statusCode, message);
        }
    }

    /**
     * Returns true if Accept header allows sending html pages
     */
    private boolean acceptWebPage(HttpServletRequest req) {
        Enumeration<String> acceptHeader = req.getHeaders("Accept");
        if (!acceptHeader.hasMoreElements()) {
            return true;
        }
        while (acceptHeader.hasMoreElements()) {
            String[] acceptValues = acceptHeader.nextElement().split(",");
            for (String acceptValue : acceptValues) {
                String[] parsed = acceptValue.split(";");
                String mediaRange = parsed[0].trim();
                if (mediaRange.startsWith("text/html") || mediaRange.startsWith("text/*")
                    || mediaRange.startsWith("*/")) {
                    float quality = 1f; //default 'q=1.0'
                    for (int i = 1; i < parsed.length; i++) {
                        if (parsed[i].trim().startsWith("q=")) {
                            String qualityValue = parsed[i].trim().substring(2).trim();
                            quality = Float.parseFloat(qualityValue);
                        }
                    }
                    if (quality > 0.5) {
                        //firefox 18 accepts every media type when requesting images with 0.5
                        // but closes stream immediately when detecting text/html
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean setCurrentSession(HttpServletRequest req) {
        MCRSession session = getMCRSession(req);
        if (session != null && !MCRSessionMgr.hasCurrentSession()) {
            MCRSessionMgr.setCurrentSession(session);
        }
        return session != null;
    }

    private MCRSession getMCRSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return MCRServlet.getSession(req);
    }

    private void setWebAppBaseURL(MCRSession session, HttpServletRequest request) {
        if (request.getAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE) != null) {
            session.put(MCRFrontendUtil.BASE_URL_ATTRIBUTE, request.getAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE));
        }
    }

    /**
     * Builds a jdom document containing the error parameter.
     *
     * @param msg the message of the error
     * @param statusCode the http status code
     * @param requestURI the uri of the request
     * @param exceptionType type of the exception
     * @param source source of the error
     * @param ex exception which is occured
     *
     * @return jdom document containing all error parameter
     */
    public static Document buildErrorPage(String msg, Integer statusCode, String requestURI,
        Class<? extends Throwable> exceptionType, String source, Throwable ex) {
        String rootname = MCRConfiguration2.getString("MCR.Frontend.ErrorPage").orElse("mcr_error");
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
        if (source != null) {
            root.setAttribute("source", source);
        }
        Throwable throwableException = ex;
        while (throwableException != null) {
            Element exception = new Element("exception");
            exception.setAttribute("type", throwableException.getClass().getName());
            Element trace = new Element("trace");
            Element message = new Element("message");
            trace.setText(MCRException.getStackTraceAsString(throwableException));
            message.setText(throwableException.getMessage());
            exception.addContent(message).addContent(trace);
            root.addContent(exception);
            throwableException = throwableException.getCause();
        }
        return new Document(root, new DocType(rootname));
    }

    protected void generateErrorPage(HttpServletRequest request, HttpServletResponse response, String msg,
        Throwable ex, Integer statusCode, Class<? extends Throwable> exceptionType, String requestURI,
        String servletName) throws IOException, TransformerException, SAXException {
        boolean exceptionThrown = ex != null;
        LOGGER.log(exceptionThrown ? Level.ERROR : Level.WARN, String.format(Locale.ENGLISH,
            "%s: Error %d occured. The following message was given: %s", requestURI, statusCode, msg), ex);

        String style = MCRFrontendUtil
            .getProperty(request, "XSL.Style")
            .filter("xml"::equals)
            .orElse("default");
        request.setAttribute("XSL.Style", style);

        Document errorDoc = buildErrorPage(msg, statusCode, requestURI, exceptionType, servletName, ex);

        final String requestAttr = "MCRErrorServlet.generateErrorPage";
        if (!response.isCommitted() && request.getAttribute(requestAttr) == null) {
            response.setStatus(Objects.requireNonNullElse(statusCode, HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            request.setAttribute(requestAttr, msg);
            boolean currentSessionActive = MCRSessionMgr.hasCurrentSession();
            boolean sessionFromRequest = setCurrentSession(request);
            MCRSession session = null;
            try {
                MCRSessionMgr.unlock();
                session = MCRSessionMgr.getCurrentSession();
                boolean openTransaction = MCRTransactionManager.hasActiveTransactions();
                if (!openTransaction) {
                    MCRTransactionManager.beginTransactions();
                }
                try {
                    setWebAppBaseURL(session, request);
                    LAYOUT_SERVICE.doLayout(request, response, new MCRJDOMContent(errorDoc));
                } finally {
                    if (!openTransaction) {
                        MCRTransactionManager.commitTransactions();
                    }
                }
            } finally {
                if (exceptionThrown || !currentSessionActive) {
                    MCRSessionMgr.releaseCurrentSession();
                }
                if (!sessionFromRequest) {
                    //new session created for transaction
                    session.close();
                }
            }
        } else {
            if (request.getAttribute(requestAttr) != null) {
                LOGGER.warn("Could not send error page. Generating error page failed. The original message:\n{}",
                    () -> request.getAttribute(requestAttr));
            } else {
                LOGGER.warn(
                    "Could not send error page. Response allready commited. The following message was given:\n{}", msg);
            }
        }
    }
}
