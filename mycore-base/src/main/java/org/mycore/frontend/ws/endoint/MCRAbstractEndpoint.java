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

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSessionResolver;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.websocket.Session;

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
        try {
            activate(session);
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
        MCRSessionMgr.unlock();
        String mcrSessionKey = MCRServlet.ATTR_MYCORE_SESSION;
        Optional<MCRSession> optionalSession = ((MCRSessionResolver) session.getUserProperties()
            .get(mcrSessionKey))
                .resolveSession();
        if (optionalSession.isPresent()) {
            MCRSession mcrSession = optionalSession.get();
            MCRSessionMgr.setCurrentSession(mcrSession);
        } else {
            throw new MCRException(
                String.format(Locale.ROOT,
                    "Unable to retrieve the mycore session of websocket %s. The mycore session should be bound to the"
                        + " property key %s.",
                    session.getId(), mcrSessionKey));
        }
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
