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
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;

public class MCRAccessKeySessionServiceImplTest extends MCRAccessKeyTestCase {

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private static final String DELETE_KEY = "delete";

    private static final String UNKNOWN_KEY = "unknown";

    private MCRObject object;

    private MCRDerivate derivate;

    private MCRObjectID unknownObjectId;

    private MCRAccessKeyService accessKeyServiceMock;

    private MCRAccessKeySessionService sessionService;

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
        sessionService = new MCRAccessKeySessionService(accessKeyServiceMock);
    }

    @Test
    public void testSetup() throws MCRPersistenceException, MCRAccessException {
        assertTrue(MCRMetadataManager.exists(object.getId()));
        assertTrue(MCRMetadataManager.exists(derivate.getId()));
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_object() {
        sessionService.activeObjectAccessKeyForReferenceByRawValue(object.getId(), READ_KEY);
        assertEquals(READ_KEY, MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            object.getId().toString(), READ_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_derivate_allowed() {
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read,writedb");
        sessionService.activeObjectAccessKeyForReferenceByRawValue(derivate.getId(), WRITE_KEY);
        assertEquals(WRITE_KEY, MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + derivate.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1))
            .getAccessKeyByReferenceAndValue(derivate.getId().toString(), WRITE_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_derivate_notAllowed() {
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read");
        assertThrows(MCRAccessKeyException.class,
            () -> sessionService.activeObjectAccessKeyForReferenceByRawValue(derivate.getId(), WRITE_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1))
            .getAccessKeyByReferenceAndValue(derivate.getId().toString(), WRITE_KEY);
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
            () -> sessionService.activeObjectAccessKeyForReferenceByRawValue(object.getId(), UNKNOWN_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            object.getId().toString(), UNKNOWN_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_permissionNotAllowed() {
        assertThrows(MCRAccessKeyException.class,
            () -> sessionService.activeObjectAccessKeyForReferenceByRawValue(object.getId(), DELETE_KEY));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            object.getId().toString(), DELETE_KEY);
    }

    @Test
    public void testActiveObjectAccessKeyForReferenceByRawValue_override() {
        MCRSessionMgr.getCurrentSession()
            .put(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + object.getId().toString(), WRITE_KEY);
        sessionService.activeObjectAccessKeyForReferenceByRawValue(object.getId(), READ_KEY);
        assertEquals(READ_KEY, MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + object.getId().toString()));
        Mockito.verify(accessKeyServiceMock, Mockito.times(1)).getAccessKeyByReferenceAndValue(
            object.getId().toString(), READ_KEY);
    }

    @Test
    public void testGetActivatedAccessKeyForReference() {
        MCRSessionMgr.getCurrentSession()
            .put(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + object.getId().toString(), READ_KEY);
        final MCRAccessKeyDto accessKey = sessionService.getActivatedAccessKeyForReference(object.getId().toString());
        assertNotNull(accessKey);
        assertEquals(READ_KEY, sessionService.getActivatedAccessKeyForReference(object.getId().toString()).getValue());
    }

    @Test
    public void testGetActivatedAccessKeyForReference_notExists() {
        assertNull(sessionService.getActivatedAccessKeyForReference(object.getId().toString()));
    }

    @Test
    public void testRemoveAccessKeySecretFromCurrentSession() {
        MCRSessionMgr.getCurrentSession()
            .put(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + object.getId().toString(), READ_KEY);
        sessionService.deactivateAccessKeyForReference(object.getId().toString());
        assertNull(MCRSessionMgr.getCurrentSession()
            .get(MCRAccessKeySessionService.ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX + object.getId().toString()));
    }

    @After
    public void teardown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }
}
