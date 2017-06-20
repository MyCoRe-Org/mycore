package org.mycore.sword.servlets;

import java.text.MessageFormat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordServlet extends HttpServlet {
    private static Logger LOGGER = LogManager.getLogger(MCRSwordServlet.class);

    protected void prepareRequest(HttpServletRequest req, HttpServletResponse resp) {
        if (req.getAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE) == null) {
            String webappBase = MCRFrontendUtil.getBaseURL(req);
            req.setAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE, webappBase);
        }
        MCRSession session = MCRServlet.getSession(req);
        MCRSessionMgr.setCurrentSession(session);
        LOGGER.info(MessageFormat.format("{0} ip={1} mcr={2} user={3}", req.getPathInfo(),
            MCRFrontendUtil.getRemoteAddr(req), session.getID(), session.getUserInformation().getUserID()));
        MCRFrontendUtil.configureSession(session, req, resp);
        MCRSessionMgr.getCurrentSession().beginTransaction();
    }

    protected void afterRequest(HttpServletRequest req, HttpServletResponse resp) {
        MCRSessionMgr.getCurrentSession().commitTransaction();
        MCRSessionMgr.releaseCurrentSession();
    }

}
