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

package org.mycore.user.restapi.v2.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.test.MyCoReTest;
import org.mycore.user.restapi.exception.MCRUserNoLocalPasswordException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;
import org.mycore.user.restapi.exception.MCRUserWrongPasswordException;
import org.mycore.user.restapi.v2.MCRUserService;
import org.mycore.user.restapi.v2.dto.MCRChangePasswordRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserProfileRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@MyCoReTest
@ExtendWith(MockitoExtension.class)
public class MCRCurrentUserTest {

    @Mock
    private MCRUserService userService;

    @Mock
    private ContainerRequestContext request;

    private MCRCurrentUser resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new MCRCurrentUser(userService);
        setField(resource, "request", request);

        // default detail level
        lenient().when(request.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    void getCurrentUserShouldCallGetStandardWhenNoDetailLevel() {
        MCRUserStandard standard = new MCRUserStandard("alice", null, null, false, null, List.of(), null, Map.of());
        setCurrentUser("alice");
        when(userService.getUserStandard("alice")).thenReturn(standard);

        Response response = resource.getCurrentUser();

        assertEquals(200, response.getStatus());
        verify(userService).getUserStandard("alice");
    }

    @Test
    void getCurrentUserShouldCallGetSummaryWhenDetailLevelIsSummary() {
        MCRUserSummary summary = new MCRUserSummary("alice", null);
        setCurrentUser("alice");
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(buildMediaType("SUMMARY")));
        when(userService.getUserSummary("alice")).thenReturn(summary);

        Response response = resource.getCurrentUser();

        assertEquals(200, response.getStatus());
        verify(userService).getUserSummary("alice");
    }

    @Test
    void getCurrentUserShouldCallGetDetailWhenDetailLevelIsDetailed() {
        MCRUserDetail detail = new MCRUserDetail(
            "alice", null, null, null, false, null, Map.of(), null, List.of(), List.of()
        );
        setCurrentUser("alice");
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(buildMediaType("DETAILED")));
        when(userService.getUserDetail("alice")).thenReturn(detail);

        Response response = resource.getCurrentUser();

        assertEquals(200, response.getStatus());
        verify(userService).getUserDetail("alice");
    }

    @Test
    void getCurrentUserShouldThrowForbiddenWhenNoLocalUserAccount() {
        setCurrentUser("alice");
        when(userService.getUserStandard("alice")).thenThrow(new MCRUserNotFoundException("alice"));

        assertThrows(ForbiddenException.class, () -> resource.getCurrentUser());
    }

    @Test
    void updateCurrentUserShouldReturn204() {
        MCRUpdateUserProfileRequest dto = new MCRUpdateUserProfileRequest("Alice", "alice@example.com", "hint");
        setCurrentUser("alice");

        Response response = resource.updateCurrentUser(dto);

        assertEquals(204, response.getStatus());
        verify(userService).updateUser("alice", dto);
    }

    @Test
    void updateCurrentUserShouldThrowForbiddenWhenNoLocalUserAccount() {
        MCRUpdateUserProfileRequest dto = new MCRUpdateUserProfileRequest("Alice", "alice@example.com", null);
        setCurrentUser("alice");
        doThrow(new MCRUserNotFoundException("alice")).when(userService).updateUser("alice", dto);

        assertThrows(ForbiddenException.class, () -> resource.updateCurrentUser(dto));
    }

    @Test
    void updateCurrentUserShouldThrowBadRequestWhenEmailInvalid() {
        MCRUpdateUserProfileRequest dto = new MCRUpdateUserProfileRequest("Alice", "not-an-email", null);
        setCurrentUser("alice");
        doThrow(new MCRUserValidationException("email is invalid")).when(userService).updateUser("alice", dto);

        assertThrows(BadRequestException.class, () -> resource.updateCurrentUser(dto));
    }

    @Test
    void changePasswordShouldReturn204ForAuthenticatedUser() {
        MCRChangePasswordRequest dto = new MCRChangePasswordRequest("old-secret", "new-secret");
        setCurrentUser("alice");

        Response response = resource.changePassword(dto);

        assertEquals(204, response.getStatus());
        verify(userService).changePassword("alice", dto);
    }

    @Test
    void changePasswordShouldThrowForbiddenWhenOldPasswordWrong() {
        MCRChangePasswordRequest dto = new MCRChangePasswordRequest("wrong", "new-secret");
        setCurrentUser("alice");
        doThrow(new MCRUserWrongPasswordException("alice"))
            .when(userService).changePassword("alice", dto);

        assertThrows(ForbiddenException.class, () -> resource.changePassword(dto));
    }

    @Test
    void changePasswordShouldThrowForbiddenWhenNoLocalPassword() {
        MCRChangePasswordRequest dto = new MCRChangePasswordRequest("whatever", "new-secret");
        setCurrentUser("alice");
        doThrow(new MCRUserNoLocalPasswordException("alice"))
            .when(userService).changePassword("alice", dto);

        assertThrows(ForbiddenException.class, () -> resource.changePassword(dto));
    }

    @Test
    void changePasswordShouldThrowForbiddenWhenNoLocalUserAccount() {
        MCRChangePasswordRequest dto = new MCRChangePasswordRequest("old-secret", "new-secret");
        setCurrentUser("alice");
        doThrow(new MCRUserNotFoundException("alice"))
            .when(userService).changePassword("alice", dto);

        assertThrows(ForbiddenException.class, () -> resource.changePassword(dto));
    }

    @Test
    void changePasswordShouldThrowBadRequestWhenNewPasswordInvalid() {
        MCRChangePasswordRequest dto = new MCRChangePasswordRequest("old-secret", " ");
        setCurrentUser("alice");
        doThrow(new MCRUserValidationException("password is required"))
            .when(userService).changePassword("alice", dto);

        assertThrows(BadRequestException.class, () -> resource.changePassword(dto));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private MediaType buildMediaType(String detailLevel) {
        return new MediaType("application", "json", Map.of("detail", detailLevel));
    }

    private void setCurrentUser(String userId) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.setUserInformation(new MCRUserInformation() {
            @Override
            public String getUserID() {
                return userId;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        });
    }
}
