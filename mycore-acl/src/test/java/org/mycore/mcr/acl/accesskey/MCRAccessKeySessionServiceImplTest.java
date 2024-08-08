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
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;

public class MCRAccessKeySessionServiceImplTest extends MCRAccessKeyTestCase {

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private static final String DELETE_KEY = "delete";

    private static final String UNKNOWN_KEY = "unknown";

    private MCRObjectID objectId = null;

    private MCRObjectID derivateId = null;

    private MCRObjectID unknownObjectId = null;

    private MCRAccessKeyService accessKeyServiceMock;

    private MCRAccessKeySessionService sessionService;

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
        sessionService = new MCRAccessKeySessionService(accessKeyServiceMock);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_object() {
        sessionService.activeObjectAccessKeyForReferenceByRawValue(objectId, READ_KEY);
        assertEquals(READ_KEY, MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + objectId.toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(objectId.toString(),
            READ_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_derivate() {
        sessionService.activeObjectAccessKeyForReferenceByRawValue(derivateId, WRITE_KEY);
        assertEquals(WRITE_KEY, MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + derivateId.toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(derivateId.toString(),
            WRITE_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_unknownObject() {
        assertThrows(MCRAccessKeyException.class,
            () -> sessionService.activeObjectAccessKeyForReferenceByRawValue(unknownObjectId, WRITE_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(0)).getAccessKeyByReferenceAndValue(Mockito.anyString(),
            Mockito.anyString());
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_unknownAccessKey() {
        assertThrows(MCRAccessKeyException.class,
            () -> sessionService.activeObjectAccessKeyForReferenceByRawValue(objectId, UNKNOWN_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(objectId.toString(),
            UNKNOWN_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_permissionNotAllowed() {
        assertThrows(MCRAccessKeyException.class,
            () -> sessionService.activeObjectAccessKeyForReferenceByRawValue(objectId, DELETE_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(objectId.toString(),
            DELETE_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_override() {
        MCRSessionMgr.getCurrentSession()
            .put(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + objectId.toString(), WRITE_KEY);
        sessionService.activeObjectAccessKeyForReferenceByRawValue(objectId, READ_KEY);
        assertEquals(READ_KEY, MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + objectId.toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(objectId.toString(),
            READ_KEY);
    }

    @Test
    public void testGetActivatedAccessKeyForReference() {
        MCRSessionMgr.getCurrentSession()
            .put(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + objectId.toString(), READ_KEY);
        final MCRAccessKeyDto accessKey = sessionService.getActivatedAccessKeyForReference(objectId.toString());
        assertNotNull(accessKey);
        assertEquals(READ_KEY, sessionService.getActivatedAccessKeyForReference(objectId.toString()).getValue());
    }

    @Test
    public void testGetActivatedAccessKeyForReference_notExists() {
        assertNull(sessionService.getActivatedAccessKeyForReference(objectId.toString()));
    }

    @Test
    public void testRemoveAccessKeySecretFromCurrentSession() {
        MCRSessionMgr.getCurrentSession()
            .put(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + objectId.toString(), READ_KEY);
        sessionService.deactivateAccessKeyForReference(objectId.toString());
        assertNull(MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + objectId.toString()));
    }

}
