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

package org.mycore.mcr.acl.accesskey;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.config.MCRAccessKeyConfig;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRAccessKeyUtilsTest extends MCRAccessKeyTestCase {

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private MCRObject object = null;

    private MCRDerivate derivate = null;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        object = createObject();
        MCRMetadataManager.create(object);
        derivate = createDerivate(object.getId());
        MCRMetadataManager.create(derivate);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object,derivate");
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read,writedb");

    }

    @Test(expected = MCRAccessKeyException.class)
    public void testUnkownKey() {
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), READ_KEY);
    }

    @Test
    public void testSession() {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), accessKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), READ_KEY);
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(object.getId()));
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(derivate.getId()));
        MCRAccessKeyUtils.removeAccessKeySecretFromCurrentSession(object.getId());
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(object.getId()));
        final MCRAccessKey accessKeyDerivate = new MCRAccessKey(WRITE_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), accessKeyDerivate);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), WRITE_KEY);
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(object.getId()));
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(derivate.getId()));
    }

    @Test
    public void testUser() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), accessKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), READ_KEY);
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(object.getId()));
        MCRAccessKeyUtils.removeAccessKeySecretFromCurrentUser(object.getId());
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(object.getId()));
        final MCRAccessKey accessKeyDerivate = new MCRAccessKey(WRITE_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), accessKeyDerivate);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), WRITE_KEY);
        assertNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(object.getId()));
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(derivate.getId()));
    }

    @Test
    public void testSessionOverride() {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), accessKeyRead);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), READ_KEY);
        final String readSecret = MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(object.getId());
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(WRITE_KEY, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), accessKeyWrite);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), WRITE_KEY);
        assertNotEquals(readSecret, MCRAccessKeyUtils.getAccessKeySecretFromCurrentSession(object.getId()));
    }

    @Test
    public void testUserOverride() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        final MCRAccessKey accessKeyRead = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), accessKeyRead);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), READ_KEY);
        final String readSecret = MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(object.getId());
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(WRITE_KEY, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), accessKeyWrite);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), WRITE_KEY);
        assertNotEquals(readSecret, MCRAccessKeyUtils.getAccessKeySecretFromCurrentUser(object.getId()));
    }

    @Test
    public void testCleanUpUserAttributes() {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), accessKey);

        final MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRAccessKeyUtils.addAccessKeySecret(MCRUserManager.getUser("junit"), object.getId(), READ_KEY);

        final MCRAccessKey accessKey2 = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        final MCRObjectID objectId2 = MCRObjectID.getInstance("mcr_object_00000002");
        MCRAccessKeyManager.createAccessKey(objectId2, accessKey2);
        MCRAccessKeyUtils.addAccessKeySecret(MCRUserManager.getUser("junit"), objectId2, READ_KEY);

        final MCRUser user1 = new MCRUser("junit1");
        MCRUserManager.createUser(user1);
        MCRAccessKeyUtils.addAccessKeySecret(MCRUserManager.getUser("junit1"), object.getId(), READ_KEY);

        MCRAccessKeyManager.removeAccessKey(object.getId(), MCRAccessKeyManager.hashSecret(READ_KEY, object.getId()));
        MCRAccessKeyUtils.cleanUpUserAttributes();

        assertNull(MCRAccessKeyUtils.getAccessKeySecret(MCRUserManager.getUser("junit"), object.getId()));
        assertNull(MCRAccessKeyUtils.getAccessKeySecret(MCRUserManager.getUser("junit1"), object.getId()));
        assertNotNull(MCRAccessKeyUtils.getAccessKeySecret(MCRUserManager.getUser("junit"), objectId2));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }
}
