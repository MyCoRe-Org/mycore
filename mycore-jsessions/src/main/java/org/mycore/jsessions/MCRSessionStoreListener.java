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

package org.mycore.jsessions;

import java.util.Optional;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * A {@link MCRSessionStoreListener} is a {@link ServletContextListener} and {@link HttpSessionListener}
 * that, if registered, keeps track of all HTTP session known to the MyCoRe application.
 * It provides access to the stored HTTP sessions with
 * {@link MCRSessionStoreListener#getSessionStore(ServletContext)}.
 * <p>
 * <strong>Limitation:</strong> {@link HttpSessionListener#sessionCreated(HttpSessionEvent)} is only called
 * when a session is actually created. When a session is recreated after a restart of the web container,
 * this method is not called again. The restarted MyCoRe application will not know about such HTTP sessions.
 * In a MyCoRe application, this is typically not a problem, because MyCoRe sessions aren't persisted
 * across restarts.
 */
public class MCRSessionStoreListener implements ServletContextListener, HttpSessionListener {

    private static final String SESSION_STORE_KEY = "MCR_HTTP_SESSION_STORE";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(SESSION_STORE_KEY, new MCRSessionStore());
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        getSessionStore(session.getServletContext()).ifPresent(sessionStore -> sessionStore.add(session));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        getSessionStore(session.getServletContext()).ifPresent(sessionStore -> sessionStore.remove(session));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(SESSION_STORE_KEY);
    }

    public static Optional<MCRSessionStore> getSessionStore(ServletContext context) {
        return Optional.ofNullable((MCRSessionStore) context.getAttribute(SESSION_STORE_KEY));
    }

}
