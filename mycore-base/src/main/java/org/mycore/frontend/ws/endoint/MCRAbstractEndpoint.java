package org.mycore.frontend.ws.endoint;

import java.util.function.Supplier;

import javax.websocket.Session;

import org.mycore.common.MCRSessionMgr;
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
        String sessionId = (String) session.getUserProperties().get(MCRServlet.ATTR_MYCORE_SESSION);
        MCRSessionMgr.setCurrentSession(MCRSessionMgr.getSession(sessionId));
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
