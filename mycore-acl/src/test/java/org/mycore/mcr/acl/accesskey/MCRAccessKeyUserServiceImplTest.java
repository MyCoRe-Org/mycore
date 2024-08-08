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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
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

    private MCRObjectID objectId = null;

    private MCRObjectID derivateId = null;

    private MCRObjectID unknownObjectId = null;

    private MCRAccessKeyService accessKeyServiceMock;

    private MCRAccessKeyUserService userService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        objectId = getObject().getId();
        derivateId = getDerivate().getId();
        unknownObjectId = MCRObjectID.getInstance("mcr_object_00000002");
        accessKeyServiceMock = Mockito.mock(MCRAccessKeyService.class);
        Mockito.when(accessKeyServiceMock.getValue(objectId.toString(), READ_KEY)).thenReturn(READ_KEY);
        Mockito.when(accessKeyServiceMock.getValue(derivateId.toString(), WRITE_KEY)).thenReturn(WRITE_KEY);
        Mockito.when(accessKeyServiceMock.getValue(objectId.toString(), UNKNOWN_KEY)).thenReturn(UNKNOWN_KEY);
        Mockito.when(accessKeyServiceMock.getValue(objectId.toString(), DELETE_KEY)).thenReturn(DELETE_KEY);
        final MCRAccessKeyDto readAccessKeyDto = new MCRAccessKeyDto();
        readAccessKeyDto.setReference(objectId.toString());
        readAccessKeyDto.setValue(READ_KEY);
        readAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_READ);
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(objectId.toString());
        writeAccessKeyDto.setValue(WRITE_KEY);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        final MCRAccessKeyDto deleteAccessKeyDto = new MCRAccessKeyDto();
        deleteAccessKeyDto.setReference(objectId.toString());
        deleteAccessKeyDto.setValue(DELETE_KEY);
        deleteAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_DELETE);
        Mockito.when(accessKeyServiceMock.getAccessKeyByReferenceAndValue(objectId.toString(), READ_KEY))
            .thenReturn(readAccessKeyDto);
        Mockito.when(accessKeyServiceMock.getAccessKeyByReferenceAndValue(derivateId.toString(), WRITE_KEY))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(accessKeyServiceMock.getAccessKeyByReferenceAndValue(objectId.toString(), UNKNOWN_KEY))
            .thenReturn(null);
        userService = new MCRAccessKeyUserService(accessKeyServiceMock);
        final MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_object() {
        userService.activeObjectAccessKeyForReferenceByRawValue(objectId, READ_KEY);
        assertEquals(READ_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(objectId.toString(),
            READ_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_derivate() {
        userService.activeObjectAccessKeyForReferenceByRawValue(derivateId, WRITE_KEY);
        assertEquals(WRITE_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + derivateId.toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(derivateId.toString(),
            WRITE_KEY);
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
            () -> userService.activeObjectAccessKeyForReferenceByRawValue(objectId, UNKNOWN_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(objectId.toString(),
            UNKNOWN_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_override() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString(), WRITE_KEY);
        userService.activeObjectAccessKeyForReferenceByRawValue(objectId, READ_KEY);
        assertEquals(READ_KEY, MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(objectId.toString(),
            READ_KEY);
    }

    @Test
    public void testGetActivatedAccessKeyForReference() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString(), READ_KEY);
        final MCRAccessKeyDto accessKey = userService.getActivatedAccessKeyForReference(objectId.toString());
        assertNotNull(accessKey);
        assertEquals(READ_KEY, userService.getActivatedAccessKeyForReference(objectId.toString()).getValue());
    }

    @Test
    public void testGetActivatedAccessKeyForReference_notExists() {
        assertNull(userService.getActivatedAccessKeyForReference(objectId.toString()));
    }

    @Test
    public void testRemoveAccessKeySecretFromCurrentSession() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString(), READ_KEY);
        userService.deactivateAccessKeyForReference(objectId.toString());
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString()));
    }

    @Test
    public void testCleanUpUserAttributes() {
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString(), UNKNOWN_KEY);
        final MCRUser user = new MCRUser("junit1");
        MCRUserManager.createUser(user);
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString(), UNKNOWN_KEY);
        MCRUserManager.getCurrentUser().setUserAttribute(
            MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + derivateId.toString(), WRITE_KEY);
        userService.cleanUpUserAttributes();
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString()));
        assertNotNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + derivateId.toString()));
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRUserManager.getUser("junit"));
        assertNull(MCRUserManager.getCurrentUser()
            .getUserAttribute(MCRAccessKeyUserService.ACCESS_KEY_USER_ATTRIBUTE_PREFIX + objectId.toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(2)).getAccessKeyByReferenceAndValue(Mockito.anyString(),
            Mockito.anyString());
    }
}
