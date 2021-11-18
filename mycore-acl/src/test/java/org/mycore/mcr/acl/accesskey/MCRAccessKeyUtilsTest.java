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

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRAccessKeyUtilsTest extends MCRAccessKeyTestCase {

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private MCRObjectID objectId = null;

    private MCRObjectID derivateId = null;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        objectId = getObject().getId();
        derivateId = getDerivate().getId();
    }

    @Test(expected = MCRAccessKeyException.class)
    public void testUnkownKey() {
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(objectId, READ_KEY);
    }

    @Test
    public void testSession() {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, READ_KEY);
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(objectId));
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(derivateId));
        MCRAccessKeyUtils.removeAccessKeySecretFromCurrentSession(objectId);
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(objectId));
        final MCRAccessKey accessKeyDerivate = new MCRAccessKey(WRITE_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.createAccessKey(derivateId, accessKeyDerivate);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, WRITE_KEY);
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(objectId));
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(derivateId));
    }

    @Test
    public void testUser() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(objectId, READ_KEY);
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(objectId));
        MCRAccessKeyUtils.removeAccessKeySecretFromCurrentUser(objectId);
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(objectId));
        final MCRAccessKey accessKeyDerivate = new MCRAccessKey(WRITE_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.createAccessKey(derivateId, accessKeyDerivate);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(objectId, WRITE_KEY);
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(objectId));
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(derivateId));
    }

    @Test
    public void testSessionOverride() {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(READ_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.createAccessKey(objectId, accessKeyRead);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, READ_KEY);
        final String readSecret = MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(objectId);
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(WRITE_KEY, PERMISSION_WRITE); 
        MCRAccessKeyManager.createAccessKey(objectId, accessKeyWrite);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, WRITE_KEY);
        assertNotEquals(readSecret, MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(objectId));
    }

    @Test
    public void testUserOverride() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        final MCRAccessKey accessKeyRead = new MCRAccessKey(READ_KEY, PERMISSION_READ); 
        MCRAccessKeyManager.createAccessKey(objectId, accessKeyRead);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(objectId, READ_KEY);
        final String readSecret = MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(objectId);
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(WRITE_KEY, PERMISSION_WRITE); 
        MCRAccessKeyManager.createAccessKey(objectId, accessKeyWrite);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(objectId, WRITE_KEY);
        assertNotEquals(readSecret, MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(objectId));
    }

    @Test
    public void testCleanUpUserAttributes() {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);

        final MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRAccessKeyUtils.addAccessKeySecret(MCRUserManager.getUser("junit"), objectId, READ_KEY);

        final MCRAccessKey accessKey2 = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        final MCRObjectID objectId2 = MCRObjectID.getInstance("mcr_object_00000002");
        MCRAccessKeyManager.createAccessKey(objectId2, accessKey2);
        MCRAccessKeyUtils.addAccessKeySecret(MCRUserManager.getUser("junit"), objectId2, READ_KEY);

        final MCRUser user1 = new MCRUser("junit1");
        MCRUserManager.createUser(user1);
        MCRAccessKeyUtils.addAccessKeySecret(MCRUserManager.getUser("junit1"), objectId, READ_KEY);

        MCRAccessKeyManager.removeAccessKey(objectId, MCRAccessKeyManager.hashSecret(READ_KEY, objectId));
        MCRAccessKeyUtils.cleanUpUserAttributes();

        assertNull(MCRAccessKeyUtils.getAccessKeySecret(MCRUserManager.getUser("junit"), objectId));
        assertNull(MCRAccessKeyUtils.getAccessKeySecret(MCRUserManager.getUser("junit1"), objectId));
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecret(MCRUserManager.getUser("junit"), objectId2));
    }
}
