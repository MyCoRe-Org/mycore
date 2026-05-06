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

package org.mycore.pi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
import org.mycore.datamodel.common.MCRLinkTableEventHandler;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
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
public class MCRPersistentIdentifierEventHandlerTest {

    @BeforeEach
    public void setUp() {
        MCREventManager.getInstance().clear();
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRXMLMetadataEventHandler());
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRLinkTableEventHandler());
    }

    @Test
    public void deleteObjectWithDanglingLinkDoesNotFail() throws Exception {
        MCRObjectID objectId = MCRObjectID.getInstance("test_document_00000001");
        MCRObjectID missingLinkedId = MCRObjectID.getInstance("test_document_00000002");
        MCRObject object = createObjectWithLink(objectId, missingLinkedId);

        MCRMetadataManager.create(object);

        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRPersistentIdentifierEventHandler());

        assertDoesNotThrow(() -> MCRMetadataManager.delete(object));
        assertFalse(MCRMetadataManager.exists(objectId));
    }

    private static MCRObject createObjectWithLink(MCRObjectID objectId, MCRObjectID linkedId) {
        MCRObject object = new MCRObject();
        object.setId(objectId);
        object.setSchema("noSchema");

        MCRMetaLinkID danglingLink = new MCRMetaLinkID("link", linkedId, linkedId.toString(), linkedId.toString());
        MCRMetaElement links = new MCRMetaElement(MCRMetaLinkID.class, "links", false, false, List.of(danglingLink));
        object.getMetadata().setMetadataElement(links);

        return object;
    }
}
