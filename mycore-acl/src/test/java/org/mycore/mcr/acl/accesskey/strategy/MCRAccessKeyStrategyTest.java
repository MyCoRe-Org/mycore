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

package org.mycore.mcr.acl.accesskey.strategy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTestCase;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUtils;
import org.mycore.mcr.acl.accesskey.config.MCRAccessKeyConfig;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.user2.MCRUser;

public class MCRAccessKeyStrategyTest extends MCRAccessKeyTestCase {

    private static final String READ_VALUE = "bla";

    private static final String WRITE_VALUE = "blu";

    private MCRAccessKeyStrategy strategy;

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
        strategy = new MCRAccessKeyStrategy();
    }

    @Test
    public void testDefaultPermission() {
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), readKey);
        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);

        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));
    }

    @Test
    public void testSessionFilter() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));

        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read");

        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));

        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "");

        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));

        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "writedb");

        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
    }

    @Test
    public void testObjectFilter() {
        final MCRAccessKey derivateKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), derivateKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));

        final MCRAccessKey objectKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), objectKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));

        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");

        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));

        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "");

        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));

        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "derivate");

        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
    }

    @Test
    public void testPermissionInheritance() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));
    }

    @Test
    public void testObjectSession() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));
    }

    @Test
    public void testObjectUser() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));
    }

    @Test
    public void testDerivateSession() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), READ_VALUE);

        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), WRITE_VALUE);

        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));
    }

    @Test
    public void testDerivateUser() {
        MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), readKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivate.getId(), READ_VALUE);

        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivate.getId(), WRITE_VALUE);

        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), PERMISSION_WRITE));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));
    }

    @Test
    public void checkDominance() {
        MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), WRITE_VALUE);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read");

        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));

        final MCRAccessKey writeKey2 = new MCRAccessKey(READ_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey2);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivate.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_READ));
        assertTrue(strategy.checkPermission(derivate.getId().toString(), PERMISSION_WRITE));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }
}
