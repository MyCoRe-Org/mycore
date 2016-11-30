/*
 * $Id$ 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
 * @author Ren\u00E9 Adler (eagle)
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

        mcrUser = MCRUserTransformer.buildMCRUser(MCRURIResolver.instance().resolve("resource:test-user.xml"));
    }

    @Test
    public void testUserMapping() throws Exception {
        MCRUserAttributeMapper attributeMapper = MCRRealmFactory.getAttributeMapper(realmId);

        assertNotNull("no attribute mapping defined", attributeMapper);

        HashMap<String, Object> attributes = new HashMap<String, Object>();
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
        Map<String, Object> attributes = new HashMap<String, Object>();
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
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", mcrUser.getEMailAddress());
        attributes.put("eduPersonAffiliation", roles);

        MCRUserInformation userInfo = new MCRShibbolethUserInformation(mcrUser.getUserName(), realmId, attributes);

        MCRTransientUser user = new MCRTransientUser(userInfo);

        assertEquals(mcrUser.getUserName(), user.getUserName());
        assertEquals(mcrUser.getRealName(), user.getRealName());
        assertTrue(user.isUserInRole("editor"));

        Map<String, String> extraAttribs = new HashMap<String, String>();
        extraAttribs.put("attrib1", "test123");
        extraAttribs.put("attrib2", "test321");
        user.setAttributes(extraAttribs);

        MCRUserManager.createUser(user);

        startNewTransaction();

        MCRUser storedUser = MCRUserManager.getUser(user.getUserName(), realmId);

        assertEquals(mcrUser.getEMailAddress(), storedUser.getEMailAddress());

        assertEquals(extraAttribs.get("attrib1"), storedUser.getAttributes().get("attrib1"));
        assertEquals(extraAttribs.get("attrib2"), storedUser.getAttributes().get("attrib2"));

        Document exportableXML = MCRUserTransformer.buildExportableXML(storedUser);
        new XMLOutputter(Format.getPrettyFormat()).output(exportableXML, System.out);
    }

    @Test
    public void testUserUpdate() throws Exception {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", mcrUser.getEMailAddress());
        attributes.put("eduPersonAffiliation", roles);

        MCRUserInformation userInfo = new MCRShibbolethUserInformation(mcrUser.getUserName(), realmId, attributes);

        MCRTransientUser user = new MCRTransientUser(userInfo);

        assertEquals(mcrUser.getUserName(), user.getUserName());
        assertEquals(mcrUser.getRealName(), user.getRealName());
        assertTrue(user.isUserInRole("editor"));

        Map<String, String> extraAttribs = new HashMap<String, String>();
        extraAttribs.put("attrib1", "test123");
        extraAttribs.put("attrib2", "test321");
        user.setAttributes(extraAttribs);

        MCRUserManager.createUser(user);

        startNewTransaction();

        attributes = new HashMap<String, Object>();
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
