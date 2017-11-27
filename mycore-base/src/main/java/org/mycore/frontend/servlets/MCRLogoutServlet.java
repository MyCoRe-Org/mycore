/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Invalidates a session and sends redirect to referring page.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRLogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String LOGOUT_REDIRECT_URL_PARAMETER = "url";

    private static final Logger LOGGER = LogManager.getLogger(MCRLogoutServlet.class);

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getUserPrincipal() != null) {
            req.logout();
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            LOGGER.debug("Invalidate HTTP-Session: {}", session.getId());
            session.invalidate();
        }
        String returnURL = getReturnURL(req);
        LOGGER.debug("Redirect to: {}", returnURL);
        resp.sendRedirect(returnURL);
    }

    static String getReturnURL(HttpServletRequest req) {
        String returnURL = req.getParameter(LOGOUT_REDIRECT_URL_PARAMETER);
        if (returnURL == null) {
            String referer = req.getHeader("Referer");
            returnURL = (referer != null) ? referer : req.getContextPath() + "/";
        }
        return returnURL;
    }

}
