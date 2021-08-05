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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import java.util.Map;

import org.junit.Test;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.backend.jpa.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;

public class MCRAccessKeyManagerTest extends MCRJPATestCase {

    private static final String MCR_OBJECT_ID = "mcr_test_00000001";

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private static final MCRObjectID objectId = MCRObjectID.getInstance(MCR_OBJECT_ID);

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test(expected = MCRAccessKeyCollisionException.class)
    public void testKeyAddCollison() {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(objectId, WRITE_KEY, 
            MCRAccessManager.PERMISSION_READ);
        MCRAccessKeyManager.addAccessKey(accessKeyRead);
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(objectId, WRITE_KEY, 
            MCRAccessManager.PERMISSION_WRITE);
        MCRAccessKeyManager.addAccessKey(accessKeyWrite);
    }

    @Test
    public void testCreateKey() throws MCRException, IOException {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(objectId, READ_KEY, 
                MCRAccessManager.PERMISSION_READ);
        MCRAccessKeyManager.addAccessKey(accessKeyRead);
        final MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyByValue(objectId, 
                MCRAccessKeyManager.encryptValue(READ_KEY, objectId));
        assertTrue(accessKey != null);
    }

    @Test(expected = MCRAccessKeyCollisionException.class)
    public void testDuplicate() {
        final MCRAccessKey accessKey = new MCRAccessKey(objectId, READ_KEY, 
            MCRAccessManager.PERMISSION_READ);
        MCRAccessKeyManager.addAccessKey(accessKey);
        final MCRAccessKey accessKeySame = new MCRAccessKey(objectId, READ_KEY, 
            MCRAccessManager.PERMISSION_READ);
        MCRAccessKeyManager.addAccessKey(accessKeySame);
    }

    @Test
    public void testExistsKey() throws MCRException {
        final MCRAccessKey accessKey = new MCRAccessKey(objectId, READ_KEY, 
            MCRAccessManager.PERMISSION_READ);
        MCRAccessKeyManager.addAccessKey(accessKey);
        assertTrue(MCRAccessKeyManager.getAccessKeys(objectId).size() > 0);
    }

    @Test
    public void testUpdateType() throws MCRException {
        final MCRAccessKey accessKey = new MCRAccessKey(objectId, READ_KEY, 
            MCRAccessManager.PERMISSION_READ);
        MCRAccessKeyManager.addAccessKey(accessKey);
        final MCRAccessKey accessKeyNew = new MCRAccessKey(objectId, accessKey.getValue(), 
            MCRAccessManager.PERMISSION_WRITE);
        MCRAccessKeyManager.updateAccessKey(accessKeyNew);
        final MCRAccessKey accessKeyUpdated = MCRAccessKeyManager
            .getAccessKeys(objectId).get(0);
        assertEquals(accessKeyNew.getType(), accessKeyUpdated.getType());
    }

    @Test
    public void testDeleteKey() throws MCRAccessException {
        final MCRAccessKey accessKey = new MCRAccessKey(objectId, READ_KEY, 
            MCRAccessManager.PERMISSION_READ);
        MCRAccessKeyManager.addAccessKey(accessKey);
        MCRAccessKeyManager.deleteAccessKey(objectId,
            MCRAccessKeyManager.encryptValue(READ_KEY, objectId));
        assertFalse(MCRAccessKeyManager.getAccessKeys(objectId).size() > 0);
    }
}
