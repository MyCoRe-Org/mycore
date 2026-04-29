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

package org.mycore.user.restapi.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mycore.common.MCRException;
import org.mycore.user.restapi.exception.MCRUserAlreadyExistsException;
import org.mycore.user.restapi.exception.MCRUserException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

@ExtendWith(MockitoExtension.class)
class MCRUserServiceTest {

    @Mock
    private MCRUserDtoMapper userDtoMapper;

    @Mock
    private ObjectMapper objectMapper;

    private MCRUserService userService;

    @BeforeEach
    void setUp() {
        userService = new MCRUserService(userDtoMapper, objectMapper);
    }

    @Test
    void createUser_shouldReturnDetail() {
        MCRCreateUserRequest request = buildCreateUserRequest("alice");
        MCRUser domainUser = mock(MCRUser.class);
        MCRUserDetail expectedDetail = mock(MCRUserDetail.class);
        List<MCRUser> ownedUsers = List.of();

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.exists("alice")).thenReturn(false);
            when(userDtoMapper.toDomain(request, null)).thenReturn(domainUser);
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(domainUser);
            mgr.when(() -> MCRUserManager.listUsers(domainUser)).thenReturn(ownedUsers);
            when(userDtoMapper.toDetail(domainUser, ownedUsers)).thenReturn(expectedDetail);

            MCRUserDetail result = userService.createUser(request);

            assertEquals(expectedDetail, result);
            verify(userDtoMapper).toDomain(request, null);
            mgr.verify(() -> MCRUserManager.createUser(domainUser));
            verify(userDtoMapper).toDetail(domainUser, ownedUsers);
        }
    }

    @Test
    void createUser_shouldThrowAlreadyExists_whenUserExists() {
        MCRCreateUserRequest request = buildCreateUserRequest("alice");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.exists("alice")).thenReturn(true);
            assertThrows(MCRUserAlreadyExistsException.class, () -> userService.createUser(request));
            mgr.verify(() -> MCRUserManager.createUser(any()), never());
        }
    }

    @Test
    void createUser_shouldThrowValidationException_whenMCRExceptionOccurs() {
        MCRCreateUserRequest request = buildCreateUserRequest("alice");
        MCRUser domainUser = mock(MCRUser.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.exists("alice")).thenReturn(false);
            when(userDtoMapper.toDomain(request, null)).thenReturn(domainUser);
            mgr.when(() -> MCRUserManager.createUser(domainUser)).thenThrow(new MCRException("invalid data"));

            assertThrows(MCRUserValidationException.class, () -> userService.createUser(request));
            mgr.verify(() -> MCRUserManager.createUser(domainUser));
        }
    }

    private MCRCreateUserRequest buildCreateUserRequest(String name) {
        return new MCRCreateUserRequest(name, null, null, null,
            null, false, null, null, null, null);
    }

    @Test
    void getUserSummary_shouldReturnMappedSummary() {
        MCRUser user = mock(MCRUser.class);
        MCRUserSummary summary = mock(MCRUserSummary.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);
            when(userDtoMapper.toSummary(user)).thenReturn(summary);

            assertEquals(summary, userService.getUserSummary("alice"));
            verify(userDtoMapper).toSummary(user);
        }
    }

    @Test
    void getUserSummary_shouldThrowNotFound_whenUserMissing() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("unknown")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class, () -> userService.getUserSummary("unknown"));
        }
    }

    @Test
    void getUserDetail_shouldReturnMappedDetail() {
        MCRUser user = mock(MCRUser.class);
        MCRUserDetail detail = mock(MCRUserDetail.class);
        List<MCRUser> ownedUsers = List.of();

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);
            mgr.when(() -> MCRUserManager.listUsers(user)).thenReturn(ownedUsers);
            when(userDtoMapper.toDetail(user, ownedUsers)).thenReturn(detail);

            assertEquals(detail, userService.getUserDetail("alice"));
            verify(userDtoMapper).toDetail(user, ownedUsers);
        }
    }

    @Test
    void getUserStandard_shouldReturnMappedStandard() {
        MCRUser user = mock(MCRUser.class);
        MCRUserStandard standard = mock(MCRUserStandard.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);
            when(userDtoMapper.toStandard(user)).thenReturn(standard);

            assertEquals(standard, userService.getUserStandard("alice"));
            verify(userDtoMapper).toStandard(user);
        }
    }

    @Test
    void listStandard_shouldReturnPagedResultsWithTotal() {
        MCRUserService.MCRUserFilter filter = new MCRUserService.MCRUserFilter("a*", "local", null, null);
        List<MCRUser> allUsers = List.of(mock(MCRUser.class), mock(MCRUser.class), mock(MCRUser.class));
        MCRUserStandard s0 = mock(MCRUserStandard.class);
        MCRUserStandard s1 = mock(MCRUserStandard.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.listUsers("a*", "local", null, null))
                .thenReturn(allUsers);
            when(userDtoMapper.toStandard(allUsers.get(0))).thenReturn(s0);
            when(userDtoMapper.toStandard(allUsers.get(1))).thenReturn(s1);

            MCRUserService.MCRUserPage<MCRUserStandard> page =
                userService.listStandard(filter, 0, 2);

            assertEquals(List.of(s0, s1), page.users());
            assertEquals(3, page.total());

            verify(userDtoMapper).toStandard(allUsers.get(0));
            verify(userDtoMapper).toStandard(allUsers.get(1));
            verify(userDtoMapper, never()).toStandard(allUsers.get(2));
        }
    }

    @Test
    void listStandard_shouldSkipUsersBeforeOffset() {
        MCRUserService.MCRUserFilter filter = new MCRUserService.MCRUserFilter(null, null, null, null);
        MCRUser u0 = mock(MCRUser.class);
        MCRUser u1 = mock(MCRUser.class);
        MCRUser u2 = mock(MCRUser.class);
        MCRUserStandard s2 = mock(MCRUserStandard.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.listUsers(null, null, null, null))
                .thenReturn(List.of(u0, u1, u2));
            when(userDtoMapper.toStandard(u2)).thenReturn(s2);

            MCRUserService.MCRUserPage<MCRUserStandard> page =
                userService.listStandard(filter, 2, 10);

            assertEquals(List.of(s2), page.users());
            assertEquals(3, page.total());

            verify(userDtoMapper, never()).toStandard(u0);
            verify(userDtoMapper, never()).toStandard(u1);
            verify(userDtoMapper).toStandard(u2);
        }
    }

    @Test
    void listSummary_shouldReturnEmptyPage_whenNoUsersExist() {
        MCRUserService.MCRUserFilter filter = new MCRUserService.MCRUserFilter(null, null, null, null);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.listUsers(null, null, null, null))
                .thenReturn(List.of());

            MCRUserService.MCRUserPage<MCRUserSummary> page =
                userService.listSummary(filter, 0, 10);

            assertTrue(page.users().isEmpty());
            assertEquals(0, page.total());

            verifyNoInteractions(userDtoMapper);
        }
    }

    @Test
    void updateUser_shouldApplyUpdateAndReturnDetail() {
        MCRUpdateUserRequest request = mock(MCRUpdateUserRequest.class);
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);
        MCRUserDetail detail = mock(MCRUserDetail.class);
        List<MCRUser> ownedUsers = List.of();

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice"))
                .thenReturn(existing)
                .thenReturn(updated);
            when(updated.getUserName()).thenReturn("alice");
            when(request.owner()).thenReturn(null);  // kein Owner
            when(userDtoMapper.applyUpdate(existing, request, null)).thenReturn(updated);
            mgr.when(() -> MCRUserManager.listUsers(updated)).thenReturn(ownedUsers);
            when(userDtoMapper.toDetail(updated, ownedUsers)).thenReturn(detail);

            assertEquals(detail, userService.updateUser("alice", request));
            mgr.verify(() -> MCRUserManager.updateUser(updated));
            verify(userDtoMapper).applyUpdate(existing, request, null);
            verify(userDtoMapper).toDetail(updated, ownedUsers);
        }
    }

    @Test
    void updateUser_shouldThrowNotFound_whenUserMissing() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("ghost")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class,
                () -> userService.updateUser("ghost", mock(MCRUpdateUserRequest.class)));

            verifyNoInteractions(userDtoMapper);
        }
    }

    @Test
    void updateUser_shouldThrowValidationException_whenMCRExceptionOccurs() {
        MCRUpdateUserRequest request = mock(MCRUpdateUserRequest.class);
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(existing);
            when(request.owner()).thenReturn(null);
            when(userDtoMapper.applyUpdate(existing, request, null)).thenReturn(updated);
            mgr.when(() -> MCRUserManager.updateUser(updated))
                .thenThrow(new MCRException("bad data"));

            assertThrows(MCRUserValidationException.class, () -> userService.updateUser("alice", request));

            mgr.verify(() -> MCRUserManager.updateUser(updated));
        }
    }

    @Test
    void patchUser_shouldApplyPatchAndReturnDetail() throws Exception {
        JsonPatch patch = mock(JsonPatch.class);
        MCRUser existing = mock(MCRUser.class);
        MCRUserDetail detailBefore = mock(MCRUserDetail.class);
        MCRUserDetail detailAfter = mock(MCRUserDetail.class);
        JsonNode beforeNode = mock(JsonNode.class);
        JsonNode afterNode = mock(JsonNode.class);
        MCRUser updated = mock(MCRUser.class);
        List<MCRUser> ownsBefore = List.of();
        List<MCRUser> ownsAfter = List.of();
        MCRUserService.MCRUserPatchState patchState =
            new MCRUserService.MCRUserPatchState(null, null, null, null, false,
                null, Map.of(), null, List.of());

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice"))
                .thenReturn(existing)
                .thenReturn(updated);
            when(updated.getUserName()).thenReturn("alice");
            mgr.when(() -> MCRUserManager.listUsers(existing)).thenReturn(ownsBefore);
            when(userDtoMapper.toDetail(existing, ownsBefore)).thenReturn(detailBefore);
            when(objectMapper.valueToTree(detailBefore)).thenReturn(beforeNode);
            when(patch.apply(beforeNode)).thenReturn(afterNode);
            when(objectMapper.treeToValue(eq(afterNode), any(Class.class))).thenReturn(patchState);
            when(userDtoMapper.applyUpdate(eq(existing), any(MCRUpdateUserRequest.class), eq(null)))
                .thenReturn(updated);
            mgr.when(() -> MCRUserManager.listUsers(updated)).thenReturn(ownsAfter);
            when(userDtoMapper.toDetail(updated, ownsAfter)).thenReturn(detailAfter);

            assertEquals(detailAfter, userService.patchUser("alice", patch));

            verify(userDtoMapper).toDetail(existing, ownsBefore);
            verify(userDtoMapper).applyUpdate(eq(existing), any(MCRUpdateUserRequest.class), eq(null));
            verify(userDtoMapper).toDetail(updated, ownsAfter);
            mgr.verify(() -> MCRUserManager.updateUser(updated));
        }
    }

    @Test
    void patchUser_shouldThrowMCRUserException_whenPatchFails() throws Exception {
        JsonPatch patch = mock(JsonPatch.class);
        MCRUser existing = mock(MCRUser.class);
        MCRUserDetail detailBefore = mock(MCRUserDetail.class);
        JsonNode beforeNode = mock(JsonNode.class);
        List<MCRUser> ownsBefore = List.of();

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(existing);
            mgr.when(() -> MCRUserManager.listUsers(existing)).thenReturn(ownsBefore);
            when(userDtoMapper.toDetail(existing, ownsBefore)).thenReturn(detailBefore);
            when(objectMapper.valueToTree(detailBefore)).thenReturn(beforeNode);
            when(patch.apply(beforeNode)).thenThrow(new JsonPatchException("op failed"));

            assertThrows(MCRUserException.class, () -> userService.patchUser("alice", patch));

            verifyNoMoreInteractions(userDtoMapper);
        }
    }

    @Test
    void deleteUser_shouldDeleteUser() {
        MCRUser user = mock(MCRUser.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);

            userService.deleteUser("alice");

            mgr.verify(() -> MCRUserManager.deleteUser("alice"));
        }
    }

    @Test
    void deleteUser_shouldDeleteExistingUser() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("ghost")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class, () -> userService.deleteUser("ghost"));

            mgr.verify(() -> MCRUserManager.deleteUser((String) any()), never());
            mgr.verify(() -> MCRUserManager.deleteUser((MCRUser) any()), never());
        }
    }
}
