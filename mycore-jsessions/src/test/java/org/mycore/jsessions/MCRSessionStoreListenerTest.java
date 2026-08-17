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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionResolver;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.jsessions.MCRSessionStore.Sessions;
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

    @Test
    public void trackHttpSessionsWithChangedIds() {

        // create sessions
        MCRSession sessionA = getMockSession("sessionA");

        // create HTTP sessions
        HttpSession httpSession1 = getMockHttpSession(servletContext, "httpSession1", sessionA);
        HttpSession httpSession2 = getMockHttpSession(servletContext, "httpSession2");

        // fire session created events
        listener.sessionCreated(new HttpSessionEvent(httpSession1));
        listener.sessionCreated(new HttpSessionEvent(httpSession2));

        // change the ID of the first HTTP session, as done after a successful login
        changeMockHttpSessionId(httpSession1, "httpSession3");
        listener.sessionIdChanged(new HttpSessionEvent(httpSession1), "httpSession1");

        // the HTTP session should be indexed by its new ID only
        assertEquals(Set.of("httpSession3", "httpSession2"), obtainHttpSessionIdsAsSet(sessionStore));
        assertEquals(Optional.of(httpSession1), sessionStore.httpSessionById("httpSession3"));
        assertEquals(Optional.empty(), sessionStore.httpSessionById("httpSession1"));

        // the corresponding MyCoRe session should be retrievable by the new ID only
        assertEquals(Optional.of(new Sessions(httpSession1, sessionA)),
            sessionStore.sessionsByHttpSessionId("httpSession3"));
        assertEquals(Optional.empty(), sessionStore.sessionsByHttpSessionId("httpSession1"));
        assertEquals(Set.of(httpSession1), sessionStore.httpSessionsByMycoreSession(sessionA));

        // change the ID again
        changeMockHttpSessionId(httpSession1, "httpSession4");
        listener.sessionIdChanged(new HttpSessionEvent(httpSession1), "httpSession3");

        assertEquals(Set.of("httpSession4", "httpSession2"), obtainHttpSessionIdsAsSet(sessionStore));
        assertEquals(Optional.of(httpSession1), sessionStore.httpSessionById("httpSession4"));
        assertEquals(Optional.empty(), sessionStore.httpSessionById("httpSession3"));

        // fire session destroyed events
        listener.sessionDestroyed(new HttpSessionEvent(httpSession1));
        listener.sessionDestroyed(new HttpSessionEvent(httpSession2));

        // no sessions should be there anymore
        assertTrue(obtainHttpSessionIdsAsSet(sessionStore).isEmpty());
        assertTrue(obtainHttpSessionsAsSet(sessionStore).isEmpty());

    }

    @Test
    public void removesHttpSessionAfterUnreportedIdChange() {

        // create HTTP session
        HttpSession httpSession1 = getMockHttpSession(servletContext, "httpSession1");

        // fire session created event
        listener.sessionCreated(new HttpSessionEvent(httpSession1));

        // change the ID without a corresponding event
        changeMockHttpSessionId(httpSession1, "httpSession2");

        // fire session destroyed event
        listener.sessionDestroyed(new HttpSessionEvent(httpSession1));

        // no stale entry should be left behind
        assertTrue(obtainHttpSessionIdsAsSet(sessionStore).isEmpty());
        assertTrue(obtainHttpSessionsAsSet(sessionStore).isEmpty());

    }

    @Test
    public void indexesHttpSessionByOneIdAfterUnreportedIdChange() {

        // create HTTP session
        HttpSession httpSession1 = getMockHttpSession(servletContext, "httpSession1");

        // fire session created event
        listener.sessionCreated(new HttpSessionEvent(httpSession1));

        // change the ID without a corresponding event
        changeMockHttpSessionId(httpSession1, "httpSession2");

        // change the ID again, this time with a corresponding event
        changeMockHttpSessionId(httpSession1, "httpSession3");
        listener.sessionIdChanged(new HttpSessionEvent(httpSession1), "httpSession2");

        // the HTTP session should be indexed by its current ID only, the unreported ID
        // must not be left behind as an alias
        assertEquals(Set.of("httpSession3"), obtainHttpSessionIdsAsSet(sessionStore));

        // fire session destroyed event
        listener.sessionDestroyed(new HttpSessionEvent(httpSession1));

        // no stale entry should be left behind
        assertTrue(obtainHttpSessionIdsAsSet(sessionStore).isEmpty());
        assertTrue(obtainHttpSessionsAsSet(sessionStore).isEmpty());

    }

    @Test
    public void createsHttpSessionsConcurrently()
        throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch firstCreationStarted = new CountDownLatch(1);
        CountDownLatch continueFirstCreation = new CountDownLatch(1);
        HttpSession httpSession1 = getMockHttpSession(servletContext, "httpSession1");
        Mockito.when(httpSession1.getId()).thenAnswer(invocation -> {
            firstCreationStarted.countDown();
            assertTrue(continueFirstCreation.await(5, TimeUnit.SECONDS));
            return "httpSession1";
        });
        HttpSession httpSession2 = getMockHttpSession(servletContext, "httpSession2");

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> firstCreation = executor.submit(
                () -> listener.sessionCreated(new HttpSessionEvent(httpSession1)));
            try {
                assertTrue(firstCreationStarted.await(5, TimeUnit.SECONDS));

                Future<?> secondCreation = executor.submit(
                    () -> listener.sessionCreated(new HttpSessionEvent(httpSession2)));
                secondCreation.get(5, TimeUnit.SECONDS);
            } finally {
                continueFirstCreation.countDown();
            }
            firstCreation.get(5, TimeUnit.SECONDS);
        }

        assertEquals(Set.of("httpSession1", "httpSession2"), obtainHttpSessionIdsAsSet(sessionStore));
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
        Map<String, Object> attributes = new HashMap<>();
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getServletContext()).thenReturn(servletContext);
        Mockito.when(httpSession.getId()).thenReturn(httpSessionId);
        Mockito.doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(httpSession).setAttribute(Mockito.anyString(), Mockito.any());
        Mockito.doAnswer(invocation -> attributes.get((String) invocation.getArgument(0))).when(httpSession)
            .getAttribute(Mockito.anyString());
        Mockito.doAnswer(invocation -> attributes.remove((String) invocation.getArgument(0))).when(httpSession)
            .removeAttribute(Mockito.anyString());
        return httpSession;
    }

    private static HttpSession getMockHttpSession(ServletContext servletContext, String httpSessionId,
        MCRSession session) {
        String sessionId = session.getID();
        MCRSessionResolver sessionResolver = Mockito.mock(MCRSessionResolver.class);
        Mockito.when(sessionResolver.getSessionID()).thenReturn(sessionId);
        Mockito.when(sessionResolver.resolveSession()).thenReturn(Optional.of(session));
        HttpSession httpSession = getMockHttpSession(servletContext, httpSessionId);
        httpSession.setAttribute(MCRServlet.ATTR_MYCORE_SESSION, sessionResolver);
        return httpSession;
    }

    private static void changeMockHttpSessionId(HttpSession httpSession, String httpSessionId) {
        Mockito.when(httpSession.getId()).thenReturn(httpSessionId);
    }

    private MCRSession getMockSession(String sessionId) {
        MCRSession session = Mockito.mock(MCRSession.class);
        Mockito.when(session.getID()).thenReturn(sessionId);
        return session;
    }

}
