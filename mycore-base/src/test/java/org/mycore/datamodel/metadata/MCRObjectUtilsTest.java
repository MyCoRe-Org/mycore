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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.hibernate.MCRHIBLinkTableStore;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.events.MCREvent.ObjectType;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRLinkTableEventHandler;
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
public class MCRObjectUtilsTest {

    private MCRObject root;

    private MCRObject l11;

    private MCRObject l12;

    private MCRObject l13;

    private MCRObject l21;

    private MCRObject l22;

    private MCRObject l31;

    @BeforeEach
    public void setUp() throws Exception {
        MCREventManager.getInstance().clear();
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRXMLMetadataEventHandler());
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRLinkTableEventHandler());
        root = createObject("test_document_00000001", null);
        l11 = createObject("test_document_00000002", root.getId());
        l12 = createObject("test_document_00000003", root.getId());
        l13 = createObject("test_document_00000004", root.getId());
        l21 = createObject("test_document_00000005", l11.getId());
        l22 = createObject("test_document_00000006", l11.getId());
        l31 = createObject("test_document_00000007", l21.getId());

        MCRMetadataManager.create(root);
        MCRMetadataManager.create(l11);
        MCRMetadataManager.create(l12);
        MCRMetadataManager.create(l13);
        MCRMetadataManager.create(l21);
        MCRMetadataManager.create(l22);
        MCRMetadataManager.create(l31);
    }

    private MCRObject createObject(String id, MCRObjectID parent) {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getInstance(id));
        object.setSchema("noSchema");
        if (parent != null) {
            object.getStructure().setParent(parent);
        }
        return object;
    }

    @Test
    public void getAncestors() {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000007"));
        List<MCRObject> ancestors = MCRObjectUtils.getAncestors(doc);
        assertEquals(3, ancestors.size());
        assertEquals(l21.getId(), ancestors.get(0).getId());
        assertEquals(l11.getId(), ancestors.get(1).getId());
        assertEquals(root.getId(), ancestors.get(2).getId());
    }

    @Test
    public void getAncestorsAndSelf() {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000007"));
        List<MCRObject> ancestors = MCRObjectUtils.getAncestorsAndSelf(doc);
        assertEquals(4, ancestors.size());
        assertEquals(l31.getId(), ancestors.get(0).getId());
        assertEquals(l21.getId(), ancestors.get(1).getId());
        assertEquals(l11.getId(), ancestors.get(2).getId());
        assertEquals(root.getId(), ancestors.get(3).getId());
    }

    @Test
    public void getRoot() {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000006"));
        MCRObject root = MCRObjectUtils.getRoot(doc);
        assertNotNull(root);
        assertEquals(this.root.getId(), root.getId());
    }

    @Test
    public void getDescendants() {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000001"));
        List<MCRObject> descendants = MCRObjectUtils.getDescendants(doc);
        assertEquals(6, descendants.size());

        MCRObject doc2 = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000002"));
        List<MCRObject> descendants2 = MCRObjectUtils.getDescendants(doc2);
        assertEquals(3, descendants2.size());

        MCRObject doc3 = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000004"));
        List<MCRObject> descendants3 = MCRObjectUtils.getDescendants(doc3);
        assertEquals(0, descendants3.size());
    }

    @Test
    public void getDescendantsAndSelf() {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000001"));
        List<MCRObject> descendants = MCRObjectUtils.getDescendantsAndSelf(doc);
        assertEquals(7, descendants.size());
    }

    private void addLinksToL22() throws MCRAccessException {
        MCRMetaLinkID l11Link = new MCRMetaLinkID("link", l11.getId(), "l11", "l11");
        MCRMetaLinkID l31Link = new MCRMetaLinkID("link", l31.getId(), "l31", "l31");
        List<MCRMetaLinkID> linkList = Arrays.asList(l11Link, l31Link);
        MCRMetaElement link = new MCRMetaElement(MCRMetaLinkID.class, "links", false, false, linkList);
        l22.getMetadata().setMetadataElement(link);
        MCRMetadataManager.update(l22);
        l22 = MCRMetadataManager.retrieveMCRObject(l22.getId());
    }

    @AfterEach
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(l31);
        MCRMetadataManager.delete(l22);
        MCRMetadataManager.delete(l21);
        MCRMetadataManager.delete(l13);
        MCRMetadataManager.delete(l12);
        MCRMetadataManager.delete(l11);
        MCRMetadataManager.delete(root);
    }

}
