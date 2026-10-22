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

package org.mycore.user2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRUserExtension.class })
public class MCRRoleManagerTest {

    @Test
    public void listSystemRoles() {
        List<MCRRole> roles = MCRRoleManager.listSystemRoles();
        assertEquals(List.of("admin", "editor"), roles.stream().map(MCRRole::getName).toList());
        assertThrows(UnsupportedOperationException.class, roles::clear,
            "List of system roles should not be modifiable.");
    }

    @Test
    public void addAndDeleteRole() {
        List<MCRRole> roles = MCRRoleManager.listSystemRoles();
        assertNull(MCRRoleManager.getRole("submitter"));

        MCRRoleManager.addRole(new MCRRole("submitter", Set.of(new MCRLabel("en", "Submitter", null))));
        assertNotNull(MCRRoleManager.getRole("submitter"), "Added role should be available immediately.");
        assertEquals(List.of("admin", "editor", "submitter"),
            MCRRoleManager.listSystemRoles().stream().map(MCRRole::getName).toList());
        assertEquals(2, roles.size(), "Previously returned list should not change.");

        MCRRoleManager.deleteRole("submitter");
        assertNull(MCRRoleManager.getRole("submitter"), "Deleted role should not be available anymore.");
        assertEquals(List.of("admin", "editor"),
            MCRRoleManager.listSystemRoles().stream().map(MCRRole::getName).toList());
    }

}
