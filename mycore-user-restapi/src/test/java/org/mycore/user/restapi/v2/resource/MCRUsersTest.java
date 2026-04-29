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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.mycore.restapi.MCRRestConstants;
import org.mycore.test.MyCoReTest;
import org.mycore.user.restapi.v2.MCRUserService;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

@MyCoReTest
@ExtendWith(MockitoExtension.class)
class MCRUsersTest {

    @Mock
    private MCRUserService userService;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private ContainerRequestContext request;

    private MCRUsers resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new MCRUsers(userService);
        setField(resource, "uriInfo", uriInfo);
        setField(resource, "request", request);

        // default detail level
        lenient().when(request.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    void createUser_shouldReturn201WithLocation() throws Exception {
        MCRCreateUserRequest dto = buildCreateUserRequest("alice");
        MCRUserDetail detail = mock(MCRUserDetail.class);
        when(detail.id()).thenReturn("alice");
        when(userService.createUser(dto)).thenReturn(detail);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromUri("http://localhost/api/v2/users"));

        Response response = resource.createUser(dto);

        assertEquals(201, response.getStatus());
        assertTrue(response.getLocation().toString().endsWith("/alice"));
    }

    @Test
    void getUser_shouldCallGetStandard_whenNoDetailLevel() {
        MCRUserStandard standard = mock(MCRUserStandard.class);
        when(userService.getUserStandard("alice")).thenReturn(standard);

        Response response = resource.getUser("alice");

        assertEquals(200, response.getStatus());
        verify(userService).getUserStandard("alice");
    }

    @Test
    void getUser_shouldCallGetSummary_whenDetailLevelIsSummary() {
        MCRUserSummary summary = mock(MCRUserSummary.class);
        when(userService.getUserSummary("alice")).thenReturn(summary);
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(buildMediaType("SUMMARY")));

        Response response = resource.getUser("alice");

        assertEquals(200, response.getStatus());
        verify(userService).getUserSummary("alice");
    }

    @Test
    void getUser_shouldCallGetDetail_whenDetailLevelIsDetailed() {
        MCRUserDetail detail = mock(MCRUserDetail.class);
        when(userService.getUserDetail("alice")).thenReturn(detail);
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(buildMediaType("DETAILED")));

        Response response = resource.getUser("alice");

        assertEquals(200, response.getStatus());
        verify(userService).getUserDetail("alice");
    }

    @Test
    void listUsers_shouldCallListStandard_withCorrectFilter() {
        MCRUserService.MCRUserPage<MCRUserStandard> page = new MCRUserService.MCRUserPage<>(List.of(), 0);
        when(userService.listStandard(any(), eq(0), eq(100))).thenReturn(page);
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE));

        Response response = resource.listUsers("alice*", "John*", "*@example.com", "local", 0, 100);

        assertEquals(200, response.getStatus());
        verify(userService).listStandard(
            eq(new MCRUserService.MCRUserFilter("alice*", "local", "John*", "*@example.com")),
            eq(0), eq(100)
        );
    }

    @Test
    void listUsers_shouldReturnTotalCountHeader() {
        MCRUserService.MCRUserPage<MCRUserStandard> page = new MCRUserService.MCRUserPage<>(List.of(), 42);
        when(userService.listStandard(any(), eq(0), eq(100))).thenReturn(page);
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE));

        Response response = resource.listUsers(null, null, null, null, 0, 100);

        assertEquals("42", response.getHeaderString(MCRRestConstants.HEADER_X_TOTAL_COUNT));
    }

    @Test
    void listUsers_shouldCallListSummary_whenDetailLevelIsSummary() {
        MCRUserService.MCRUserPage<MCRUserSummary> page = new MCRUserService.MCRUserPage<>(List.of(), 0);
        when(userService.listSummary(any(), eq(0), eq(100))).thenReturn(page);
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(buildMediaType("SUMMARY")));

        Response response = resource.listUsers(null, null, null, null, 0, 100);

        assertEquals(200, response.getStatus());
        verify(userService).listSummary(any(), eq(0), eq(100));
    }

    @Test
    void listUsers_shouldCallListDetail_whenDetailLevelIsDetailed() {
        MCRUserService.MCRUserPage<MCRUserDetail> page = new MCRUserService.MCRUserPage<>(List.of(), 0);
        when(userService.listDetail(any(), eq(0), eq(100))).thenReturn(page);
        when(request.getAcceptableMediaTypes()).thenReturn(List.of(buildMediaType("DETAILED")));

        Response response = resource.listUsers(null, null, null, null, 0, 100);

        assertEquals(200, response.getStatus());
        verify(userService).listDetail(any(), eq(0), eq(100));
    }

    @Test
    void updateUser_shouldReturn204() {
        MCRUpdateUserRequest dto = buildUpdateUserRequest();
        Response response = resource.updateUser("alice", dto);

        assertEquals(204, response.getStatus());
        verify(userService).updateUser("alice", dto);
    }

    @Test
    void patchUser_shouldReturn204() throws Exception {
        String patchJson = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Bob\"}]";
        JsonPatch patch = new ObjectMapper().readValue(patchJson, JsonPatch.class);

        Response response = resource.patchUser("alice", patch);

        assertEquals(204, response.getStatus());
        verify(userService).patchUser("alice", patch);
    }

    @Test
    void deleteUser_shouldReturn204() {
        Response response = resource.deleteUser("alice");

        assertEquals(204, response.getStatus());
        verify(userService).deleteUser("alice");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private MediaType buildMediaType(String detailLevel) {
        return new MediaType("application", "json", Map.of("detail", detailLevel));
    }

    private MCRUpdateUserRequest buildUpdateUserRequest() {
        return new MCRUpdateUserRequest(null, null, null, null, false,
            null, Map.of(), null, List.of());
    }

    private MCRCreateUserRequest buildCreateUserRequest(String name) {
        return new MCRCreateUserRequest(name, null, null, null,
            null, false, null, Map.of(), null, List.of());
    }
}
