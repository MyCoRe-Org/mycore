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

package org.mycore.restapi;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MyCoReTest
public class MCRSessionFilterTest {

    private MCRSessionFilter filter;
    private ContainerRequestContext requestContext;
    private HttpServletRequest servletRequest;
    private MCRSession session;
    private MCRUser user;

    private MockedStatic<MCRSessionMgr> sessionManager;
    private MockedStatic<MCRTransactionManager> transactionManager;
    private MockedStatic<MCRFrontendUtil> frontendUtil;
    private MockedStatic<MCRUserManager> userManager;

    @BeforeEach
    void setUp() {
        filter = new MCRSessionFilter();

        filter.app = mock(Application.class);
        requestContext = mock(ContainerRequestContext.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        servletRequest = mock(HttpServletRequest.class);
        session = mock(MCRSession.class);
        user = mock(MCRUser.class);

        filter.httpServletRequest = servletRequest;

        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(false);
        when(session.getUserInformation()).thenReturn(user);

        sessionManager = mockStatic(MCRSessionMgr.class);
        transactionManager = mockStatic(MCRTransactionManager.class);
        frontendUtil = mockStatic(MCRFrontendUtil.class);
        userManager = mockStatic(MCRUserManager.class);

        sessionManager.when(MCRSessionMgr::hasCurrentSession)
            .thenReturn(false);
        sessionManager.when(MCRSessionMgr::getCurrentSession)
            .thenReturn(session);

        frontendUtil.when(() -> MCRFrontendUtil.getRemoteAddr(servletRequest))
            .thenReturn("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        userManager.close();
        frontendUtil.close();
        transactionManager.close();
        sessionManager.close();
    }

    @Test
    void testBasicAuthenticationFullUserAndPw() {
        setBasicAuthorization("alice", "password");

        userManager.when(() -> MCRUserManager.checkPassword("alice", "password"))
            .thenReturn(user);

        assertDoesNotThrow(() -> filter.filter(requestContext));

        userManager.verify(() -> MCRUserManager.checkPassword("alice", "password"));
        verify(session).setUserInformation(user);
        verify(requestContext).setSecurityContext(any(SecurityContext.class));
    }

    @Test
    void testBasicAuthenticationNoPw() {
        setBasicAuthorization("alice", "");

        userManager.when(() -> MCRUserManager.checkPassword("alice", ""))
            .thenReturn(user);

        assertThrows(NotAuthorizedException.class, () -> filter.filter(requestContext));

        userManager.verify(() -> MCRUserManager.checkPassword("alice", ""), never());
        verify(session, never()).setUserInformation(user);
        verify(requestContext, never()).setSecurityContext(any(SecurityContext.class));
    }

    @Test
    void testBasicAuthenticationNoUser() {
        setBasicAuthorization("", "password");

        userManager.when(() -> MCRUserManager.checkPassword("", "password"))
            .thenReturn(user);

        assertThrows(NotAuthorizedException.class, () -> filter.filter(requestContext));

        userManager.verify(() -> MCRUserManager.checkPassword("", "password"), never());
        verify(session, never()).setUserInformation(user);
        verify(requestContext, never()).setSecurityContext(any(SecurityContext.class));
    }

    @Test
    void testBasicAuthenticationNoUserNoPW() {
        setBasicAuthorization("", "");

        userManager.when(() -> MCRUserManager.checkPassword("", ""))
            .thenReturn(user);

        assertThrows(NotAuthorizedException.class, () -> filter.filter(requestContext));

        userManager.verify(() -> MCRUserManager.checkPassword("", ""), never());
        verify(session, never()).setUserInformation(user);
        verify(requestContext, never()).setSecurityContext(any(SecurityContext.class));
    }

    private void setBasicAuthorization(String username, String password) {
        String credentials = username + ":" + password;

        String authorization = "Basic " + Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.ISO_8859_1));

        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
            .thenReturn(authorization);
    }
}
