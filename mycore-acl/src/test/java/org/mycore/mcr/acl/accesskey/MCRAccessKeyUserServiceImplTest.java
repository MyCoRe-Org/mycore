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

package org.mycore.mcr.acl.accesskey;

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
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRAccessKeyUserServiceImplTest extends MCRAccessKeyTestCase {

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private static final String DELETE_KEY = "delete";

    private static final String UNKNOWN_KEY = "unknown";

    private MCRObject object = null;

    private MCRDerivate derivate = null;

    private MCRObjectID unknownObjectId = null;

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
        Mockito.when(accessKeyServiceMock.getValue(object.getId().toString(), READ_KEY)).thenReturn(READ_KEY);
        Mockito.when(accessKeyServiceMock.getValue(derivate.getId().toString(), WRITE_KEY)).thenReturn(WRITE_KEY);
        Mockito.when(accessKeyServiceMock.getValue(object.getId().toString(), UNKNOWN_KEY)).thenReturn(UNKNOWN_KEY);
        Mockito.when(accessKeyServiceMock.getValue(object.getId().toString(), DELETE_KEY)).thenReturn(DELETE_KEY);
        final MCRAccessKeyDto readAccessKeyDto = new MCRAccessKeyDto();
        readAccessKeyDto.setReference(object.getId().toString());
        readAccessKeyDto.setValue(READ_KEY);
        readAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_READ);
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setValue(WRITE_KEY);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        final MCRAccessKeyDto deleteAccessKeyDto = new MCRAccessKeyDto();
        deleteAccessKeyDto.setReference(object.getId().toString());
        deleteAccessKeyDto.setValue(DELETE_KEY);
        deleteAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_DELETE);
        Mockito.when(accessKeyServiceMock.getAccessKeyByReferenceAndValue(object.getId().toString(), READ_KEY))
            .thenReturn(readAccessKeyDto);
        Mockito.when(accessKeyServiceMock.getAccessKeyByReferenceAndValue(derivate.getId().toString(), WRITE_KEY))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(accessKeyServiceMock.getAccessKeyByReferenceAndValue(object.getId().toString(), UNKNOWN_KEY))
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
    public void testActiveObjectAccessKeyForReferenceByRawValue_object() {
        userService.activeObjectAccessKeyForReferenceByRawValue(object.getId(), READ_KEY);
        assertEquals(READ_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            object.getId().toString(), READ_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_derivate() {
        userService.activeObjectAccessKeyForReferenceByRawValue(derivate.getId(), WRITE_KEY);
        assertEquals(WRITE_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + derivate.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            derivate.getId().toString(), WRITE_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_unknownObject() {
        assertThrows(MCRAccessKeyException.class,
            () -> userService.activeObjectAccessKeyForReferenceByRawValue(unknownObjectId, WRITE_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(0)).getAccessKeyByReferenceAndValue(Mockito.anyString(),
            Mockito.anyString());
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_unknownAccessKey() {
        assertThrows(MCRAccessKeyException.class,
            () -> userService.activeObjectAccessKeyForReferenceByRawValue(object.getId(), UNKNOWN_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            object.getId().toString(), UNKNOWN_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_override() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), WRITE_KEY);
        userService.activeObjectAccessKeyForReferenceByRawValue(object.getId(), READ_KEY);
        assertEquals(READ_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            object.getId().toString(), READ_KEY);
    }

    @Test
    public void testGetActivatedAccessKeyForReference() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), READ_KEY);
        final MCRAccessKeyDto accessKey = userService.getActivatedAccessKeyForReference(object.getId().toString());
        assertNotNull(accessKey);
        assertEquals(READ_KEY, userService.getActivatedAccessKeyForReference(object.getId().toString()).getValue());
    }

    @Test
    public void testGetActivatedAccessKeyForReference_notExists() {
        assertNull(userService.getActivatedAccessKeyForReference(object.getId().toString()));
    }

    @Test
    public void testRemoveAccessKeySecretFromCurrentSession() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), READ_KEY);
        userService.deactivateAccessKeyForReference(object.getId().toString());
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
    }

    @Test
    public void testCleanUpUserAttributes() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString(), UNKNOWN_KEY);
        final MCRUser user = new MCRUser("junit1");
        MCRUserManager.createUser(user);
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());
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
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRUserManager.getUser("junit"));
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(2)).getAccessKeyByReferenceAndValue(Mockito.anyString(),
            Mockito.anyString());
    }

    @After
    public void teardown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }
}
