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

package org.mycore.sword.servlets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Serial;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    protected void prepareRequest(HttpServletRequest req, HttpServletResponse resp) {
        if (req.getAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE) == null) {
            String webappBase = MCRFrontendUtil.getBaseURL(req);
            req.setAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE, webappBase);
        }
        MCRSession session = MCRServlet.getSession(req);
        MCRSessionMgr.setCurrentSession(session);
        LOGGER.info("{} ip={} mcr={} user={}",
            req::getPathInfo, () -> MCRFrontendUtil.getRemoteAddr(req),
            session::getID, () -> session.getUserInformation().getUserID());
        MCRFrontendUtil.configureSession(session, req, resp);
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.beginTransactions();
    }

    protected void afterRequest(HttpServletRequest req, HttpServletResponse resp) {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.commitTransactions();
        MCRSessionMgr.releaseCurrentSession();
    }

}
