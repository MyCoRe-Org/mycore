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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRJPATestHelper;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * @author Thomas Scheffler (yagee)
 */
@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRUserExtension.class })
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.URIResolver.CachingResolver.Capacity", string = "0"),
    @MCRTestProperty(key = "MCR.URIResolver.CachingResolver.MaxAge", string = "0")
})
public class MCRUserManagerTest {
    MCRUser user;

    @BeforeEach
    public void setUp() throws Exception {
        user = new MCRUser("junit");
        user.setRealName("Test Case");
        user.setUserAttribute("junit", "test");
        MCRUserManager.createUser(user);
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#getUser(java.lang.String, org.mycore.user2.MCRRealm)}.
     */
    @Test
    public final void testGetUserStringMCRRealm() {
        assertNull(MCRUserManager.getUser(this.user.getUserName(), ""), "Should not load user.");
        MCRUser user = MCRUserManager.getUser(this.user.getUserName(), MCRRealmFactory.getLocalRealm());
        assertNotNull(user, "Could not load user.");
        assertEquals("junit", user.getUserName(), "User name is not as expected");
        assertEquals("Test Case", user.getRealName(), "Real name is not as expected");
    }

    @Test
    public final void testGetUsersByProperty() {
        MCRUser user2 = new MCRUser("junit2");
        user2.setRealName("Test Case II");
        user2.setUserAttribute("junit", "foo");
        user2.setUserAttribute("bar", "test");
        MCRUserManager.createUser(user2);
        MCRUser user3 = new MCRUser("junit3");
        user3.setRealName("Test Case III");
        user3.setUserAttribute("junit", "foo");
        user3.getAttributes().add(new MCRUserAttribute("junit", "test"));
        user3.setUserAttribute("bar", "test failed");
        MCRUserManager.createUser(user3);
        MCRJPATestHelper.startNewTransaction();
        assertEquals(2, MCRUserManager.getUsers("junit", "test").count());
        assertEquals(0, MCRUserManager.getUsers("junit", "test failed").count());
    }

    //MCR-1885
    @Test
    public final void testGetUserPropertiesAfterGetUsers() {
        MCRUser user = new MCRUser("john");
        user.setRealName("John Doe");
        user.setUserAttribute("id_orcid", "1234-5678-1234-0000");
        user.setUserAttribute("id_scopus", "87654321");
        assertEquals(2, user.getAttributes().size());

        MCRUserManager.createUser(user);
        assertEquals(2, user.getAttributes().size());

        MCRJPATestHelper.startNewTransaction();
        MCRUser user2 = MCRUserManager.getUsers("id_orcid", "1234-5678-1234-0000").findFirst().get();
        assertEquals("john", user2.getUserName());

        MCRUser user3 = MCRUserManager.getUser(user2.getUserName(), user2.getRealmID());
        assertEquals(2, user3.getAttributes().size());
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#exists(java.lang.String, org.mycore.user2.MCRRealm)}.
     */
    @Test
    public final void testExistsStringMCRRealm() {
        assertFalse(MCRUserManager.exists(this.user.getUserName(), ""), "Should not find user");
        assertTrue(MCRUserManager.exists(this.user.getUserName(), this.user.getRealm()), "Could not find user");
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#updateUser(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testUpdateUser() {
        String eMail = "info@mycore.de";
        this.user.setEMail(eMail);
        String groupName = "admin";
        this.user.getSystemRoleIDs().add(groupName);
        this.user.getAttributes().add(new MCRUserAttribute("id_key1", "value1"));
        this.user.getAttributes().add(new MCRUserAttribute("id_key1", "value2"));

        MCRUserManager.updateUser(this.user);
        MCRJPATestHelper.startNewTransaction();
        MCRUser user = MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm());
        assertEquals( eMail, user.getEMail(), "User information was not updated");
        assertEquals(1, MCRUserManager.countUsers(null, null, null, null), "User was created not updated");
        assertTrue(user.getSystemRoleIDs().contains(groupName), "User is not in group " + groupName);

        final List<MCRUserAttribute> attributes = user.getAttributes()
            .stream()
            .filter(attr -> attr.getName().equals("id_key1"))
            .toList();

        assertEquals(2, attributes.size(), "There should be two (id_key1) attributes");

        final MCRUserAttribute value2Attr = attributes
            .stream()
            .filter(attr -> attr.getValue().equals("value2"))
            .findFirst()
            .get();

        user.getAttributes().retainAll(List.of(value2Attr));
        MCRUserManager.updateUser(user);
        MCRJPATestHelper.startNewTransaction();
        user = MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm());

        /*
         * currently both attributes get removed if retain all is used
         * Hibernate: delete from MCRUserAttr where id=? and name=? // this is the default attribute junit=test
         * Hibernate: delete from MCRUserAttr where id=? and name=? // this is the id_key1 attribute(s)
         */
        assertEquals(1, user.getAttributes().size(), "There should be one attribute");
    }

    @Test
    public final void testCreateUserUpdateBug() {
        //MCR-2912
        MCRUser user2Created = new MCRUser("junit2");
        user2Created.setRealName("Test Case 2");
        user2Created.setHash("test2");
        user2Created.setUserAttribute("junit2", "test2");
        MCRUserManager.createUser(user2Created);

        user2Created.getAttributes().add(new MCRUserAttribute("junit4", "test3"));
        user2Created.getAttributes().add(new MCRUserAttribute("junit5", "test4"));
        MCRUserManager.updateUser(user2Created);

        MCRUser junit2 = MCRUserManager.getUser("junit2", MCRRealmFactory.getLocalRealm());
        assertEquals(3, junit2.getAttributes().size());
        MCRJPATestHelper.startNewTransaction();

        junit2 = MCRUserManager.getUser("junit2", MCRRealmFactory.getLocalRealm());
        System.out.println(junit2.getAttributes().stream().map(attr -> attr.getName() + "=" + attr.getValue())
            .collect(Collectors.toList()));
        assertEquals(3, junit2.getAttributes().size());
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#deleteUser(java.lang.String, org.mycore.user2.MCRRealm)}.
     */
    @Test
    public final void testDeleteUserStringMCRRealm() {
        MCRUserManager.deleteUser(this.user.getUserName(), this.user.getRealm());
        assertNull(MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm()), "Should not find user");
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#deleteUser(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testDeleteUserMCRUser() {
        MCRUserManager.deleteUser(user);
        assertNull(MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm()), "Should not find user");
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#listUsers(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testListUsersMCRUser() {
        List<MCRUser> listUsers = MCRUserManager.listUsers(null);
        assertEquals(0, listUsers.size(), "Should not find a user");
        user.setOwner(user);
        MCRUserManager.updateUser(user);
        MCRJPATestHelper.startNewTransaction();
        listUsers = MCRUserManager.listUsers(user);
        assertEquals(1, listUsers.size(), "Could not find a user");
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#listUsers(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testListUsersStringStringString() {
        List<MCRUser> listUsers = MCRUserManager.listUsers(null, null, "Test*", null);
        assertEquals(1, listUsers.size(), "Could not find a user");
    }

    /**
     * Test method buildCondition with user attribute name pattern
     */
    @Test
    public final void testConditionWithUserAttributeNamePattern() {
        assertEquals(0, MCRUserManager.countUsers(null, null, null, null,
            "bar", null), "Attribute name search failed");

        MCRUser user2 = new MCRUser("junit2");
        user2.setRealName("Test Case II");
        user2.setUserAttribute("junit2", "foo");
        user2.setUserAttribute("bar", "test");
        MCRUserManager.createUser(user2);
        assertEquals(1, MCRUserManager.countUsers(null, null, null, null,
            "bar", null), "Attribute name search failed");
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#countUsers(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testCountUsers() {
        assertEquals(1, MCRUserManager.countUsers(null, null, "*Case*", null), "Could not find a user");
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#login(java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testLogin() {
        String clearPasswd = "passwd123";
        Date curTime = new Date();
        MCRUser user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNull(user, "Should not login user");
        MCRUserManager.setUserPassword(this.user, clearPasswd);
        MCRUserManager.updateUser(this.user);
        MCRJPATestHelper.startNewTransaction();
        user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNotNull(user, "Could not login user");
        assertNotNull(user.getHashType(), "Hash type was not updated");
        assertNotNull("Hash value was not updated", user.getHash());
        user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNotNull(user.getLastLogin(), "No date set for last login.");
        assertTrue(curTime.before(user.getLastLogin()), "Date was not updated");
        user.disableLogin();
        MCRUserManager.updateUser(user);
        MCRJPATestHelper.startNewTransaction();
        assertNull(MCRUserManager.login(this.user.getUserName(), clearPasswd),
                "Should not login user when account is disabled");
        user.enableLogin();
        user.setValidUntil(new Date());
        MCRUserManager.updateUser(user);
        MCRJPATestHelper.startNewTransaction();
        assertNull(MCRUserManager.login(this.user.getUserName(), clearPasswd),
                "Should not login user when password is expired");
    }

    @Test
    public final void toXML() throws IOException {
        MCRUserManager.setUserPassword(this.user, "passwd123");
        this.user.setEMail("info@mycore.de");
        this.user.setHint("JUnit Test");
        this.user.getSystemRoleIDs().add("admin");
        this.user.getSystemRoleIDs().add("editor");
        this.user.setLastLogin(new Date());
        this.user.setRealName("Test Case");
        this.user.setUserAttribute("tel", "555 4812");
        this.user.setUserAttribute("street", "Heidestraße 12");
        this.user.getAttributes().add(new MCRUserAttribute("tel", "555 4711"));
        this.user.setOwner(this.user);
        MCRUserManager.updateUser(this.user);
        MCRJPATestHelper.startNewTransaction();
        assertEquals(1, MCRUserManager.countUsers(null, null, null, null), "Too many users");
        assertEquals(1, MCRUserManager.listUsers(this.user).size(), "Too many users");
        Document exportableXML = MCRUserTransformer.buildExportableXML(this.user);
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        xout.output(exportableXML, System.out);
    }

    @Test
    public final void testLoadTestUser() {
        Element input = MCRURIResolver.obtainInstance().resolve("resource:test-user.xml");
        MCRUser mcrUser = MCRUserTransformer.buildMCRUser(input);
        mcrUser.setUserName("junit2");
        MCRUserManager.createUser(mcrUser);
    }

}
