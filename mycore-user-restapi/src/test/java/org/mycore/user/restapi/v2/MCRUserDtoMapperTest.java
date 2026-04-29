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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserExtension;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRUserExtension.class })
public class MCRUserDtoMapperTest {

    private MCRUserDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MCRUserDtoMapper();
    }

    @Test
    void toDomain_shouldMapBasicFields() {
        MCRCreateUserRequest request = new MCRCreateUserRequest(
            "john", "John Doe", "john@example.com",
            null, null, false, null,
            Map.of(), null, List.of()
        );

        MCRUser result = mapper.toDomain(request, null);

        assertEquals("john", result.getUserID());
        assertEquals("John Doe", result.getRealName());
        assertEquals("john@example.com", result.getEMail());
    }

    @Test
    void toDomain_shouldAssignRoles() {
        MCRCreateUserRequest request = new MCRCreateUserRequest(
            "john", null, null,
            null, null, false, null,
            Map.of(), null, List.of("admin", "editor")
        );

        MCRUser result = mapper.toDomain(request, null);

        assertEquals(2, result.getSystemRoleIDs().size());
        assertTrue(result.getSystemRoleIDs().contains("admin"));
        assertTrue(result.getSystemRoleIDs().contains("editor"));
    }

    @Test
    void toDomain_shouldResolveOwner() {
        MCRUser owner = buildUser("admin");

        MCRCreateUserRequest request = new MCRCreateUserRequest(
            "john", null, null,
            null, null, false, null,
            Map.of(), "admin", List.of()
        );

        MCRUser result = mapper.toDomain(request, owner);

        assertEquals("admin", result.getOwner().getUserID());
    }

    @Test
    void toSummary_shouldMapUserIdAndRealName() {
        MCRUser user = buildUser("john");
        user.setRealName("John Doe");

        MCRUserSummary summary = mapper.toSummary(user);

        assertEquals("john", summary.id());
        assertEquals("John Doe", summary.name());
    }

    @Test
    void toSummary_shouldHandleNullRealName() {
        MCRUser user = buildUser("john");

        MCRUserSummary summary = mapper.toSummary(user);

        assertNull(summary.name());
    }

    @Test
    void toStandard_shouldMapAllFields() {
        MCRUser user = buildUser("john");
        user.setRealName("John Doe");
        user.setEMail("john@example.com");
        user.setLocked(true);
        user.setValidUntil(Date.from(Instant.parse("2099-01-01T00:00:00Z")));
        user.assignRole("admin");

        MCRUserStandard standard = mapper.toStandard(user);

        assertEquals("john", standard.id());
        assertEquals("John Doe", standard.name());
        assertEquals("john@example.com", standard.email());
        assertTrue(standard.locked());
        assertNotNull(standard.validUntil());
        assertEquals(1, standard.roles().size());
        assertTrue(standard.roles().contains("admin"));
    }

    @Test
    void toStandard_shouldHandleNullValidUntil() {
        MCRUser user = buildUser("john");

        MCRUserStandard standard = mapper.toStandard(user);

        assertNull(standard.validUntil());
    }

    @Test
    void toStandard_shouldHandleNullOwner() {
        MCRUser user = buildUser("john");

        MCRUserStandard standard = mapper.toStandard(user);

        assertNull(standard.owner());
    }

    @Test
    void toStandard_shouldMapOwner() {
        MCRUser owner = buildUser("admin");
        MCRUser user = buildUser("john");
        user.setOwner(owner);

        MCRUserStandard standard = mapper.toStandard(user);

        assertEquals("admin", standard.owner());
    }

    @Test
    void toStandard_shouldMapAttributes() {
        MCRUser user = buildUser("john");
        user.setUserAttribute("foo", "bar");

        MCRUserStandard standard = mapper.toStandard(user);

        assertEquals(1, standard.attributes().size());
        assertNotNull(standard.attributes().get("foo"));
        assertEquals("bar", standard.attributes().get("foo"));
    }

    @Test
    void toDetail_shouldMapAllFields() {
        MCRUser user = buildUser("john");
        user.setRealName("John Doe");
        user.setEMail("john@example.com");
        user.setLocked(false);
        user.setValidUntil(Date.from(Instant.parse("2099-01-01T00:00:00Z")));
        user.assignRole("editor");

        MCRUserDetail detail = mapper.toDetail(user, List.of());

        assertEquals("john", detail.id());
        assertEquals("John Doe", detail.name());
        assertEquals("john@example.com", detail.email());
        assertFalse(detail.locked());
        assertNotNull(detail.validUntil());
        assertEquals(1, detail.roles().size());
        assertTrue(detail.roles().contains("editor"));
    }

    @Test
    void toDetail_shouldMapOwnedUsers() {
        MCRUser user = buildUser("admin");
        MCRUser owned = buildUser("editor");

        MCRUserDetail detail = mapper.toDetail(user, List.of(owned));

        assertEquals(1, detail.owns().size());
        assertEquals("editor", detail.owns().getFirst());
    }

    @Test
    void toDetail_shouldHandleNullLastLogin() {
        MCRUser user = buildUser("john");

        MCRUserDetail detail = mapper.toDetail(user, List.of());

        assertNull(detail.lastLogin());
    }

    @Test
    void applyUpdate_shouldReplaceRoles() {
        MCRUser user = buildUser("john");
        user.assignRole("editor");

        MCRUpdateUserRequest request = new MCRUpdateUserRequest(
            "John Doe", null, null, null, false, null,
            Map.of(), null, List.of("admin"));

        MCRUser result = mapper.applyUpdate(user, request, null);

        assertEquals(1, result.getSystemRoleIDs().size());
        assertTrue(result.getSystemRoleIDs().contains("admin"));
        assertFalse(result.getSystemRoleIDs().contains("editor"));
    }

    @Test
    void applyUpdate_shouldReplaceAttributes() {
        MCRUser user = buildUser("john");
        user.setUserAttribute("old", "value");

        MCRUpdateUserRequest request = new MCRUpdateUserRequest(
            "John Doe", null, null, null, false, null,
            Map.of("new", "value"), null, List.of());

        MCRUser result = mapper.applyUpdate(user, request, null);

        assertEquals(1, result.getAttributes().size());
        assertNotNull(result.getUserAttribute("new"));
        assertNull(result.getUserAttribute("old"));
    }

    private MCRUser buildUser(String userId) {
        return new MCRUser(userId);
    }
}
