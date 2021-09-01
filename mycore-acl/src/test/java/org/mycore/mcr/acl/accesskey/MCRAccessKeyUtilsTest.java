/*
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

package org.mycore.mcr.acl.accesskey;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.user2.MCRUser;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

public class MCRAccessKeyUtilsTest extends MCRJPATestCase {

    private static final String OBJECT_ID = "mcr_test_00000001";

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private MCRObjectID objectId;

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectId = MCRObjectID.getInstance(OBJECT_ID);
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
    }

    @Test(expected = MCRAccessKeyNotFoundException.class)
    public void testUnkownKey() {
        MCRAccessKeyUtils.addAccessKeyToCurrentUser(objectId, READ_KEY);
    }

    @Test
    public void testUser() {
        final MCRAccessKey accessKey = new MCRAccessKey(objectId, READ_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.addAccessKey(accessKey);
        MCRAccessKeyUtils.addAccessKeyToCurrentUser(objectId, READ_KEY);
        assertNotNull(MCRAccessKeyUtils.getAccessKeyValueFromCurrentUser(objectId));
        assertNotNull(MCRAccessKeyUtils.getAccessKeyFromCurrentUser(objectId));
        MCRAccessKeyUtils.deleteAccessKeyFromCurrentUser(objectId);
        assertNull(MCRAccessKeyUtils.getAccessKeyValueFromCurrentUser(objectId));
        assertNull(MCRAccessKeyUtils.getAccessKeyFromCurrentUser(objectId));
    }

    @Test
    public void testOverride() {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(objectId, READ_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.addAccessKey(accessKeyRead);
        MCRAccessKeyUtils.addAccessKeyToCurrentUser(objectId, READ_KEY);
        final String readValue = MCRAccessKeyUtils.getAccessKeyValueFromCurrentUser(objectId);
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(objectId, WRITE_KEY, PERMISSION_WRITE); 
        MCRAccessKeyManager.addAccessKey(accessKeyWrite);
        MCRAccessKeyUtils.addAccessKeyToCurrentUser(objectId, WRITE_KEY);
        assertNotEquals(readValue, MCRAccessKeyUtils.getAccessKeyValueFromCurrentUser(objectId));
    }
}
