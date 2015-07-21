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
package org.mycore.user2.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttributeMapper;
import org.mycore.user2.MCRUserTestCase;
import org.mycore.user2.login.MCRShibbolethUserInformation;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRUserAttributeMapperTest extends MCRUserTestCase {

    private static String realmId = "mycore.de";

    private MCRUser mcrUser;

    private String roles;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mcrUser = MCRUserTransformer.buildMCRUser(MCRURIResolver.instance().resolve("resource:test-user.xml"));

        roles = "";
        for (String role : mcrUser.getSystemRoleIDs()) {
            if (!roles.isEmpty())
                roles += ",";

            roles += role;
        }
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
        assertEquals(mcrUser.getSystemRoleIDs(), user.getSystemRoleIDs());
    }

    @Test
    public void testUserInformationMapping() throws Exception {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("eduPersonPrincipalName", mcrUser.getUserName() + "@" + realmId);
        attributes.put("displayName", mcrUser.getRealName());
        attributes.put("mail", mcrUser.getEMailAddress());
        attributes.put("eduPersonAffiliation", roles);

        MCRUserInformation userInfo = new MCRShibbolethUserInformation(mcrUser.getUserID(), realmId, attributes);

        MCRTransientUser user = new MCRTransientUser(userInfo);

        assertEquals(mcrUser.getUserName(), user.getUserName());
        assertEquals(mcrUser.getRealName(), user.getRealName());
        assertEquals(mcrUser.getSystemRoleIDs(), user.getSystemRoleIDs());
    }
}
