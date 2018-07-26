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

package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRLinkTableEventHandler;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;

public class MCRObjectUtilsTest extends MCRStoreTestCase {

    private MCRObject root;

    private MCRObject l11;

    private MCRObject l12;

    private MCRObject l13;

    private MCRObject l21;

    private MCRObject l22;

    private MCRObject l31;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCREventManager.instance().clear();
        MCREventManager.instance().addEventHandler("MCRObject", new MCRXMLMetadataEventHandler());
        MCREventManager.instance().addEventHandler("MCRObject", new MCRLinkTableEventHandler());
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

    @Test
    public void removeLink() throws MCRAccessException {
        // remove parent link
        assertTrue(MCRObjectUtils.removeLink(l22, l11.getId()));
        MCRMetadataManager.update(l22);
        l11 = MCRMetadataManager.retrieveMCRObject(l11.getId());
        l22 = MCRMetadataManager.retrieveMCRObject(l22.getId());
        assertNull(l22.getParent());
        assertFalse(l11.getStructure().containsChild(l22.getId()));

        // add metadata links to test
        addLinksToL22();
        assertEquals(2, l22.getMetadata().stream("links").count());

        // remove metadata link
        assertTrue(MCRObjectUtils.removeLink(l22, l31.getId()));
        assertEquals(1, l22.getMetadata().stream("links").count());
        assertTrue(MCRObjectUtils.removeLink(l22, l11.getId()));
        assertEquals(0, l22.getMetadata().stream("links").count());

        // check if links element is completely removed
        MCRMetadataManager.update(l22);
        l22 = MCRMetadataManager.retrieveMCRObject(l22.getId());
        assertNull(l22.getMetadata().getMetadataElement("links"));
    }

    @Test
    public void removeLinks() throws MCRAccessException {
        // add metadata links to test
        addLinksToL22();
        assertEquals(2, l22.getMetadata().stream("links").count());

        // remove l11
        for (MCRObject linkedObject : MCRObjectUtils.removeLinks(l11.getId()).collect(Collectors.toList())) {
            MCRMetadataManager.update(linkedObject);
        }
        l22 = MCRMetadataManager.retrieveMCRObject(l22.getId());
        assertEquals(1, l22.getMetadata().stream("links").count());

        // remove l31
        for (MCRObject linkedObject : MCRObjectUtils.removeLinks(l31.getId()).collect(Collectors.toList())) {
            MCRMetadataManager.update(linkedObject);
        }
        l22 = MCRMetadataManager.retrieveMCRObject(l22.getId());
        assertEquals(0, l22.getMetadata().stream("links").count());

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

    @Override
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(l31);
        MCRMetadataManager.delete(l22);
        MCRMetadataManager.delete(l21);
        MCRMetadataManager.delete(l13);
        MCRMetadataManager.delete(l12);
        MCRMetadataManager.delete(l11);
        MCRMetadataManager.delete(root);
        super.tearDown();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties
            .put("MCR.Persistence.LinkTable.Store.Class", "org.mycore.backend.hibernate.MCRHIBLinkTableStore");
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Metadata.Type.document", "true");
        return testProperties;
    }

}
