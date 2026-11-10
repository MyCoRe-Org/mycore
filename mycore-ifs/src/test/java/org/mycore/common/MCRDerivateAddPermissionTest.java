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

package org.mycore.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRMissingPermissionException;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Checks that creating a derivate requires the permissions "writedb" and "add-derivate" on the parent object
 * and no longer evaluates the pool privileges "create-{base}" or "create-{type}".
 */
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRDerivateAddPermissionTest.RecordingAccess.class),
        @MCRTestProperty(key = "MCR.Access.Strategy.Class",
            classNameOf = MCRDerivateAddPermissionTest.RecordingAccess.class)
    })
public class MCRDerivateAddPermissionTest extends MCRIFSTest {

    private MCRObject root;

    private MCRDerivate derivate;

    @BeforeEach
    public void setUp() throws Exception {
        RecordingAccess.reset();
        root = createObject();
        MCRMetadataManager.create(root);
        derivate = createDerivate(root.getId());
        derivate.setId(MCRObjectID.getInstance(MCRObjectID.formatID("mycore_derivate", 0)));
        RecordingAccess.reset();
        MCRAccessManager.invalidPermissionCacheByID(root.getId().toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        RecordingAccess.reset();
        if (MCRMetadataManager.exists(derivate.getId())) {
            MCRMetadataManager.delete(derivate);
        }
        MCRMetadataManager.delete(root);
    }

    @Test
    public void createChecksPermissionsOnParentObject() throws Exception {
        MCRMetadataManager.create(derivate);

        assertNotEquals(0, derivate.getId().getNumberAsInteger(), "derivate ID must be assigned");
        List<String> calls = RecordingAccess.CALLS;
        assertTrue(calls.contains(root.getId() + ":" + MCRAccessManager.PERMISSION_WRITE), calls::toString);
        assertTrue(calls.contains(root.getId() + ":" + MCRAccessManager.PERMISSION_ADD_DERIVATE), calls::toString);
        assertTrue(calls.stream().noneMatch(call -> call.startsWith("null:create-")), calls::toString);
    }

    @Test
    public void createFailsWithoutAddDerivatePermission() {
        RecordingAccess.DENIED.add(MCRAccessManager.PERMISSION_ADD_DERIVATE);

        MCRMissingPermissionException exception = assertThrows(MCRMissingPermissionException.class,
            () -> MCRMetadataManager.create(derivate));

        assertEquals(Optional.of(MCRAccessManager.PERMISSION_ADD_DERIVATE), exception.getPermission());
        assertEquals(Optional.of(root.getId().toString()), exception.getId());
        assertEquals(0, derivate.getId().getNumberAsInteger(),
            "derivate ID must not be assigned when access is denied");
    }

    @Test
    public void createFailsWithoutWritePermission() {
        RecordingAccess.DENIED.add(MCRAccessManager.PERMISSION_WRITE);

        MCRMissingPermissionException exception = assertThrows(MCRMissingPermissionException.class,
            () -> MCRMetadataManager.create(derivate));

        assertEquals(Optional.of(MCRAccessManager.PERMISSION_WRITE), exception.getPermission());
        assertFalse(RecordingAccess.CALLS.contains(root.getId() + ":" + MCRAccessManager.PERMISSION_ADD_DERIVATE),
            RecordingAccess.CALLS::toString);
    }

    /**
     * Records every permission check as "{id}:{permission}" and denies the permissions in {@link #DENIED}.
     */
    public static class RecordingAccess implements MCRAccessInterface, MCRAccessCheckStrategy {

        static final List<String> CALLS = new ArrayList<>();

        static final Set<String> DENIED = new HashSet<>();

        static void reset() {
            CALLS.clear();
            DENIED.clear();
        }

        @Override
        public boolean checkPermission(String permission) {
            return checkPermission(null, permission);
        }

        @Override
        public boolean checkPermission(String id, String permission) {
            CALLS.add(id + ":" + permission);
            return !DENIED.contains(permission);
        }

        @Override
        public boolean checkPermissionForUser(String permission, MCRUserInformation userInfo) {
            return checkPermission(permission);
        }

        @Override
        public boolean checkPermission(String id, String permission, MCRUserInformation userInfo) {
            return checkPermission(id, permission);
        }

    }

}
