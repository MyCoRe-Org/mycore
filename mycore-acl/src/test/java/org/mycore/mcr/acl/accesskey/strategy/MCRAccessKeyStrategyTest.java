/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.mcr.acl.accesskey.strategy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTestCase;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUtils;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.user2.MCRUser;

public class MCRAccessKeyStrategyTest extends MCRAccessKeyTestCase {

    private static final String READ_VALUE = "bla";
    
    private static final String WRITE_VALUE = "blu";

    private MCRAccessKeyStrategy strategy;

    private MCRObjectID objectId = null;

    private MCRObjectID derivateId = null;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        objectId = getObject().getId();
        derivateId = getDerivate().getId();
        strategy = new MCRAccessKeyStrategy();
    }

    @Test
    public void testDefaultPermission() {
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, readKey);
        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(objectId, writeKey);

        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));
    }

    @Test
    public void testSessionFilter() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, READ_VALUE);

        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(objectId, writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, WRITE_VALUE);

        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));

        MCRConfiguration2.set(ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read");

        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));

        MCRConfiguration2.set(ALLOWED_SESSION_PERMISSION_TYPES_PROP, "");

        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));

        MCRConfiguration2.set(ALLOWED_SESSION_PERMISSION_TYPES_PROP, "writedb");

        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
    }

    @Test
    public void testObjectFilter() {
        final MCRAccessKey derivateKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivateId, derivateKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivateId, READ_VALUE);

        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));

        final MCRAccessKey objectKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, objectKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, READ_VALUE);

        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));

        MCRConfiguration2.set(ALLOWED_OBJECT_TYPES_PROP, "object");

        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));

        MCRConfiguration2.set(ALLOWED_OBJECT_TYPES_PROP, "");

        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));

        MCRConfiguration2.set(ALLOWED_OBJECT_TYPES_PROP, "derivate");

        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
    }

    @Test
    public void testPermissionInheritance() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivateId, readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivateId, READ_VALUE);

        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(objectId, writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, WRITE_VALUE);

        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));
    }

    @Test
    public void testObjectSession() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, READ_VALUE);

        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(objectId, writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, WRITE_VALUE);

        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));
    }

    @Test
    public void testObjectUser() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(objectId, readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(objectId, READ_VALUE);

        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(objectId, writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(objectId, WRITE_VALUE);

        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));
    }

    @Test
    public void testDerivateSession() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivateId, readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivateId, READ_VALUE);

        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivateId, writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivateId, WRITE_VALUE);

        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));
    }
    
    @Test
    public void testDerivateUser() {
        MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivateId, readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivateId, READ_VALUE);

        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivateId, writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivateId, WRITE_VALUE);

        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(objectId.toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));
    }

    @Test
    public void checkDominance() {
        MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivateId, writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivateId, WRITE_VALUE);
        MCRConfiguration2.set(ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read");

        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey2 = new MCRAccessKey(READ_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivateId, writeKey2);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivateId, READ_VALUE);

        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivateId.toString(), PERMISSION_WRITE));
    }
}
