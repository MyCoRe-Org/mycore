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

package org.mycore.mcr.acl.accesskey.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTestCase;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRAccessKeyUserServiceImplTest extends MCRAccessKeyTestCase {

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private static final String DELETE_KEY = "delete";

    private static final String UNKNOWN_KEY = "unknown";

    private MCRObject object;

    private MCRDerivate derivate;

    private MCRObjectID unknownObjectId;

    private MCRAccessKeyService accessKeyServiceMock;

    private MCRAccessKeyUserService userService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        object = createObject();
        MCRMetadataManager.create(object);
        derivate = createDerivate(object.getId());
        MCRMetadataManager.create(derivate);
        unknownObjectId = MCRObjectID.getInstance("mcr_object_00000002");
        accessKeyServiceMock = Mockito.mock(MCRAccessKeyService.class);
        Mockito.when(accessKeyServiceMock.processSecret(object.getId().toString(), READ_KEY)).thenReturn(READ_KEY);
        Mockito.when(accessKeyServiceMock.processSecret(derivate.getId().toString(), WRITE_KEY)).thenReturn(WRITE_KEY);
        Mockito.when(accessKeyServiceMock.processSecret(object.getId().toString(), UNKNOWN_KEY)).thenReturn(UNKNOWN_KEY);
        Mockito.when(accessKeyServiceMock.processSecret(object.getId().toString(), DELETE_KEY)).thenReturn(DELETE_KEY);
        final MCRAccessKeyDto readAccessKeyDto = new MCRAccessKeyDto();
        readAccessKeyDto.setReference(object.getId().toString());
        readAccessKeyDto.setSecret(READ_KEY);
        readAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_READ);
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setSecret(WRITE_KEY);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        final MCRAccessKeyDto deleteAccessKeyDto = new MCRAccessKeyDto();
        deleteAccessKeyDto.setReference(object.getId().toString());
        deleteAccessKeyDto.setSecret(DELETE_KEY);
        deleteAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_DELETE);
        Mockito.when(accessKeyServiceMock.findAccessKeyByReferenceAndSecret(object.getId().toString(), READ_KEY))
            .thenReturn(readAccessKeyDto);
        Mockito.when(accessKeyServiceMock.findAccessKeyByReferenceAndSecret(derivate.getId().toString(), WRITE_KEY))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(accessKeyServiceMock.findAccessKeyByReferenceAndSecret(object.getId().toString(), UNKNOWN_KEY))
            .thenReturn(null);
        userService = new MCRAccessKeyUserService(accessKeyServiceMock);
        final MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
    }

    @Test
    public void testSetup() {
        assertTrue(MCRMetadataManager.exists(object.getId()));
        assertTrue(MCRMetadataManager.exists(derivate.getId()));
    }

    @Test
    public void testActivateAccessKeyForReferenceByRawValue_object() {
        userService.activateAccessKey(object.getId().toString(), READ_KEY);
        assertEquals(READ_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).findAccessKeyByReferenceAndSecret(
            object.getId().toString(), READ_KEY);
    }

    @Test
    public void testActivateAccessKeyForReferenceByRawValue_derivate() {
        userService.activateAccessKey(derivate.getId().toString(), WRITE_KEY);
        assertEquals(WRITE_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + derivate.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).findAccessKeyByReferenceAndSecret(
            derivate.getId().toString(), WRITE_KEY);
    }

    @Test
    public void testActivateAccessKeyForReferenceByRawValue_unknownObject() {
        assertThrows(MCRAccessKeyException.class,
            () -> userService.activateAccessKey(unknownObjectId.toString(), WRITE_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(0)).findAccessKeyByReferenceAndSecret(Mockito.anyString(),
            Mockito.anyString());
    }

    @Test
    public void testActivateAccessKeyForReferenceByRawValue_unknownAccessKey() {
        assertThrows(MCRAccessKeyException.class,
            () -> userService.activateAccessKey(object.getId().toString(), UNKNOWN_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).findAccessKeyByReferenceAndSecret(
            object.getId().toString(), UNKNOWN_KEY);
    }

    @Test
    public void testActivateAccessKeyForReferenceByRawValue_override() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), WRITE_KEY);
        userService.activateAccessKey(object.getId().toString(), READ_KEY);
        assertEquals(READ_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).findAccessKeyByReferenceAndSecret(
            object.getId().toString(), READ_KEY);
    }

    @Test
    public void testGetActivatedAccessKeyForReference() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), READ_KEY);
        final MCRAccessKeyDto accessKey = userService.findActiveAccessKey(object.getId().toString());
        assertNotNull(accessKey);
        assertEquals(READ_KEY, userService.findActiveAccessKey(object.getId().toString()).getSecret());
    }

    @Test
    public void testGetActivatedAccessKeyForReference_notExists() {
        assertNull(userService.findActiveAccessKey(object.getId().toString()));
    }

    @Test
    public void testRemoveAccessKeySecretFromCurrentSession() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), READ_KEY);
        userService.deactivateAccessKey(object.getId().toString());
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
    }

    @Test
    public void testCleanUpUserAttributes() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), UNKNOWN_KEY);
        final MCRUser user = new MCRUser("junit1");
        MCRUserManager.createUser(user);
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.GUEST);
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), UNKNOWN_KEY);
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + derivate.getId().toString(), WRITE_KEY);
        userService.cleanUpUserAttributes();
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
        assertNotNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + derivate.getId().toString()));
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.GUEST);
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRUserManager.getUser("junit"));
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(2)).findAccessKeyByReferenceAndSecret(Mockito.anyString(),
            Mockito.anyString());
    }

    @After
    public void teardown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }
}
