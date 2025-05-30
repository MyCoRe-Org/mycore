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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

@Deprecated
public class MCRAccessKeyManagerTest extends MCRJPATestCase {

    private static final String OBJECT_ID = "mcr_test_00000001";

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private static MCRObjectID objectId;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectId = MCRObjectID.getInstance(OBJECT_ID);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test(expected = MCRAccessKeyCollisionException.class)
    public void testKeyAddCollison() {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(WRITE_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKeyRead);
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(WRITE_KEY, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(objectId, accessKeyWrite);
    }

    @Test
    public void testCreateKey() throws MCRException {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKeyRead);
        final MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyWithSecret(objectId,
            MCRAccessKeyManager.hashSecret(READ_KEY, objectId));
        assertNotNull(accessKey);
    }

    @Test(expected = MCRAccessKeyCollisionException.class)
    public void testDuplicate() {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        final MCRAccessKey accessKeySame = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKeySame);
    }

    @Test
    public void testExistsKey() throws MCRException {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        assertTrue(MCRAccessKeyManager.listAccessKeys(objectId).size() > 0);
    }

    @Test
    public void testUpdateType() throws MCRException {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        final MCRAccessKey accessKeyNew = new MCRAccessKey(null, PERMISSION_WRITE);
        MCRAccessKeyManager.updateAccessKey(objectId, MCRAccessKeyManager.hashSecret(READ_KEY, objectId),
            accessKeyNew);
        final MCRAccessKey accessKeyUpdated = MCRAccessKeyManager.listAccessKeys(objectId).getFirst();
        assertEquals(accessKeyNew.getType(), accessKeyUpdated.getType());
    }

    @Test
    public void testUpdateIsActive() throws MCRException {
        MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        accessKey.setIsActive(false);
        MCRAccessKeyManager.updateAccessKey(objectId, MCRAccessKeyManager.hashSecret(READ_KEY, objectId), accessKey);
        accessKey = MCRAccessKeyManager.listAccessKeys(objectId).getFirst();
        assertEquals(accessKey.getIsActive(), false);
        accessKey.setIsActive(true);
        MCRAccessKeyManager.updateAccessKey(objectId, MCRAccessKeyManager.hashSecret(READ_KEY, objectId), accessKey);
        accessKey = MCRAccessKeyManager.listAccessKeys(objectId).getFirst();
        assertEquals(accessKey.getIsActive(), true);
    }

    @Test
    public void testDeleteKey() throws MCRException {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        MCRAccessKeyManager.removeAccessKey(objectId, MCRAccessKeyManager.hashSecret(READ_KEY, objectId));
        assertFalse(MCRAccessKeyManager.listAccessKeys(objectId).size() > 0);
    }
}
