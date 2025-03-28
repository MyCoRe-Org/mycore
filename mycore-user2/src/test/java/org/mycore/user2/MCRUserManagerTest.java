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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUserManagerTest extends MCRUserTestCase {
    MCRUser user;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        user = new MCRUser("junit");
        user.setRealName("Test Case");
        user.setPassword("test");
        user.setUserAttribute("junit", "test");
        MCRUserManager.createUser(user);
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#getUser(java.lang.String, org.mycore.user2.MCRRealm)}.
     */
    @Test
    public final void testGetUserStringMCRRealm() {
        assertNull("Should not load user.", MCRUserManager.getUser(this.user.getUserName(), ""));
        MCRUser user = MCRUserManager.getUser(this.user.getUserName(), MCRRealmFactory.getLocalRealm());
        assertNotNull("Could not load user.", user);
        assertEquals("Password hash is not as expected", "test", user.getPassword());
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
        startNewTransaction();
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

        startNewTransaction();
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
        assertFalse("Should not find user", MCRUserManager.exists(this.user.getUserName(), ""));
        assertTrue("Could not find user", MCRUserManager.exists(this.user.getUserName(), this.user.getRealm()));
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
        startNewTransaction();
        MCRUser user = MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm());
        assertEquals("User information was not updated", eMail, user.getEMailAddress());
        assertEquals("User was created not updated", 1, MCRUserManager.countUsers(null, null, null, null));
        assertTrue("User is not in group " + groupName, user.getSystemRoleIDs().contains(groupName));

        final List<MCRUserAttribute> attributes = user.getAttributes()
            .stream()
            .filter(attr -> attr.getName().equals("id_key1"))
            .collect(Collectors.toList());

        assertEquals("There should be two (id_key1) attributes", 2, attributes.size());

        final MCRUserAttribute value2Attr = attributes
            .stream()
            .filter(attr -> attr.getValue().equals("value2"))
            .findFirst()
            .get();

        user.getAttributes().retainAll(List.of(value2Attr));
        MCRUserManager.updateUser(user);
        startNewTransaction();
        user = MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm());

        /*
         * currently both attributes get removed if retain all is used
         * Hibernate: delete from MCRUserAttr where id=? and name=? // this is the default attribute junit=test
         * Hibernate: delete from MCRUserAttr where id=? and name=? // this is the id_key1 attribute(s)
         */
        assertEquals("There should be one attribute", 1, user.getAttributes().size());
    }

    @Test
    public final void testCreateUserUpdateBug() {
        //MCR-2912
        MCRUser user2Created = new MCRUser("junit2");
        user2Created.setRealName("Test Case 2");
        user2Created.setPassword("test2");
        user2Created.setUserAttribute("junit2", "test2");
        MCRUserManager.createUser(user2Created);

        user2Created.getAttributes().add(new MCRUserAttribute("junit4", "test3"));
        user2Created.getAttributes().add(new MCRUserAttribute("junit5", "test4"));
        MCRUserManager.updateUser(user2Created);

        MCRUser junit2 = MCRUserManager.getUser("junit2", MCRRealmFactory.getLocalRealm());
        assertEquals(3, junit2.getAttributes().size());
        startNewTransaction();

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
        assertNull("Should not find user", MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm()));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#deleteUser(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testDeleteUserMCRUser() {
        MCRUserManager.deleteUser(user);
        assertNull("Should not find user", MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm()));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#listUsers(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testListUsersMCRUser() {
        List<MCRUser> listUsers = MCRUserManager.listUsers(null);
        assertEquals("Should not find a user", 0, listUsers.size());
        user.setOwner(user);
        MCRUserManager.updateUser(user);
        startNewTransaction();
        listUsers = MCRUserManager.listUsers(user);
        assertEquals("Could not find a user", 1, listUsers.size());
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#listUsers(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testListUsersStringStringString() {
        List<MCRUser> listUsers = MCRUserManager.listUsers(null, null, "Test*", null);
        assertEquals("Could not find a user", 1, listUsers.size());
    }

    /**
     * Test method buildCondition with user attribute name pattern
     */
    @Test
    public final void testConditionWithUserAttributeNamePattern() {
        assertEquals("Attribute name search failed", 0, MCRUserManager.countUsers(null, null, null, null,
            "bar", null));

        MCRUser user2 = new MCRUser("junit2");
        user2.setRealName("Test Case II");
        user2.setUserAttribute("junit2", "foo");
        user2.setUserAttribute("bar", "test");
        MCRUserManager.createUser(user2);
        assertEquals("Attribute name search failed", 1, MCRUserManager.countUsers(null, null, null, null,
            "bar", null));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#countUsers(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testCountUsers() {
        assertEquals("Could not find a user", 1, MCRUserManager.countUsers(null, null, "*Case*", null));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#login(java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testLogin() {
        String clearPasswd = user.getPassword();
        Date curTime = new Date();
        MCRUser user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNull("Should not login user", user);
        MCRUserManager.setUserPassword(this.user, clearPasswd);
        MCRUserManager.updateUser(this.user);
        startNewTransaction();
        user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNotNull("Could not login user", user);
        assertNotNull("Hash value was not updated", user.getHashType());
        user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNotNull("No date set for last login.", user.getLastLogin());
        assertTrue("Date was not updated", curTime.before(user.getLastLogin()));
        user.disableLogin();
        MCRUserManager.updateUser(user);
        startNewTransaction();
        assertNull("Should not login user when account is disabled",
            MCRUserManager.login(this.user.getUserName(), clearPasswd));
        user.enableLogin();
        user.setValidUntil(new Date());
        MCRUserManager.updateUser(user);
        startNewTransaction();
        assertNull("Should not login user when password is expired",
            MCRUserManager.login(this.user.getUserName(), clearPasswd));
    }

    @Test
    public final void toXML() throws IOException {
        MCRUserManager.setUserPassword(this.user, this.user.getPassword());
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
        startNewTransaction();
        assertEquals("Too many users", 1, MCRUserManager.countUsers(null, null, null, null));
        assertEquals("Too many users", 1, MCRUserManager.listUsers(this.user).size());
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

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.URIResolver.CachingResolver.Capacity", "0");
        testProperties.put("MCR.URIResolver.CachingResolver.MaxAge", "0");
        return testProperties;
    }
}
