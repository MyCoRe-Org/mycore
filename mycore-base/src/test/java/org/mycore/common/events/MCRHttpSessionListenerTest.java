/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionResolver;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.test.MyCoReTest;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

@MyCoReTest
public class MCRHttpSessionListenerTest {

    private MCRHttpSessionListener listener;

    private MCRSession session;

    @BeforeEach
    public void setUp() {
        listener = new MCRHttpSessionListener();
        session = getMockSession();
    }

    @Test
    public void sessionIdChangedUpdatesMyCoReSession() {

        HttpSession httpSession = getMockHttpSession("httpSession1", session);
        session.put(MCRServlet.HTTP_SESSION_ID_KEY, "httpSession1");

        // rotate the HTTP session ID, as done after a successful login
        changeMockHttpSessionId(httpSession, "httpSession2");
        listener.sessionIdChanged(new HttpSessionEvent(httpSession), "httpSession1");

        assertEquals("httpSession2", session.get(MCRServlet.HTTP_SESSION_ID_KEY));

        // rotate again
        changeMockHttpSessionId(httpSession, "httpSession3");
        listener.sessionIdChanged(new HttpSessionEvent(httpSession), "httpSession2");

        assertEquals("httpSession3", session.get(MCRServlet.HTTP_SESSION_ID_KEY));

    }

    @Test
    public void sessionIdChangedKeepsAbsentValueAbsent() {

        // MCRServlet uses the absence of the value to detect the first request of a session,
        // so the listener must not introduce it
        HttpSession httpSession = getMockHttpSession("httpSession1", session);

        changeMockHttpSessionId(httpSession, "httpSession2");
        listener.sessionIdChanged(new HttpSessionEvent(httpSession), "httpSession1");

        assertNull(session.get(MCRServlet.HTTP_SESSION_ID_KEY));

    }

    @Test
    public void sessionIdChangedWithoutMyCoReSession() {

        // no MCRSession is associated with the HTTP session, nothing to do
        HttpSession httpSession = getMockHttpSession("httpSession1");

        changeMockHttpSessionId(httpSession, "httpSession2");
        listener.sessionIdChanged(new HttpSessionEvent(httpSession), "httpSession1");

    }

    private static HttpSession getMockHttpSession(String httpSessionId) {
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getId()).thenReturn(httpSessionId);
        return httpSession;
    }

    private static HttpSession getMockHttpSession(String httpSessionId, MCRSession session) {
        String sessionId = session.getID();
        MCRSessionResolver sessionResolver = Mockito.mock(MCRSessionResolver.class);
        Mockito.when(sessionResolver.getSessionID()).thenReturn(sessionId);
        Mockito.when(sessionResolver.resolveSession()).thenReturn(Optional.of(session));
        HttpSession httpSession = getMockHttpSession(httpSessionId);
        Mockito.when(httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION)).thenReturn(sessionResolver);
        return httpSession;
    }

    private static void changeMockHttpSessionId(HttpSession httpSession, String httpSessionId) {
        Mockito.when(httpSession.getId()).thenReturn(httpSessionId);
    }

    private static MCRSession getMockSession() {
        Map<Object, Object> values = new HashMap<>();
        MCRSession session = Mockito.mock(MCRSession.class);
        Mockito.when(session.getID()).thenReturn("session");
        Mockito.doAnswer(invocation -> values.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(session).put(Mockito.any(), Mockito.any());
        Mockito.doAnswer(invocation -> values.get(invocation.getArgument(0)))
            .when(session).get(Mockito.any());
        return session;
    }

}
