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

package org.mycore.datamodel.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.backend.hibernate.MCRHIBLinkTableStore;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.events.MCREvent.ObjectType;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableEventHandler;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
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
public class MCRMetadataManagerDeleteTest {

    private MCRObject parent;

    private MCRObject child;

    @BeforeEach
    public void setUp() throws Exception {
        MCREventManager.getInstance().clear();
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRXMLMetadataEventHandler());
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRLinkTableEventHandler());
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
    public void failedDeleteRemovesMark() throws Exception {
        // the child is referenced by another object, so deleting the parent fails while deleting its children
        MCRLinkTableManager.getInstance().addReferenceLink(MCRObjectID.getInstance("test_document_00000003"),
            child.getId(), MCRLinkTableManager.ENTRY_TYPE_REFERENCE, "");
        assertThrows(MCRActiveLinkException.class, () -> MCRMetadataManager.delete(parent));

        assertTrue(MCRMetadataManager.exists(parent.getId()), "parent should still exist");
        assertTrue(MCRMetadataManager.exists(child.getId()), "child should still exist");
        assertFalse(MCRMarkManager.getInstance().isMarked(parent.getId()),
            "parent should not be marked after failed delete");
        assertFalse(MCRMarkManager.getInstance().isMarked(child.getId()),
            "child should not be marked after failed delete");

        // updates of the parent must not be ignored
        MCRObject parentToUpdate = MCRMetadataManager.retrieveMCRObject(parent.getId());
        parentToUpdate.getService().addFlag("test", "updated");
        MCRMetadataManager.update(parentToUpdate);
        MCRObject updatedParent = MCRMetadataManager.retrieveMCRObject(parent.getId());
        assertEquals(List.of("updated"), updatedParent.getService().getFlags("test"),
            "update of parent should be stored after failed delete");
    }

    @Test
    public void successfulDeleteRemovesMark() throws Exception {
        MCRMetadataManager.delete(parent);

        assertFalse(MCRMetadataManager.exists(parent.getId()), "parent should be deleted");
        assertFalse(MCRMetadataManager.exists(child.getId()), "child should be deleted");
        assertFalse(MCRMarkManager.getInstance().isMarked(parent.getId()), "parent should not be marked");
        assertFalse(MCRMarkManager.getInstance().isMarked(child.getId()), "child should not be marked");
    }

}
