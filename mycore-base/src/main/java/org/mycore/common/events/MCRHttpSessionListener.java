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

package org.mycore.common.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionResolver;
import org.mycore.frontend.servlets.MCRServlet;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Optional;

/**
 * Handles different HttpSession events.
 *
 * This class is used to free up MCRSessions when their associated HttpSession
 * is destroyed or a new MCRSession replaces an old one.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRHttpSessionListener implements HttpSessionListener {

    private final static Logger LOGGER = LogManager.getLogger();

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
     */
    public void sessionCreated(HttpSessionEvent hse) {
        LOGGER.debug(() -> "HttpSession " + hse.getSession().getId() + " is being created by: " + hse.getSource());
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
     */
    public void sessionDestroyed(HttpSessionEvent hse) {
        // clear MCRSessions
        HttpSession httpSession = hse.getSession();
        LOGGER.debug(() -> "HttpSession " + httpSession.getId() + " is being destroyed by " + hse.getSource()
            + ", clearing up.");
        LOGGER.debug("Removing any MCRSessions from HttpSession");
        Optional.ofNullable(httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
            .map(MCRSessionResolver.class::cast).flatMap(MCRSessionResolver::resolveSession)
            .ifPresent(MCRSession::close);
        httpSession.removeAttribute(MCRServlet.ATTR_MYCORE_SESSION);
        LOGGER.debug("Clearing up done");
    }

}
