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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mycore.common.MCRException;
import org.mycore.test.MyCoReTest;
import org.mycore.user.restapi.exception.MCRUserAlreadyExistsException;
import org.mycore.user.restapi.exception.MCRUserNoLocalPasswordException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;
import org.mycore.user.restapi.exception.MCRUserWrongPasswordException;
import org.mycore.user.restapi.v2.dto.MCRChangePasswordRequest;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRSetPasswordRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserProfileRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

@MyCoReTest
@ExtendWith(MockitoExtension.class)
public class MCRUserServiceTest {

    @Mock
    private MCRUserDtoMapper userDtoMapper;

    private MCRUserService userService;

    @BeforeEach
    void setUp() {
        userService = new MCRUserService(userDtoMapper);
    }

    @Test
    void createUserShouldReturnDetail() {
        MCRCreateUserRequest request = buildCreateUserRequest("alice", "foo");
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
    void createUserShouldThrowAlreadyExistsWhenUserExists() {
        MCRCreateUserRequest request = buildCreateUserRequest("alice", "foo");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.exists("alice")).thenReturn(true);
            assertThrows(MCRUserAlreadyExistsException.class, () -> userService.createUser(request));
            mgr.verify(() -> MCRUserManager.createUser(any()), never());
        }
    }

    @Test
    void createUserShouldThrowValidationExceptionWhenMCRExceptionOccurs() {
        MCRCreateUserRequest request = buildCreateUserRequest("alice", "foo");
        MCRUser domainUser = mock(MCRUser.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.exists("alice")).thenReturn(false);
            when(userDtoMapper.toDomain(request, null)).thenReturn(domainUser);
            mgr.when(() -> MCRUserManager.createUser(domainUser)).thenThrow(new MCRException("invalid data"));

            assertThrows(MCRUserValidationException.class, () -> userService.createUser(request));
            mgr.verify(() -> MCRUserManager.createUser(domainUser));
        }
    }

    private MCRCreateUserRequest buildCreateUserRequest(String name, String password) {
        return new MCRCreateUserRequest(name, null, null, password,
            null, false, null, null, null, null);
    }

    @Test
    void getUserSummaryShouldReturnMappedSummary() {
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
    void getUserSummaryShouldThrowNotFoundWhenUserMissing() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("unknown")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class, () -> userService.getUserSummary("unknown"));
        }
    }

    @Test
    void getUserDetailShouldReturnMappedDetail() {
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
    void getUserStandardShouldReturnMappedStandard() {
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
    void listStandardShouldReturnPagedResultsWithTotal() {
        MCRUserService.MCRUserFilter filter = new MCRUserService.MCRUserFilter("a*", "local", null, null);
        MCRUser u0 = mock(MCRUser.class);
        MCRUser u1 = mock(MCRUser.class);
        MCRUserStandard s0 = mock(MCRUserStandard.class);
        MCRUserStandard s1 = mock(MCRUserStandard.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.listUsers("a*", "local", null, null, null, null, 0, 2))
                .thenReturn(List.of(u0, u1));
            mgr.when(() -> MCRUserManager.countUsers("a*", "local", null, null))
                .thenReturn(3);
            when(userDtoMapper.toStandard(u0)).thenReturn(s0);
            when(userDtoMapper.toStandard(u1)).thenReturn(s1);

            MCRUserService.MCRUserPage<MCRUserStandard> page =
                userService.listStandard(filter, 0, 2);

            assertEquals(List.of(s0, s1), page.users());
            assertEquals(3L, page.total());

            verify(userDtoMapper).toStandard(u0);
            verify(userDtoMapper).toStandard(u1);
        }
    }

    @Test
    void listStandardShouldSkipUsersBeforeOffset() {
        MCRUserService.MCRUserFilter filter = new MCRUserService.MCRUserFilter(null, null, null, null);
        MCRUser u2 = mock(MCRUser.class);
        MCRUserStandard s2 = mock(MCRUserStandard.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.listUsers(null, null, null, null, null, null, 2, 10))
                .thenReturn(List.of(u2));
            mgr.when(() -> MCRUserManager.countUsers(null, null, null, null))
                .thenReturn(3);
            when(userDtoMapper.toStandard(u2)).thenReturn(s2);

            MCRUserService.MCRUserPage<MCRUserStandard> page =
                userService.listStandard(filter, 2, 10);

            assertEquals(List.of(s2), page.users());
            assertEquals(3L, page.total());

            verify(userDtoMapper).toStandard(u2);
        }
    }

    @Test
    void listSummaryShouldReturnEmptyPageWhenNoUsersExist() {
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
    void updateUserShouldApplyUpdateAndReturnDetail() {
        MCRUpdateUserRequest request = mock(MCRUpdateUserRequest.class);
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);
        MCRUserDetail detail = mock(MCRUserDetail.class);
        List<MCRUser> ownedUsers = List.of();

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice"))
                .thenReturn(existing)
                .thenReturn(updated);
            when(updated.getUserID()).thenReturn("alice");
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
    void updateUserShouldRefetchByQualifiedUserIdForNonLocalRealmUser() {
        // Regression test: MCRUserManager.getUser(String) resolves an unqualified name (no "@")
        // against the LOCAL realm only. Refetching by getUserName() instead of getUserID()
        // would incorrectly look up a non-local-realm user in the wrong realm and report a
        // successful update as MCRUserNotFoundException.
        MCRUpdateUserRequest request = mock(MCRUpdateUserRequest.class);
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);
        MCRUserDetail detail = mock(MCRUserDetail.class);
        List<MCRUser> ownedUsers = List.of();

        when(request.owner()).thenReturn(null);
        when(updated.getUserID()).thenReturn("bob@shibboleth");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("bob@shibboleth"))
                .thenReturn(existing)
                .thenReturn(updated);
            when(userDtoMapper.applyUpdate(existing, request, null)).thenReturn(updated);
            mgr.when(() -> MCRUserManager.listUsers(updated)).thenReturn(ownedUsers);
            when(userDtoMapper.toDetail(updated, ownedUsers)).thenReturn(detail);

            assertEquals(detail, userService.updateUser("bob@shibboleth", request));

            mgr.verify(() -> MCRUserManager.getUser("bob@shibboleth"), times(2));
            mgr.verify(() -> MCRUserManager.getUser("bob"), never());
        }
    }

    @Test
    void updateUserShouldSetPasswordForLocalRealmUser() {
        MCRUpdateUserRequest request = mock(MCRUpdateUserRequest.class);
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);
        MCRUserDetail detail = mock(MCRUserDetail.class);
        MCRRealm localRealm = mock(MCRRealm.class);
        List<MCRUser> ownedUsers = List.of();

        when(request.password()).thenReturn("new-secret");
        when(existing.getRealm()).thenReturn(localRealm);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class);
            MockedStatic<MCRRealmFactory> realmFactory = mockStatic(MCRRealmFactory.class)) {
            realmFactory.when(MCRRealmFactory::getLocalRealm).thenReturn(localRealm);
            mgr.when(() -> MCRUserManager.getUser("alice"))
                .thenReturn(existing)
                .thenReturn(updated);
            when(updated.getUserID()).thenReturn("alice");
            when(request.owner()).thenReturn(null);
            when(userDtoMapper.applyUpdate(existing, request, null)).thenReturn(updated);
            mgr.when(() -> MCRUserManager.listUsers(updated)).thenReturn(ownedUsers);
            when(userDtoMapper.toDetail(updated, ownedUsers)).thenReturn(detail);

            assertEquals(detail, userService.updateUser("alice", request));
            mgr.verify(() -> MCRUserManager.setPassword(updated, "new-secret"));
        }
    }

    @Test
    void updateUserShouldThrowNoLocalPasswordWhenSettingPasswordForNonLocalRealmUser() {
        MCRUpdateUserRequest request = mock(MCRUpdateUserRequest.class);
        MCRUser existing = mock(MCRUser.class);
        MCRRealm localRealm = mock(MCRRealm.class);
        MCRRealm externalRealm = mock(MCRRealm.class);

        when(request.password()).thenReturn("new-secret");
        when(existing.getRealm()).thenReturn(externalRealm);
        when(existing.getUserID()).thenReturn("bob@shibboleth");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class);
            MockedStatic<MCRRealmFactory> realmFactory = mockStatic(MCRRealmFactory.class)) {
            realmFactory.when(MCRRealmFactory::getLocalRealm).thenReturn(localRealm);
            mgr.when(() -> MCRUserManager.getUser("bob@shibboleth")).thenReturn(existing);

            assertThrows(MCRUserNoLocalPasswordException.class,
                () -> userService.updateUser("bob@shibboleth", request));

            verifyNoInteractions(userDtoMapper);
            mgr.verify(() -> MCRUserManager.updateUser(any()), never());
        }
    }

    @Test
    void updateUserShouldThrowNotFoundWhenUserMissing() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("ghost")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class,
                () -> userService.updateUser("ghost", mock(MCRUpdateUserRequest.class)));

            verifyNoInteractions(userDtoMapper);
        }
    }

    @Test
    void updateUserShouldThrowValidationExceptionWhenMCRExceptionOccurs() {
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
    void updateUserWithOwnRequestShouldApplyUpdateAndReturnDetail() {
        MCRUpdateUserProfileRequest request = new MCRUpdateUserProfileRequest("Alice", "alice@example.com", "hint");
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);
        MCRUserDetail detail = mock(MCRUserDetail.class);
        List<MCRUser> ownedUsers = List.of();

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice"))
                .thenReturn(existing)
                .thenReturn(updated);
            when(updated.getUserID()).thenReturn("alice");
            when(userDtoMapper.applyUpdate(existing, request)).thenReturn(updated);
            mgr.when(() -> MCRUserManager.listUsers(updated)).thenReturn(ownedUsers);
            when(userDtoMapper.toDetail(updated, ownedUsers)).thenReturn(detail);

            assertEquals(detail, userService.updateUser("alice", request));
            mgr.verify(() -> MCRUserManager.updateUser(updated));
            verify(userDtoMapper).applyUpdate(existing, request);
        }
    }

    @Test
    void updateUserWithOwnRequestShouldRefetchByQualifiedUserIdForNonLocalRealmUser() {
        // Same regression as updateUserShouldRefetchByQualifiedUserIdForNonLocalRealmUser,
        // for the self-service overload.
        MCRUpdateUserProfileRequest request = new MCRUpdateUserProfileRequest("Bob", "bob@example.com", null);
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);
        MCRUserDetail detail = mock(MCRUserDetail.class);
        List<MCRUser> ownedUsers = List.of();

        when(updated.getUserID()).thenReturn("bob@shibboleth");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("bob@shibboleth"))
                .thenReturn(existing)
                .thenReturn(updated);
            when(userDtoMapper.applyUpdate(existing, request)).thenReturn(updated);
            mgr.when(() -> MCRUserManager.listUsers(updated)).thenReturn(ownedUsers);
            when(userDtoMapper.toDetail(updated, ownedUsers)).thenReturn(detail);

            assertEquals(detail, userService.updateUser("bob@shibboleth", request));

            mgr.verify(() -> MCRUserManager.getUser("bob@shibboleth"), times(2));
            mgr.verify(() -> MCRUserManager.getUser("bob"), never());
        }
    }

    @Test
    void updateUserWithOwnRequestShouldThrowNotFoundWhenUserMissing() {
        MCRUpdateUserProfileRequest request = new MCRUpdateUserProfileRequest("Ghost", "ghost@example.com", null);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("ghost")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class, () -> userService.updateUser("ghost", request));

            verifyNoInteractions(userDtoMapper);
        }
    }

    @Test
    void updateUserWithOwnRequestShouldThrowValidationExceptionWhenEmailInvalid() {
        MCRUpdateUserProfileRequest request = new MCRUpdateUserProfileRequest("Alice", "not-an-email", null);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            assertThrows(MCRUserValidationException.class, () -> userService.updateUser("alice", request));

            mgr.verify(() -> MCRUserManager.getUser(any()), never());
        }
    }

    @Test
    void updateUserWithOwnRequestShouldThrowValidationExceptionWhenNameBlank() {
        // MCRUpdateUserProfileRequest documents name as required (unlike passwordHint, which
        // may be null); a blank name must not silently wipe the existing real name.
        MCRUpdateUserProfileRequest request = new MCRUpdateUserProfileRequest(" ", "alice@example.com", null);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            assertThrows(MCRUserValidationException.class, () -> userService.updateUser("alice", request));

            mgr.verify(() -> MCRUserManager.getUser(any()), never());
        }
    }

    @Test
    void updateUserWithOwnRequestShouldThrowValidationExceptionWhenMCRExceptionOccurs() {
        MCRUpdateUserProfileRequest request = new MCRUpdateUserProfileRequest("Alice", "alice@example.com", null);
        MCRUser existing = mock(MCRUser.class);
        MCRUser updated = mock(MCRUser.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(existing);
            when(userDtoMapper.applyUpdate(existing, request)).thenReturn(updated);
            mgr.when(() -> MCRUserManager.updateUser(updated)).thenThrow(new MCRException("bad data"));

            assertThrows(MCRUserValidationException.class, () -> userService.updateUser("alice", request));

            mgr.verify(() -> MCRUserManager.updateUser(updated));
        }
    }

    @Test
    void setPasswordShouldSetNewPassword() {
        MCRUser user = mock(MCRUser.class);
        MCRRealm localRealm = mock(MCRRealm.class);
        when(user.getRealm()).thenReturn(localRealm);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class);
            MockedStatic<MCRRealmFactory> realmFactory = mockStatic(MCRRealmFactory.class)) {
            realmFactory.when(MCRRealmFactory::getLocalRealm).thenReturn(localRealm);
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);

            userService.setPassword("alice", new MCRSetPasswordRequest("new-secret"));

            mgr.verify(() -> MCRUserManager.setPassword(user, "new-secret"));
        }
    }

    @Test
    void setPasswordShouldThrowNoLocalPasswordWhenUserNotInLocalRealm() {
        MCRUser user = mock(MCRUser.class);
        MCRRealm localRealm = mock(MCRRealm.class);
        MCRRealm externalRealm = mock(MCRRealm.class);
        when(user.getRealm()).thenReturn(externalRealm);
        when(user.getUserID()).thenReturn("bob@shibboleth");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class);
            MockedStatic<MCRRealmFactory> realmFactory = mockStatic(MCRRealmFactory.class)) {
            realmFactory.when(MCRRealmFactory::getLocalRealm).thenReturn(localRealm);
            mgr.when(() -> MCRUserManager.getUser("bob@shibboleth")).thenReturn(user);

            assertThrows(MCRUserNoLocalPasswordException.class,
                () -> userService.setPassword("bob@shibboleth", new MCRSetPasswordRequest("new-secret")));

            mgr.verify(() -> MCRUserManager.setPassword(any(), any()), never());
        }
    }

    @Test
    void setPasswordShouldThrowNotFoundWhenUserMissing() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("ghost")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class,
                () -> userService.setPassword("ghost", new MCRSetPasswordRequest("new-secret")));

            mgr.verify(() -> MCRUserManager.setPassword(any(), any()), never());
        }
    }

    @Test
    void setPasswordShouldThrowValidationExceptionWhenPasswordBlank() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            assertThrows(MCRUserValidationException.class,
                () -> userService.setPassword("alice", new MCRSetPasswordRequest(" ")));

            mgr.verify(() -> MCRUserManager.getUser(any()), never());
        }
    }

    @Test
    void setPasswordShouldThrowValidationExceptionWhenMCRExceptionOccurs() {
        MCRUser user = mock(MCRUser.class);
        MCRRealm localRealm = mock(MCRRealm.class);
        when(user.getRealm()).thenReturn(localRealm);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class);
            MockedStatic<MCRRealmFactory> realmFactory = mockStatic(MCRRealmFactory.class)) {
            realmFactory.when(MCRRealmFactory::getLocalRealm).thenReturn(localRealm);
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);
            mgr.when(() -> MCRUserManager.setPassword(user, "new-secret"))
                .thenThrow(new MCRException("not allowed"));

            assertThrows(MCRUserValidationException.class,
                () -> userService.setPassword("alice", new MCRSetPasswordRequest("new-secret")));
        }
    }

    @Test
    void changePasswordShouldSucceedWhenOldPasswordCorrect() {
        MCRUser user = mock(MCRUser.class);
        when(user.getUserName()).thenReturn("alice");
        when(user.getHashType()).thenReturn("sha256");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);
            mgr.when(() -> MCRUserManager.checkPassword("alice", "old-secret")).thenReturn(user);

            userService.changePassword("alice", new MCRChangePasswordRequest("old-secret", "new-secret"));

            mgr.verify(() -> MCRUserManager.setPassword(user, "new-secret"));
        }
    }

    @Test
    void changePasswordShouldThrowWrongPasswordWhenOldPasswordIncorrect() {
        MCRUser user = mock(MCRUser.class);
        when(user.getUserName()).thenReturn("alice");
        when(user.getHashType()).thenReturn("sha256");

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);
            mgr.when(() -> MCRUserManager.checkPassword("alice", "wrong")).thenReturn(null);

            assertThrows(MCRUserWrongPasswordException.class,
                () -> userService.changePassword("alice", new MCRChangePasswordRequest("wrong", "new-secret")));

            mgr.verify(() -> MCRUserManager.setPassword(any(), any()), never());
        }
    }

    @Test
    void changePasswordShouldThrowNoLocalPasswordWhenUserHasNoLocalPassword() {
        MCRUser user = mock(MCRUser.class);
        when(user.getHashType()).thenReturn(null);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);

            assertThrows(MCRUserNoLocalPasswordException.class,
                () -> userService.changePassword("alice", new MCRChangePasswordRequest("whatever", "new-secret")));

            mgr.verify(() -> MCRUserManager.checkPassword(any(), any()), never());
            mgr.verify(() -> MCRUserManager.setPassword(any(), any()), never());
        }
    }

    @Test
    void changePasswordShouldThrowNotFoundWhenUserMissing() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("ghost")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class,
                () -> userService.changePassword("ghost", new MCRChangePasswordRequest("old", "new-secret")));
        }
    }

    @Test
    void deleteUserShouldDeleteUser() {
        MCRUser user = mock(MCRUser.class);

        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("alice")).thenReturn(user);

            userService.deleteUser("alice");

            mgr.verify(() -> MCRUserManager.deleteUser("alice"));
        }
    }

    @Test
    void deleteUserShouldDeleteExistingUser() {
        try (MockedStatic<MCRUserManager> mgr = mockStatic(MCRUserManager.class)) {
            mgr.when(() -> MCRUserManager.getUser("ghost")).thenReturn(null);

            assertThrows(MCRUserNotFoundException.class, () -> userService.deleteUser("ghost"));

            mgr.verify(() -> MCRUserManager.deleteUser((String) any()), never());
            mgr.verify(() -> MCRUserManager.deleteUser((MCRUser) any()), never());
        }
    }
}
