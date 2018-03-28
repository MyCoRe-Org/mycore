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

package org.mycore.common;

import static org.mycore.common.events.MCRSessionEvent.Type.created;
import static org.mycore.common.events.MCRSessionEvent.Type.destroyed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.util.concurrent.MCRReadWriteGuard;

/**
 * Manages sessions for a MyCoRe system. This class is backed by a ThreadLocal variable, so every Thread is guaranteed
 * to get a unique instance of MCRSession. Care must be taken when using an environment utilizing a Thread pool, such as
 * many Servlet engines. In this case it is possible for the session object to stay attached to a thread where it should
 * not be. Use the {@link #releaseCurrentSession()}method to reset the session object for a Thread to its default
 * values. The basic idea for the implementation of this class is taken from an apache project, namely the class
 * org.apache.common.latka.LatkaProperties.java written by Morgan Delagrange.
 * 
 * @author Detlev Degenhardt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRSessionMgr {

    private static Map<String, MCRSession> sessions = Collections.synchronizedMap(new HashMap<String, MCRSession>());

    private static List<MCRSessionListener> listeners = new ArrayList<>();

    private static MCRReadWriteGuard listenersGuard = new MCRReadWriteGuard();

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This ThreadLocal is automatically instantiated per thread with a MyCoRe session object containing the default
     * session parameters which are set in the constructor of MCRSession.
     * 
     * @see ThreadLocal
     */
    private static ThreadLocal<MCRSession> theThreadLocalSession = ThreadLocal.withInitial(MCRSession::new);

    private static ThreadLocal<Boolean> isSessionAttached = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static ThreadLocal<Boolean> isSessionCreationLocked = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    /**
     * This method returns the unique MyCoRe session object for the current Thread. The session object is initialized
     * with the default MyCoRe session data.
     * 
     * @return MyCoRe MCRSession object
     */
    public static MCRSession getCurrentSession() {
        if (isSessionCreationLocked.get()) {
            throw new MCRException("Session creation is locked!");
        }
        isSessionAttached.set(Boolean.TRUE);
        return theThreadLocalSession.get();
    }

    /**
     * This method sets a MyCoRe session object for the current Thread. This method fires a "activated" event, when
     * called the first time for this session and thread.
     * 
     * @see org.mycore.common.events.MCRSessionEvent.Type#activated
     */
    public static void setCurrentSession(MCRSession theSession) {
        if (hasCurrentSession()) {
            MCRSession currentSession = getCurrentSession();
            if (currentSession != theSession && currentSession.getID() != null) {
                LOGGER.error("Current session will be released: " + currentSession.toString(),
                    new MCRException("Current thread already has a session attached!"));
                releaseCurrentSession();
            }

        }
        theSession.activate();
        theThreadLocalSession.set(theSession);
        isSessionAttached.set(Boolean.TRUE);
    }

    /**
     * Releases the MyCoRe session from its current thread. Subsequent calls of getCurrentSession() will return a
     * different MCRSession object than before for the current Thread. One use for this method is to reset the session
     * inside a Thread-pooling environment like Servlet engines. This method fires a "passivated" event, when called the
     * last time for this session and thread.
     * 
     * @see org.mycore.common.events.MCRSessionEvent.Type#passivated
     */
    public static void releaseCurrentSession() {
        //theThreadLocalSession maybe null if called after close()
        if (theThreadLocalSession != null && hasCurrentSession()) {
            MCRSession session = theThreadLocalSession.get();
            session.passivate();
            MCRSession.LOGGER.debug("MCRSession released {}", session.getID());
            theThreadLocalSession.remove();
            isSessionAttached.remove();
        }
    }

    /**
     * Returns a boolean indicating if a {@link MCRSession} is bound to the current thread.
     * 
     * @return true if a session is bound to the current thread
     */
    public static boolean hasCurrentSession() {
        return isSessionAttached.get();
    }

    /**
     * Returns the current session ID. This method does not spawn a new session as {@link #getCurrentSession()} would
     * do.
     * 
     * @return current session ID or <code>null</code> if current thread has no session attached.
     */
    public static String getCurrentSessionID() {
        if (hasCurrentSession()) {
            return getCurrentSession().getID();
        }
        return null;
    }

    public static void lock() {
        isSessionCreationLocked.set(true);
    }

    public static void unlock() {
        isSessionCreationLocked.set(false);
    }

    /**
     * Returns the MCRSession for the given sessionID.
     */
    public static MCRSession getSession(String sessionID) {
        MCRSession s = sessions.get(sessionID);
        if (s == null) {
            MCRSession.LOGGER.warn("MCRSession with ID {} not cached any more", sessionID);
        }
        return s;
    }

    /**
     * Add MCRSession to a static Map that manages all sessions. This method fires a "created" event and is invoked by
     * MCRSession constructor.
     * 
     * @see MCRSession#MCRSession()
     * @see org.mycore.common.events.MCRSessionEvent.Type#created
     */
    static void addSession(MCRSession session) {
        sessions.put(session.getID(), session);
        session.fireSessionEvent(created, session.concurrentAccess.get());
    }

    /**
     * Remove MCRSession from a static Map that manages all sessions. This method fires a "destroyed" event and is
     * invoked by MCRSession.close().
     * 
     * @see MCRSession#close()
     * @see org.mycore.common.events.MCRSessionEvent.Type#destroyed
     */
    static void removeSession(MCRSession session) {
        sessions.remove(session.getID());
        session.fireSessionEvent(destroyed, session.concurrentAccess.get());
    }

    /**
     * Add a MCRSessionListener, that gets infomed about MCRSessionEvents.
     * 
     * @see #removeSessionListener(MCRSessionListener)
     */
    public static void addSessionListener(MCRSessionListener listener) {
        listenersGuard.write(() -> listeners.add(listener));
    }

    /**
     * Removes a MCRSessionListener from the list.
     * 
     * @see #addSessionListener(MCRSessionListener)
     */
    public static void removeSessionListener(MCRSessionListener listener) {
        listenersGuard.write(() -> listeners.remove(listener));
    }

    /**
     * Allows access to all MCRSessionListener instances. Mainly for internal use of MCRSession.
     */
    static List<MCRSessionListener> getListeners() {
        return listenersGuard.read(() -> listeners.stream().collect(Collectors.toList()));
    }

    public static void close() {
        listenersGuard.write(() -> {
            Collection<MCRSession> var = sessions.values();
            for (MCRSession session : var.toArray(new MCRSession[var.size()])) {
                session.close();
            }
            LogManager.getLogger(MCRSessionMgr.class).info("Removing thread locals...");
            isSessionAttached = null;
            theThreadLocalSession = null;
            listeners.clear();
            LogManager.getLogger(MCRSessionMgr.class).info("...done.");
        });
        listenersGuard = null;
    }

    public static Map<String, MCRSession> getAllSessions() {
        return sessions;
    }

}
