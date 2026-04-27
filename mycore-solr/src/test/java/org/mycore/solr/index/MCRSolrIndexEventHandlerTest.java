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

package org.mycore.solr.index;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.common.MCRLinkType;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.document", string = "true")
})
public class MCRSolrIndexEventHandlerTest {

    @Test
    public void handleObjectLinkUpdatedWithDanglingLinkDoesNotFail() {
        MCRObjectID missingLinkedId = MCRObjectID.getInstance("test_document_00000002");
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getInstance("test_document_00000001"));
        object.setSchema("noSchema");
        assertFalse(MCRMetadataManager.exists(missingLinkedId));

        TestableMCRSolrIndexEventHandler handler = new TestableMCRSolrIndexEventHandler();
        MCREvent event = new MCREvent(MCREvent.ObjectType.OBJECT, MCREvent.EventType.LINKED_UPDATED);

        assertDoesNotThrow(() -> handler.handleLinkedUpdate(event, object, MCRLinkType.REFERENCE, missingLinkedId));
    }

    private static final class TestableMCRSolrIndexEventHandler extends MCRSolrIndexEventHandler {
        private void handleLinkedUpdate(MCREvent event, MCRObject updatedObject, MCRLinkType relation,
            MCRObjectID linkedId) {
            handleObjectLinkUpdated(event, updatedObject, relation, linkedId);
        }
    }
}
