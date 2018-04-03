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

package org.mycore.frontend.ws.endoint;

import java.util.function.Supplier;

import javax.websocket.Session;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSessionResolver;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Starting class for all mycore websocket endpoints.
 * 
 * @author Michel Buechner (mcrmibue)
 * @author Matthias Eichner
 */
public abstract class MCRAbstractEndpoint {

    /**
     * Encapsulates a supplier with a mycore session.
     * 
     * @param session the websocket session
     * @param supplier the supplier
     * @return the result of the supplier
     */
    protected <T> T sessionized(Session session, Supplier<T> supplier) {
        activate(session);
        try {
            return supplier.get();
        } finally {
            passivate(session);
        }
    }

    /**
     * Encapsulates a function with a mycore session.
     * 
     * @param session the websocket session
     * @param runnable the runnable
     */
    protected void sessionized(Session session, Runnable runnable) {
        activate(session);
        try {
            runnable.run();
        } finally {
            passivate(session);
        }
    }

    /**
     * Retrieves the mycore session id from the websocket session and binds
     * the current thread with this mycore session.
     * 
     * @param session the websocket session
     */
    protected void activate(Session session) {
        MCRSession mcrSession = ((MCRSessionResolver) session.getUserProperties().get(MCRServlet.ATTR_MYCORE_SESSION))
            .resolveSession().get();
        MCRSessionMgr.setCurrentSession(mcrSession);
    }

    /**
     * Releases the mycore session from this thread.
     * 
     * @param session the websocket session
     */
    protected void passivate(Session session) {
        MCRSessionMgr.releaseCurrentSession();
    }

}
