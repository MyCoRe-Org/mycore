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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mycore.test.MyCoReTest;
import org.mycore.user.restapi.v2.dto.MCRRealmInfo;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;

import jakarta.ws.rs.core.Response;

@MyCoReTest
public class MCRRealmsTest {

    private final MCRRealms resource = new MCRRealms();

    @Test
    void listRealmsShouldMapAllRealmFields() {
        MCRRealm local = mock(MCRRealm.class);
        when(local.getID()).thenReturn("local");
        when(local.getLabel()).thenReturn("Local");
        when(local.getLoginURL()).thenReturn("/login");
        when(local.getPasswordChangeURL()).thenReturn(null);
        when(local.getCreateURL()).thenReturn("/create");

        MCRRealm ldap = mock(MCRRealm.class);
        when(ldap.getID()).thenReturn("ldap");
        when(ldap.getLabel()).thenReturn("LDAP");
        when(ldap.getLoginURL()).thenReturn("https://ldap.example.org/login");
        when(ldap.getPasswordChangeURL()).thenReturn("https://ldap.example.org/password");
        when(ldap.getCreateURL()).thenReturn(null);

        try (MockedStatic<MCRRealmFactory> realmFactory = mockStatic(MCRRealmFactory.class)) {
            realmFactory.when(MCRRealmFactory::listRealms).thenReturn(List.of(local, ldap));

            Response response = resource.listRealms();

            assertEquals(200, response.getStatus());
            @SuppressWarnings("unchecked")
            List<MCRRealmInfo> realms = (List<MCRRealmInfo>) response.getEntity();
            assertEquals(
                List.of(
                    new MCRRealmInfo("local", "Local", "/login", null, "/create"),
                    new MCRRealmInfo("ldap", "LDAP", "https://ldap.example.org/login",
                        "https://ldap.example.org/password", null)
                ),
                realms
            );
        }
    }

    @Test
    void listRealmsShouldReturnEmptyListWhenNoRealmsConfigured() {
        try (MockedStatic<MCRRealmFactory> realmFactory = mockStatic(MCRRealmFactory.class)) {
            realmFactory.when(MCRRealmFactory::listRealms).thenReturn(List.of());

            Response response = resource.listRealms();

            assertEquals(200, response.getStatus());
            assertEquals(List.of(), response.getEntity());
        }
    }
}
