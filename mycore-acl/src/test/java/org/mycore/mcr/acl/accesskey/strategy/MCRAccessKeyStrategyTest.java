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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyConfig;
import org.mycore.mcr.acl.accesskey.MCRAccessKeySessionService;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTestCase;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUserService;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;

public class MCRAccessKeyStrategyTest extends MCRAccessKeyTestCase {

    private static final String READ_VALUE = "bla";

    private static final String WRITE_VALUE = "blu";

    private MCRAccessKeyUserService userServiceMock = null;

    private MCRAccessKeySessionService sessionServiceMock = null;

    private MCRAccessKeyStrategy strategy;

    private MCRObject object;

    private MCRDerivate derivate;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        object = createObject();
        MCRMetadataManager.create(object);
        derivate = createDerivate(object.getId());
        MCRMetadataManager.create(derivate);
        userServiceMock = Mockito.mock(MCRAccessKeyUserService.class);
        sessionServiceMock = Mockito.mock(MCRAccessKeySessionService.class);
        strategy = new MCRAccessKeyStrategy(userServiceMock, sessionServiceMock);

    }

    @Test
    public void testCheckPermission_noPermission() {
        assertFalse(strategy.checkPermission(object.getId().toString(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkPermission(object.getId().toString(), MCRAccessManager.PERMISSION_WRITE));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkPermission(derivate.getId().toString(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckPermission_noObjectReference() {
        assertFalse(strategy.checkPermission("noObjectId", MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkPermission("noObjectId", MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_session_readKey() {
        final MCRAccessKeyDto readAccessKeyDto = new MCRAccessKeyDto();
        readAccessKeyDto.setReference(object.getId().toString());
        readAccessKeyDto.setValue(READ_VALUE);
        readAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_READ);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(readAccessKeyDto);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read,writedb");
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_session_writeKey() {
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setValue(WRITE_VALUE);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read,writedb");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_session_permissionFilter() {
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setValue(READ_VALUE);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "");
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read,writedb");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_session_objectFilter() {
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setValue(READ_VALUE);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read,writedb");
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "");
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_user_readKey() {
        final MCRAccessKeyDto readAccessKeyDto = new MCRAccessKeyDto();
        readAccessKeyDto.setReference(object.getId().toString());
        readAccessKeyDto.setValue(READ_VALUE);
        readAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_READ);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(readAccessKeyDto);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_user_writeKey() {
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setValue(WRITE_VALUE);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_user_objectFilter() {
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setValue(READ_VALUE);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "");
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckDerivatePermission_user_inheritance() {
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_OBJECT_TYPES_PROP, "object,derivate");
        final MCRAccessKeyDto readDerivateAccessKeyDto = new MCRAccessKeyDto();
        readDerivateAccessKeyDto.setReference(derivate.getId().toString());
        readDerivateAccessKeyDto.setValue(READ_VALUE);
        readDerivateAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_READ);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(derivate.getId().toString()))
            .thenReturn(readDerivateAccessKeyDto);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(derivate.getId().toString()))
            .thenReturn(null);
        assertTrue(strategy.checkDerivatePermission(derivate.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkDerivatePermission(derivate.getId(), MCRAccessManager.PERMISSION_WRITE));
        final MCRAccessKeyDto writeObjectAccessKeyDto = new MCRAccessKeyDto();
        writeObjectAccessKeyDto.setReference(object.getId().toString());
        writeObjectAccessKeyDto.setValue(WRITE_VALUE);
        writeObjectAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeObjectAccessKeyDto);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        assertTrue(strategy.checkDerivatePermission(derivate.getId(), MCRAccessManager.PERMISSION_READ));
        assertTrue(strategy.checkDerivatePermission(derivate.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testCheckObjectPermission_dominance() {
        final MCRAccessKeyDto writeAccessKeyDto = new MCRAccessKeyDto();
        writeAccessKeyDto.setReference(object.getId().toString());
        writeAccessKeyDto.setValue(WRITE_VALUE);
        writeAccessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        Mockito.when(sessionServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeAccessKeyDto);
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(null);
        MCRConfiguration2.set(MCRAccessKeyConfig.ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read");
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertFalse(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
        Mockito.when(userServiceMock.getActivatedAccessKeyForReference(object.getId().toString()))
            .thenReturn(writeAccessKeyDto);
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_READ));
        assertTrue(strategy.checkObjectPermission(object.getId(), MCRAccessManager.PERMISSION_WRITE));
    }

    @After
    public void teardown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }
}
