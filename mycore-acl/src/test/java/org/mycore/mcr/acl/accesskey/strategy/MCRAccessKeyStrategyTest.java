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
import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_PREVIEW;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_VIEW;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.Date;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUtils;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.user2.MCRUser;

public class MCRAccessKeyStrategyTest extends MCRStoreTestCase {
    
    private static final String OBJECT_ID = "mcr_object_00000001";

    private static final String DERIVATE_ID = "mcr_derivate_00000001";

    private static MCRObject object;

    private static MCRDerivate derivate;

    private static final String READ_VALUE = "bla";
    
    private static final String WRITE_VALUE = "blu";

    private MCRAccessKeyStrategy strategy;

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties
            .put("MCR.Persistence.LinkTable.Store.Class", "org.mycore.backend.hibernate.MCRHIBLinkTableStore");
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.AccessKey.Session.AllowedPermissionTypes", "read");
        testProperties.put("MCR.Metadata.Type.document", "true");
        testProperties.put("MCR.Metadata.Type.object", Boolean.TRUE.toString());
        testProperties.put("MCR.Metadata.Type.derivate", Boolean.TRUE.toString());
        testProperties.put("MCR.Metadata.ObjectID.NumberPattern", "00000000");
        return testProperties;
    }
    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        strategy = new MCRAccessKeyStrategy();

        object = new MCRObject();
        object.setSchema("noSchema");
        MCRObjectID objectId = MCRObjectID.getInstance(OBJECT_ID);
        object.setId(objectId);
        MCRMetadataManager.create(object);

        derivate = new MCRDerivate();
        derivate.setSchema("datamodel-derivate.xsd");
        MCRObjectID derivateId = MCRObjectID.getInstance(DERIVATE_ID);
        derivate.setId(derivateId);
        MCRMetaIFS ifs = new MCRMetaIFS("internal", null);
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID metaLinkID = new MCRMetaLinkID("internal", 0);
        metaLinkID.setReference(objectId.toString(), null, null);
        derivate.getDerivate().setLinkMeta(metaLinkID);
        MCRMetadataManager.create(derivate);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }

    @Test
    public void testDefaultPermission() {
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));
    }

    @Test
    public void testObjectSession() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), readKey);
        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);

        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));

        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));

        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(object.getId(), WRITE_VALUE); //test filter

        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
    }

    @Test
    public void testIsActive() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));

        writeKey.setIsActive(false);
        MCRAccessKeyManager.updateAccessKey(object.getId(), 
            MCRAccessKeyManager.hashSecret(WRITE_VALUE, object.getId()), writeKey);
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));

        writeKey.setIsActive(true);
        MCRAccessKeyManager.updateAccessKey(object.getId(), 
            MCRAccessKeyManager.hashSecret(WRITE_VALUE, object.getId()), writeKey);
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
    }

    @Test
    public void testExpiration() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);
        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));

        writeKey.setExpiration(new Date()); //now
        MCRAccessKeyManager.updateAccessKey(object.getId(), 
            MCRAccessKeyManager.hashSecret(WRITE_VALUE, object.getId()), writeKey);
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));

        writeKey.setExpiration(new Date(new Date().getTime() + (1000 * 60 * 60 * 24))); //tomorrow
        MCRAccessKeyManager.updateAccessKey(object.getId(), 
            MCRAccessKeyManager.hashSecret(WRITE_VALUE, object.getId()), writeKey);
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
    }

    @Test
    public void testObjectUser() {
        final MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(object.getId(), readKey);
        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(object.getId(), writeKey);

        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));

        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));

        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(object.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
    }

    @Test
    public void testDerivateSession() {
        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), readKey);
        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey);

        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), READ_VALUE);

        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));

        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), WRITE_VALUE);

        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
    }
    
    @Test
    public void testDerivateUser() {
        MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey readKey = new MCRAccessKey(READ_VALUE, PERMISSION_READ);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), readKey);
        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey);

        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivate.getId(), READ_VALUE);

        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));

        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivate.getId(), WRITE_VALUE);

        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_DELETE));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_PREVIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_READ));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(OBJECT_ID, PERMISSION_WRITE));

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));
    }

    @Test
    public void checkDominance() {
        MCRUser user = new MCRUser("junit");
        MCRSessionMgr.getCurrentSession().setUserInformation(user);

        final MCRAccessKey writeKey = new MCRAccessKey(WRITE_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey);

        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(derivate.getId(), WRITE_VALUE);

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));

        final MCRAccessKey writeKey2 = new MCRAccessKey(READ_VALUE, PERMISSION_WRITE);
        MCRAccessKeyManager.createAccessKey(derivate.getId(), writeKey2);

        MCRAccessKeyUtils.addAccessKeySecretToCurrentUser(derivate.getId(), READ_VALUE);

        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_READ));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_PREVIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_VIEW));
        assertTrue(strategy.checkPermission(DERIVATE_ID, PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(DERIVATE_ID, PERMISSION_DELETE));

    }
}
