/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.common.events;

import java.util.Enumeration;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Handles different HttpSession events.
 * 
 * This class is used to free up MCRSessions when their associated HttpSession
 * is destroyed or a new MCRSession replaces an old one.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRHttpSessionListener implements HttpSessionListener, HttpSessionBindingListener {
    Logger LOGGER = LogManager.getLogger();

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
     */
    public void sessionCreated(HttpSessionEvent hse) {
        LOGGER.debug(() -> "HttpSession " + hse.getSession().getId() + " is beeing created by: " + hse.getSource());
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
     */
    public void sessionDestroyed(HttpSessionEvent hse) {
        // clear MCRSessions
        HttpSession httpSession = hse.getSession();
        LOGGER.debug(() -> "HttpSession " + httpSession.getId() + " is beeing destroyed by " + hse.getSource()
            + ", clearing up.");
        LOGGER.debug("Removing any MCRSessions from HttpSession");
        for (Enumeration<String> e = httpSession.getAttributeNames(); e.hasMoreElements();) {
            String key = e.nextElement();
            if (key.equals(MCRServlet.ATTR_MYCORE_SESSION)) {
                MCRSession mcrSession = MCRSessionMgr.getSession((String) httpSession.getAttribute(key));
                if (mcrSession != null) {
                    mcrSession.close();
                }
                // remove reference in httpSession
                httpSession.removeAttribute(key);
            }
        }
        LOGGER.debug("Clearing up done");
    }

    public void valueBound(HttpSessionBindingEvent hsbe) {
    }

    public void valueUnbound(HttpSessionBindingEvent hsbe) {
        LOGGER.debug("Attribute " + hsbe.getName() + " is beeing unbound from session");
        Object obj = hsbe.getValue();
        if (obj instanceof MCRSession) {
            MCRSession mcrSession = (MCRSession) obj;
            mcrSession.close();
        }
        LOGGER.debug("Attribute " + hsbe.getName() + " is unbounded from session");
    }

}
