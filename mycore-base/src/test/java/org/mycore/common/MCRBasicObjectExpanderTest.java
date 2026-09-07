package org.mycore.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.datamodel.classifications2.mapping.MCRNoOpClassificationMapper;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.junit", string = "true")
})
public class MCRBasicObjectExpanderTest {
    @Test
    void testExpandedObjectContainsAttributes() {
        final MCRObject mcrObject = new MCRObject();
        mcrObject.setId(MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("MyCoRe_test"));
        mcrObject.setSchema("my_schema.xsd");
        mcrObject.setVersion("2026.06");
        mcrObject.setLabel("my_label");

        final MCRExpandedObject expandedObject =
            new MCRBasicObjectExpander(new MCRNoOpClassificationMapper()).expand(mcrObject);

        assertEquals(mcrObject.getId(), expandedObject.getId());
        assertEquals(mcrObject.getSchema(), expandedObject.getSchema());
        assertEquals(mcrObject.getVersion(), expandedObject.getVersion());
        assertEquals(mcrObject.getLabel(), expandedObject.getLabel());
    }
}
