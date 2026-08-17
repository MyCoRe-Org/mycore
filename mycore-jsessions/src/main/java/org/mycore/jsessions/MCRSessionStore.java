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

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionResolver;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.http.HttpSession;

/**
 * A {@link MCRSessionStore} holds {@link HttpSession} instances and provides access to the
 * HTTP sessions as well as to the HTTP sessions along with the corresponding MyCoRe session.
 * <p>
 * It relies on{@link MCRSessionResolver} instances to be stored in each HTTP sessions as
 * an attribute with name {@link MCRServlet#ATTR_MYCORE_SESSION}.
 */
public final class MCRSessionStore {

    private static final String SESSION_INDEX_KEY = MCRSessionStore.class.getName() + ".SESSION_INDEX";

    private final ConcurrentMap<String, HttpSession> httpSessions = new ConcurrentHashMap<>();

    /**
     * Returns all HTTP session IDs.
     */
    public Iterable<String> httpSessionIds() {
        return this.httpSessions.keySet();
    }

    /**
     * Returns all HTTP sessions.
     */
    public Iterable<HttpSession> httpSessions() {
        return this.httpSessions.values();
    }

    /**
     * Returns all HTTP sessions, each along with the corresponding MyCoRe session.
     * Includes only HTTP sessions that are associated with an existing MyCoRe session.
     */
    public Iterable<Sessions> sessions() {
        return httpSessions.values().stream()
            .map(MCRSessionStore::sessionsByHttpSession)
            .flatMap(Optional::stream)
            .toList();
    }

    /**
     * Returns the HTTP session with the given HTTP session ID, if such an HTTP session exists.
     */
    public Optional<HttpSession> httpSessionById(String httpSessionId) {
        return Optional.ofNullable(httpSessions.get(httpSessionId));
    }

    /**
     * Returns the HTTP session  with the given HTTP session ID along with the corresponding MyCoRe session,
     * if such an HTTP session exist and is associated with an existing MyCoRe session.
     */
    public Optional<Sessions> sessionsByHttpSessionId(String httpSessionId) {
        return httpSessionById(httpSessionId).flatMap(MCRSessionStore::sessionsByHttpSession);
    }

    /**
     * Returns a given HTTP session along with the corresponding MyCoRe session,
     * if it is associated with an existing MyCoRe session.
     */
    public static Optional<Sessions> sessionsByHttpSession(HttpSession httpSession) {
        return Optional.ofNullable(httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
            .map(MCRSessionResolver.class::cast)
            .flatMap(MCRSessionResolver::resolveSession)
            .map(session -> new Sessions(httpSession, session));
    }

    /**
     * Returns all HTTP session that are associated with a given MyCoRe session.
     */
    public Set<HttpSession> httpSessionsByMycoreSession(MCRSession mycoreSession) {
        return httpSessions.values().stream()
            .filter(httpSession -> Optional.ofNullable(httpSession
                .getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
                .map(MCRSessionResolver.class::cast)
                .map(MCRSessionResolver::getSessionID)
                .filter(sessionId -> sessionId.equals(mycoreSession.getID()))
                .isPresent())
            .collect(Collectors.toSet());
    }

    void add(HttpSession session) {
        SessionIndex sessionIndex = new SessionIndex();
        sessionIndex.lock.lock();
        try {
            session.setAttribute(SESSION_INDEX_KEY, sessionIndex);
            reindex(session, sessionIndex);
        } finally {
            sessionIndex.lock.unlock();
        }
    }

    /**
     * Reindexes a HTTP session whose ID has been changed by the servlet container, i.e. by
     * {@link jakarta.servlet.http.HttpServletRequest#changeSessionId()}.
     * <p>
     * The session is indexed by its current ID and all other IDs it is indexed by are dropped. This
     * restores the invariant that each HTTP session is indexed by exactly one ID, even if a previous
     * ID change was never reported to this store. The current ID is added before the previous IDs are
     * dropped, so that the session can always be looked up by at least one ID.
     *
     * @param session the HTTP session, already carrying its new ID
     */
    void changeId(HttpSession session) {
        SessionIndex sessionIndex = sessionIndex(session);
        if (sessionIndex == null) {
            return;
        }
        sessionIndex.lock.lock();
        try {
            if (!sessionIndex.removed) {
                reindex(session, sessionIndex);
            }
        } finally {
            sessionIndex.lock.unlock();
        }
    }

    void remove(HttpSession session) {
        SessionIndex sessionIndex = sessionIndex(session);
        if (sessionIndex == null) {
            httpSessions.values().removeIf(value -> value.equals(session));
            return;
        }
        sessionIndex.lock.lock();
        try {
            sessionIndex.removed = true;
            if (sessionIndex.indexedId != null) {
                httpSessions.remove(sessionIndex.indexedId, session);
                sessionIndex.indexedId = null;
            }
        } finally {
            sessionIndex.lock.unlock();
        }
    }

    private void reindex(HttpSession session, SessionIndex sessionIndex) {
        String currentId = session.getId();
        httpSessions.put(currentId, session);
        if (sessionIndex.indexedId != null && !sessionIndex.indexedId.equals(currentId)) {
            httpSessions.remove(sessionIndex.indexedId, session);
        }
        sessionIndex.indexedId = currentId;
    }

    private static SessionIndex sessionIndex(HttpSession session) {
        Object sessionIndex = session.getAttribute(SESSION_INDEX_KEY);
        return sessionIndex instanceof SessionIndex index ? index : null;
    }

    public record Sessions(HttpSession httpSession, MCRSession session) {
    }

    private static final class SessionIndex implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final ReentrantLock lock = new ReentrantLock();

        private String indexedId;

        private boolean removed;

    }

}
