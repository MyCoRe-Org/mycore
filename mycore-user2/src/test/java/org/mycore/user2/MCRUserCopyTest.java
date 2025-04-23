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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRUserExtension.class })
public class MCRUserCopyTest {

    @Test
    public void testClone() throws CloneNotSupportedException {

        MCRUser boss = createUser("Michael Scott", "ms", null, "admin", "ext-roles:foo");
        MCRUser employee = createUser("Dwight Schrute", "ds", boss, "editor", "ext-roles:bar");

        MCRUser employeeCopy = employee.clone();
        MCRUser bossCopy = employeeCopy.getOwner();

        assertEquals(TestUser.class, employeeCopy.getClass()); // <-- same class as original
        assertEquals(employee.isLocked(), employeeCopy.isLocked());
        assertEquals(employee.isDisabled(), employeeCopy.isDisabled());
        assertEquals(employee.getUserName(), employeeCopy.getUserName());
        assertEquals(employee.getHashType(), employeeCopy.getHashType());
        assertEquals(employee.getHash(), employeeCopy.getHash());
        assertEquals(employee.getSalt(), employeeCopy.getSalt());
        assertEquals(employee.getHint(), employeeCopy.getHint());
        assertEquals(employee.getRealmID(), employeeCopy.getRealmID());
        assertEquals(employee.getRealName(), employeeCopy.getRealName());
        assertEquals(employee.getEMail(), employeeCopy.getEMail());
        assertEquals(employee.getLastLogin(), employeeCopy.getLastLogin());
        assertEquals(employee.getValidUntil(), employeeCopy.getValidUntil());
        assertEquals(employee.getAttributes(), employeeCopy.getAttributes());
        assertEquals(employee.getSystemRoleIDs(), employeeCopy.getSystemRoleIDs());
        assertEquals(employee.getExternalRoleIDs(), employeeCopy.getExternalRoleIDs());

        assertEquals(MCRUser.class, bossCopy.getClass()); // <-- generic user class
        assertEquals(boss.isLocked(), bossCopy.isLocked());
        assertEquals(boss.isDisabled(), bossCopy.isDisabled());
        assertEquals(boss.getUserName(), bossCopy.getUserName());
        assertEquals(boss.getHashType(), bossCopy.getHashType());
        assertEquals(boss.getHash(), bossCopy.getHash());
        assertEquals(boss.getSalt(), bossCopy.getSalt());
        assertEquals(boss.getHint(), bossCopy.getHint());
        assertEquals(boss.getRealmID(), bossCopy.getRealmID());
        assertEquals(boss.getRealName(), bossCopy.getRealName());
        assertEquals(boss.getEMail(), bossCopy.getEMail());
        assertEquals(boss.getLastLogin(), bossCopy.getLastLogin());
        assertEquals(boss.getValidUntil(), bossCopy.getValidUntil());
        assertEquals(boss.getAttributes(), bossCopy.getAttributes());
        assertEquals(boss.getSystemRoleIDs(), bossCopy.getSystemRoleIDs());
        assertEquals(boss.getExternalRoleIDs(), bossCopy.getExternalRoleIDs());

    }

    @Test
    public void testFullCopy() {

        MCRUser boss = createUser("Michael Scott", "ms", null, "admin", "ext-roles:foo");
        MCRUser employee = createUser("Dwight Schrute", "ds", boss, "editor", "ext-roles:bar");

        MCRUser employeeCopy = employee.getFullCopy();
        MCRUser bossCopy = employeeCopy.getOwner();

        assertEquals(MCRUser.class, employeeCopy.getClass()); // <-- generic user class
        assertEquals(employee.isLocked(), employeeCopy.isLocked());
        assertEquals(employee.isDisabled(), employeeCopy.isDisabled());
        assertEquals(employee.getUserName(), employeeCopy.getUserName());
        assertEquals(employee.getHashType(), employeeCopy.getHashType());
        assertEquals(employee.getHash(), employeeCopy.getHash());
        assertEquals(employee.getSalt(), employeeCopy.getSalt());
        assertEquals(employee.getHint(), employeeCopy.getHint());
        assertEquals(employee.getRealmID(), employeeCopy.getRealmID());
        assertEquals(employee.getRealName(), employeeCopy.getRealName());
        assertEquals(employee.getEMail(), employeeCopy.getEMail());
        assertEquals(employee.getLastLogin(), employeeCopy.getLastLogin());
        assertEquals(employee.getValidUntil(), employeeCopy.getValidUntil());
        assertEquals(employee.getAttributes(), employeeCopy.getAttributes());
        assertEquals(employee.getSystemRoleIDs(), employeeCopy.getSystemRoleIDs());
        assertEquals(employee.getExternalRoleIDs(), employeeCopy.getExternalRoleIDs());

        assertEquals(MCRUser.class, bossCopy.getClass()); // <-- generic user class
        assertEquals(boss.isLocked(), bossCopy.isLocked());
        assertEquals(boss.isDisabled(), bossCopy.isDisabled());
        assertEquals(boss.getUserName(), bossCopy.getUserName());
        assertEquals(boss.getHashType(), bossCopy.getHashType());
        assertEquals(boss.getHash(), bossCopy.getHash());
        assertEquals(boss.getSalt(), bossCopy.getSalt());
        assertEquals(boss.getHint(), bossCopy.getHint());
        assertEquals(boss.getRealmID(), bossCopy.getRealmID());
        assertEquals(boss.getRealName(), bossCopy.getRealName());
        assertEquals(boss.getEMail(), bossCopy.getEMail());
        assertEquals(boss.getLastLogin(), bossCopy.getLastLogin());
        assertEquals(boss.getValidUntil(), bossCopy.getValidUntil());
        assertEquals(boss.getAttributes(), bossCopy.getAttributes());
        assertEquals(boss.getSystemRoleIDs(), bossCopy.getSystemRoleIDs());
        assertEquals(boss.getExternalRoleIDs(), bossCopy.getExternalRoleIDs());

    }

    @Test
    public void testSafeCopy() {

        MCRUser boss = createUser("Michael Scott", "ms", null, "admin", "ext-roles:foo");
        MCRUser employee = createUser("Dwight Schrute", "ds", boss, "editor", "ext-roles:bar");

        MCRUser employeeCopy = employee.getSafeCopy();
        MCRUser bossCopy = employeeCopy.getOwner();

        assertEquals(MCRUser.class, employeeCopy.getClass()); // <-- generic user class
        assertEquals(employee.isLocked(), employeeCopy.isLocked());
        assertEquals(employee.isDisabled(), employeeCopy.isDisabled());
        assertEquals(employee.getUserName(), employeeCopy.getUserName());
        assertNull(employeeCopy.getHashType()); // <-- password information not present
        assertNull(employeeCopy.getHash());
        assertNull(employeeCopy.getSalt());
        assertEquals(employee.getHint(), employeeCopy.getHint());
        assertEquals(employee.getRealmID(), employeeCopy.getRealmID());
        assertEquals(employee.getRealName(), employeeCopy.getRealName());
        assertEquals(employee.getEMail(), employeeCopy.getEMail());
        assertEquals(employee.getLastLogin(), employeeCopy.getLastLogin());
        assertEquals(employee.getValidUntil(), employeeCopy.getValidUntil());
        assertEquals(employee.getAttributes(), employeeCopy.getAttributes());
        assertEquals(employee.getSystemRoleIDs(), employeeCopy.getSystemRoleIDs());
        assertEquals(employee.getExternalRoleIDs(), employeeCopy.getExternalRoleIDs());

        assertEquals(MCRUser.class, bossCopy.getClass()); // <-- generic user class
        assertEquals(boss.isLocked(), bossCopy.isLocked());
        assertEquals(boss.isDisabled(), bossCopy.isDisabled());
        assertEquals(boss.getUserName(), bossCopy.getUserName());
        assertNull(bossCopy.getHashType()); // <-- password information not present
        assertNull(bossCopy.getHash());
        assertNull(bossCopy.getSalt());
        assertEquals(boss.getHint(), bossCopy.getHint());
        assertEquals(boss.getRealmID(), bossCopy.getRealmID());
        assertEquals(boss.getRealName(), bossCopy.getRealName());
        assertEquals(boss.getEMail(), bossCopy.getEMail());
        assertEquals(boss.getLastLogin(), bossCopy.getLastLogin());
        assertEquals(boss.getValidUntil(), bossCopy.getValidUntil());
        assertEquals(boss.getAttributes(), bossCopy.getAttributes());
        assertEquals(boss.getSystemRoleIDs(), bossCopy.getSystemRoleIDs());
        assertEquals(boss.getExternalRoleIDs(), bossCopy.getExternalRoleIDs());

    }

    @Test
    public void testBasicCopy() {

        MCRUser boss = createUser("Michael Scott", "ms", null, "admin", "ext-roles:foo");
        MCRUser employee = createUser("Dwight Schrute", "ds", boss, "editor", "ext-roles:bar");

        MCRUser employeeCopy = employee.getBasicCopy();
        MCRUser bossCopy = employeeCopy.getOwner();

        assertEquals(MCRUser.class, employeeCopy.getClass()); // <-- generic user class
        assertEquals(employee.isLocked(), employeeCopy.isLocked());
        assertEquals(employee.isDisabled(), employeeCopy.isDisabled());
        assertEquals(employee.getUserName(), employeeCopy.getUserName());
        assertNull(employeeCopy.getHashType()); // <-- password information not present
        assertNull(employeeCopy.getHash());
        assertNull(employeeCopy.getSalt());
        assertNull(employeeCopy.getHint()); // <-- password hint not present
        assertEquals(employee.getRealmID(), employeeCopy.getRealmID());
        assertNull(employeeCopy.getRealName()); // <-- personal information not present
        assertNull(employeeCopy.getEMail());
        assertNull(employeeCopy.getLastLogin()); // <-- internal information not present
        assertNull(employeeCopy.getValidUntil());
        assertEquals(Collections.emptySortedSet(), employeeCopy.getAttributes()); // <-- empty collections
        assertEquals(Collections.emptySet(), employeeCopy.getSystemRoleIDs());
        assertEquals(Collections.emptySet(), employeeCopy.getExternalRoleIDs());

        assertEquals(MCRUser.class, bossCopy.getClass()); // <-- generic user class
        assertEquals(boss.isLocked(), bossCopy.isLocked());
        assertEquals(boss.isDisabled(), bossCopy.isDisabled());
        assertEquals(boss.getUserName(), bossCopy.getUserName());
        assertNull(bossCopy.getHashType()); // <-- password information not present
        assertNull(bossCopy.getHash());
        assertNull(bossCopy.getSalt());
        assertNull(bossCopy.getHint()); // <-- password hint not present
        assertEquals(boss.getRealmID(), bossCopy.getRealmID());
        assertNull(bossCopy.getRealName()); // <-- personal information not present
        assertNull(bossCopy.getEMail());
        assertNull(bossCopy.getLastLogin()); // <-- internal information not present
        assertNull(bossCopy.getValidUntil());
        assertEquals(Collections.emptySortedSet(), bossCopy.getAttributes()); // <-- empty collections
        assertEquals(Collections.emptySet(), bossCopy.getSystemRoleIDs());
        assertEquals(Collections.emptySet(), bossCopy.getExternalRoleIDs());

    }

    private static MCRUser createUser(String realName, String id, MCRUser owner, String... roles) {
        MCRUser user = new TestUser();
        user.setLocked(true);
        user.setDisabled(true);
        user.setUserName("theUserName-" + id);
        user.setHashType("theHashType-" + id);
        user.setHash("thePassword-" + id);
        user.setSalt("theSalt-" + id);
        user.setHint("theHint-" + id);
        user.setRealmID("local");
        user.setOwner(owner);
        user.setRealName(realName);
        user.setEMail(id + "@e.mail");
        user.setLastLogin(new Date(1000));
        user.setValidUntil(new Date(2000));
        user.setUserAttribute("theAttribute1", "theValue1-" + id);
        user.setUserAttribute("theAttribute2", "theValue2-" + id);
        for (String role : roles) {
            user.assignRole(role);
        }
        return user;
    }

    private static class TestUser extends MCRUser {
    }

}
