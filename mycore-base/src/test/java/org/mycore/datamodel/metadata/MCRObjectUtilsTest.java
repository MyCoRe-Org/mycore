package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.events.MCREventManager;
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
        MCREventManager.instance().clear().addEventHandler("MCRObject", new MCRXMLMetadataEventHandler());
        root = new MCRObject();
        root.setId(MCRObjectID.getInstance("test_document_00000001"));
        root.setSchema("noSchema");
        l11 = new MCRObject();
        l11.setId(MCRObjectID.getInstance("test_document_00000002"));
        l11.setSchema("noSchema");
        l11.getStructure().setParent(root.getId());
        l12 = new MCRObject();
        l12.setId(MCRObjectID.getInstance("test_document_00000003"));
        l12.setSchema("noSchema");
        l12.getStructure().setParent(root.getId());
        l13 = new MCRObject();
        l13.setId(MCRObjectID.getInstance("test_document_00000004"));
        l13.setSchema("noSchema");
        l13.getStructure().setParent(root.getId());
        l21 = new MCRObject();
        l21.setId(MCRObjectID.getInstance("test_document_00000005"));
        l21.setSchema("noSchema");
        l21.getStructure().setParent(l11.getId());
        l22 = new MCRObject();
        l22.setId(MCRObjectID.getInstance("test_document_00000006"));
        l22.setSchema("noSchema");
        l22.getStructure().setParent(l11.getId());
        l31 = new MCRObject();
        l31.setId(MCRObjectID.getInstance("test_document_00000007"));
        l31.setSchema("noSchema");
        l31.getStructure().setParent(l21.getId());
        MCRMetadataManager.create(root);
        MCRMetadataManager.create(l11);
        MCRMetadataManager.create(l12);
        MCRMetadataManager.create(l13);
        MCRMetadataManager.create(l21);
        MCRMetadataManager.create(l22);
        MCRMetadataManager.create(l31);
    }

    @Test
    public void getAncestors() throws Exception {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000007"));
        List<MCRObject> ancestors = MCRObjectUtils.getAncestors(doc);
        assertEquals(3, ancestors.size());
        assertEquals(l21.getId(), ancestors.get(0).getId());
        assertEquals(l11.getId(), ancestors.get(1).getId());
        assertEquals(root.getId(), ancestors.get(2).getId());
    }

    @Test
    public void getAncestorsAndSelf() throws Exception {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000007"));
        List<MCRObject> ancestors = MCRObjectUtils.getAncestorsAndSelf(doc);
        assertEquals(4, ancestors.size());
        assertEquals(l31.getId(), ancestors.get(0).getId());
        assertEquals(l21.getId(), ancestors.get(1).getId());
        assertEquals(l11.getId(), ancestors.get(2).getId());
        assertEquals(root.getId(), ancestors.get(3).getId());
    }

    @Test
    public void getRoot() throws Exception {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000006"));
        assertEquals(root.getId(), MCRObjectUtils.getRoot(doc).getId());
    }

    @Test
    public void getDescendants() throws Exception {
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
    public void getDescendantsAndSelf() throws Exception {
        MCRObject doc = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance("test_document_00000001"));
        List<MCRObject> descendants = MCRObjectUtils.getDescendantsAndSelf(doc);
        assertEquals(7, descendants.size());
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
