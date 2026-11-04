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

package org.mycore.frontend.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.backend.hibernate.MCRHIBLinkTableStore;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREvent.ObjectType;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRLinkTableEventHandler;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Persistence.LinkTable.Store.Class", classNameOf = MCRHIBLinkTableStore.class),
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.document", string = "true")
})
public class MCRObjectCommandsTest {

    private final List<MCRObjectID> deletedObjects = new ArrayList<>();

    private final MCREventHandlerBase recorder = new MCREventHandlerBase() {
        @Override
        protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
            deletedObjects.add(obj.getId());
        }
    };

    private MCRObject parent;

    private MCRObject child;

    @BeforeEach
    public void setUp() throws Exception {
        MCREventManager.getInstance().clear();
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRXMLMetadataEventHandler());
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRLinkTableEventHandler());
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, recorder);
        parent = createObject("test_document_00000001", null);
        child = createObject("test_document_00000002", parent.getId());
        MCRMetadataManager.create(parent);
        MCRMetadataManager.create(child);
    }

    private MCRObject createObject(String id, MCRObjectID parentId) {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getInstance(id));
        object.setSchema("noSchema");
        if (parentId != null) {
            object.getStructure().setParent(parentId);
        }
        return object;
    }

    @Test
    public void deleteAllObjectsSkipsChildDeletedWithParent() throws Exception {
        List<String> commands = MCRObjectCommands.deleteAllObjects("document");
        assertEquals(Set.of("delete existing object " + parent.getId(), "delete existing object " + child.getId()),
            Set.copyOf(commands));

        assertEquals(List.of("delete existing object " + parent.getId(), "delete existing object " + child.getId()),
            MCRObjectCommands.deleteFromTo(parent.getId().toString(), child.getId().toString()));
        assertEquals(List.of("delete existing object " + child.getId(), "delete existing object " + parent.getId()),
            MCRObjectCommands.deleteTopologicalAllObjects());

        // the child is deleted together with its parent, its own command runs afterwards
        MCRObjectCommands.deleteExisting(parent.getId().toString());
        MCRObjectCommands.deleteExisting(child.getId().toString());

        assertFalse(MCRMetadataManager.exists(parent.getId()), "parent should be deleted");
        assertFalse(MCRMetadataManager.exists(child.getId()), "child should be deleted");
        assertEquals(List.of(child.getId(), parent.getId()), deletedObjects,
            "DELETE event should be fired only once for each object");
    }

    @Test
    public void deleteRemovesLeftoversOfNonExistingObject() throws Exception {
        MCRObjectCommands.delete(parent.getId().toString());
        deletedObjects.clear();
        // only record the event, the stores would log errors for the already deleted object
        MCREventManager.getInstance().clear();
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, recorder);

        // "delete object" still fires the DELETE event, so that event handlers can remove leftovers
        MCRObjectCommands.delete(child.getId().toString());
        assertEquals(List.of(child.getId()), deletedObjects, "DELETE event should be fired for removed object");
    }

}
