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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.user2.login.MCRShibbolethUserInformation;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * @author René Adler (eagle)
 *
 */
public class MCRUserAttributeMapperTest extends MCRUserTestCase {

    private static String realmId = "mycore.de";

    private static String roles = "staff,member";

    private MCRUser mcrUser;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mcrUser = MCRUserTransformer.buildMCRUser(MCRURIResolver.obtainInstance().resolve("resource:test-user.xml"));
    }

    @Test
    public void testUserMapping() throws Exception {
        MCRUserAttributeMapper attributeMapper = MCRRealmFactory.getAttributeMapper(realmId);

        assertNotNull("no attribute mapping defined", attributeMapper);

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", mcrUser.getEMailAddress());
        attributes.put("eduPersonAffiliation", roles);

        MCRUser user = new MCRUser(null, realmId);
        attributeMapper.mapAttributes(user, attributes);

        assertEquals(mcrUser.getUserName(), user.getUserName());
        assertEquals(mcrUser.getRealName(), user.getRealName());
        assertEquals(mcrUser.getEMailAddress(), user.getEMailAddress());
        assertTrue(user.isUserInRole("editor"));
    }

    @Test
    public void testUserInformationMapping() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", mcrUser.getEMailAddress());
        attributes.put("eduPersonAffiliation", roles);

        MCRUserInformation userInfo = new MCRShibbolethUserInformation(mcrUser.getUserName(), realmId, attributes);

        MCRTransientUser user = new MCRTransientUser(userInfo);

        assertEquals(mcrUser.getUserName(), user.getUserName());
        assertEquals(mcrUser.getRealName(), user.getRealName());
        assertEquals(mcrUser.getEMailAddress(), user.getEMailAddress());
        assertTrue(user.isUserInRole("editor"));
    }

    @Test
    public void testUserCreate() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", mcrUser.getEMailAddress());
        attributes.put("eduPersonAffiliation", roles);

        MCRUserInformation userInfo = new MCRShibbolethUserInformation(mcrUser.getUserName(), realmId, attributes);

        MCRTransientUser user = new MCRTransientUser(userInfo);

        assertEquals(mcrUser.getUserName(), user.getUserName());
        assertEquals(mcrUser.getRealName(), user.getRealName());
        assertTrue(user.isUserInRole("editor"));

        Map<String, String> extraAttribs = new HashMap<>();
        extraAttribs.put("attrib1", "test123");
        extraAttribs.put("attrib2", "test321");
        extraAttribs.forEach((key, value) -> user.setUserAttribute(key, value));

        MCRUserManager.createUser(user);

        startNewTransaction();

        MCRUser storedUser = MCRUserManager.getUser(user.getUserName(), realmId);

        assertEquals(mcrUser.getEMailAddress(), storedUser.getEMailAddress());

        assertEquals(extraAttribs.get("attrib1"), storedUser.getUserAttribute("attrib1"));
        assertEquals(extraAttribs.get("attrib2"), storedUser.getUserAttribute("attrib2"));

        Document exportableXML = MCRUserTransformer.buildExportableXML(storedUser);
        new XMLOutputter(Format.getPrettyFormat()).output(exportableXML, System.out);
    }

    @Test
    public void testUserUpdate() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", mcrUser.getEMailAddress());
        attributes.put("eduPersonAffiliation", roles);

        MCRUserInformation userInfo = new MCRShibbolethUserInformation(mcrUser.getUserName(), realmId, attributes);

        MCRTransientUser user = new MCRTransientUser(userInfo);

        assertEquals(mcrUser.getUserName(), user.getUserName());
        assertEquals(mcrUser.getRealName(), user.getRealName());
        assertTrue(user.isUserInRole("editor"));

        Map<String, String> extraAttribs = new HashMap<>();
        extraAttribs.put("attrib1", "test123");
        extraAttribs.put("attrib2", "test321");
        extraAttribs.forEach((key, value) -> user.setUserAttribute(key, value));

        MCRUserManager.createUser(user);

        startNewTransaction();

        attributes = new HashMap<>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", "new@mycore.de");
        attributes.put("eduPersonAffiliation", "admin");

        MCRUser storedUser = MCRUserManager.getUser(user.getUserName(), realmId);

        MCRUserAttributeMapper attributeMapper = MCRRealmFactory.getAttributeMapper(realmId);
        boolean changed = attributeMapper.mapAttributes(storedUser, attributes);

        assertTrue("should changed", changed);
        assertNotEquals(user.getEMailAddress(), storedUser.getEMailAddress());

        Document exportableXML = MCRUserTransformer.buildExportableXML(storedUser);
        new XMLOutputter(Format.getPrettyFormat()).output(exportableXML, System.out);
    }
}
