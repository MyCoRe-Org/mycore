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

package org.mycore.jsession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionResolver;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.jsessions.MCRSessionStore;
import org.mycore.jsessions.MCRSessionStore.Sessions;
import org.mycore.jsessions.MCRSessionStoreListener;
import org.mycore.test.MyCoReTest;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

@MyCoReTest
public class MCRSessionStoreListenerTest {

    private MCRSessionStoreListener listener;

    private ServletContext servletContext;

    private MCRSessionStore sessionStore;

    @BeforeEach
    public void setUp() {

        // create listener
        listener = new MCRSessionStoreListener();

        // fire initialized event
        servletContext = getMockServletContext();
        listener.contextInitialized(new ServletContextEvent(servletContext));

        // retrieve session store
        Optional<MCRSessionStore> sessionStoreOptional = MCRSessionStoreListener.getSessionStore(servletContext);
        assertTrue(sessionStoreOptional.isPresent());
        sessionStore = sessionStoreOptional.get();

    }

    @Test
    public void trackHttpSessions() {

        // create HTTP sessions
        HttpSession httpSession1 = getMockHttpSession(servletContext, "httpSession1");
        HttpSession httpSession2 = getMockHttpSession(servletContext, "httpSession2");
        HttpSession httpSession3 = getMockHttpSession(servletContext, "httpSession3");

        // fire session created events
        listener.sessionCreated(new HttpSessionEvent(httpSession1));
        listener.sessionCreated(new HttpSessionEvent(httpSession2));
        listener.sessionCreated(new HttpSessionEvent(httpSession3));

        // all HTTP session IDs should be there
        assertEquals(Set.of("httpSession1", "httpSession2", "httpSession3"), obtainHttpSessionIdsAsSet(sessionStore));

        // all HTTP session should be there
        assertEquals(Set.of(httpSession1, httpSession2, httpSession3), obtainHttpSessionsAsSet(sessionStore));

        // all HTTP sessions should be retrievable
        assertEquals(Optional.of(httpSession1), sessionStore.httpSessionById("httpSession1"));
        assertEquals(Optional.of(httpSession2), sessionStore.httpSessionById("httpSession2"));
        assertEquals(Optional.of(httpSession3), sessionStore.httpSessionById("httpSession3"));
        assertEquals(Optional.empty(), sessionStore.httpSessionById("httpSession4"));

        // fire session destroyed events
        listener.sessionDestroyed(new HttpSessionEvent(httpSession3));
        listener.sessionDestroyed(new HttpSessionEvent(httpSession1));
        listener.sessionDestroyed(new HttpSessionEvent(httpSession2));

        // no session IDs should be there anymore
        assertTrue(obtainHttpSessionIdsAsSet(sessionStore).isEmpty());

        // no sessions should be there anymore
        assertTrue(obtainHttpSessionsAsSet(sessionStore).isEmpty());

    }

    @Test
    public void trackHttpSessionsWithCorrespondingMyCoReSessions() {

        // create sessions
        MCRSession sessionA = getMockSession("sessionA");
        MCRSession sessionB = getMockSession("sessionB");

        // create HTTP sessions
        HttpSession httpSession1 = getMockHttpSession(servletContext, "httpSession1", sessionA);
        HttpSession httpSession2 = getMockHttpSession(servletContext, "httpSession2", sessionB);
        HttpSession httpSession3 = getMockHttpSession(servletContext, "httpSession3");
        HttpSession httpSession4 = getMockHttpSession(servletContext, "httpSession4", sessionA);

        // create available combinations
        Sessions sessions1A = new Sessions(httpSession1, sessionA);
        Sessions sessions2B = new Sessions(httpSession2, sessionB);
        Sessions sessions4A = new Sessions(httpSession4, sessionA);

        // fire session created events
        listener.sessionCreated(new HttpSessionEvent(httpSession1));
        listener.sessionCreated(new HttpSessionEvent(httpSession2));
        listener.sessionCreated(new HttpSessionEvent(httpSession3));
        listener.sessionCreated(new HttpSessionEvent(httpSession4));

        // all HTTP session should be there, with corresponding MyCoRe session
        assertEquals(Set.of(sessions1A, sessions2B, sessions4A), obtainSessionsAsSet(sessionStore));

        // all HTTP sessions should be there, if they have a corresponding MyCoRe sessions
        assertEquals(Optional.of(sessions1A), sessionStore.sessionsByHttpSessionId("httpSession1"));
        assertEquals(Optional.of(sessions2B), sessionStore.sessionsByHttpSessionId("httpSession2"));
        assertEquals(Optional.empty(), sessionStore.sessionsByHttpSessionId("httpSession3"));
        assertEquals(Optional.of(sessions4A), sessionStore.sessionsByHttpSessionId("httpSession4"));
        assertEquals(Optional.empty(), sessionStore.sessionsByHttpSessionId("httpSession5"));

        // all HTTP session should be there for their corresponding MyCoRe session
        assertEquals(Set.of(httpSession1, httpSession4), sessionStore.httpSessionsByMycoreSession(sessionA));
        assertEquals(Set.of(httpSession2), sessionStore.httpSessionsByMycoreSession(sessionB));

    }

    private static Set<String> obtainHttpSessionIdsAsSet(MCRSessionStore sessionStore) {
        Set<String> httpSessionIds = new HashSet<>();
        sessionStore.httpSessionIds().forEach(httpSessionIds::add);
        return httpSessionIds;
    }

    private static Set<HttpSession> obtainHttpSessionsAsSet(MCRSessionStore sessionStore) {
        Set<HttpSession> httpSessions = new HashSet<>();
        sessionStore.httpSessions().forEach(httpSessions::add);
        return httpSessions;
    }

    private static Set<Sessions> obtainSessionsAsSet(MCRSessionStore sessionStore) {
        Set<Sessions> sessions = new HashSet<>();
        sessionStore.sessions().forEach(sessions::add);
        return sessions;
    }

    private static ServletContext getMockServletContext() {
        Map<String, Object> attributes = new HashMap<>();
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(servletContext).setAttribute(Mockito.anyString(), Mockito.any());
        Mockito.doAnswer(invocation -> attributes.get((String) invocation.getArgument(0))).when(servletContext)
            .getAttribute(Mockito.anyString());
        Mockito.doAnswer(invocation -> attributes.remove((String) invocation.getArgument(0))).when(servletContext)
            .removeAttribute(Mockito.anyString());
        return servletContext;
    }

    private static HttpSession getMockHttpSession(ServletContext servletContext, String httpSessionId) {
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getServletContext()).thenReturn(servletContext);
        Mockito.when(httpSession.getId()).thenReturn(httpSessionId);
        return httpSession;
    }

    private static HttpSession getMockHttpSession(ServletContext servletContext, String httpSessionId,
        MCRSession session) {
        String sessionId = session.getID();
        MCRSessionResolver sessionResolver = Mockito.mock(MCRSessionResolver.class);
        Mockito.when(sessionResolver.getSessionID()).thenReturn(sessionId);
        Mockito.when(sessionResolver.resolveSession()).thenReturn(Optional.of(session));
        HttpSession httpSession = getMockHttpSession(servletContext, httpSessionId);
        Mockito.when(httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION)).thenReturn(sessionResolver);
        return httpSession;
    }

    private MCRSession getMockSession(String sessionId) {
        MCRSession session = Mockito.mock(MCRSession.class);
        Mockito.when(session.getID()).thenReturn(sessionId);
        return session;
    }

}
