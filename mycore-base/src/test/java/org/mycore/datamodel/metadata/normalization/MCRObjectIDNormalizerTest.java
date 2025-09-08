package org.mycore.datamodel.metadata.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key="MCR.Metadata.Type.test", string = "true")
    }
)
public class MCRObjectIDNormalizerTest {

    @Test
    public void testNormalize() {
        MCRObjectIDNormalizer normalizer = new MCRObjectIDNormalizer();

        MCRObject mcrObject = new MCRObject();
        MCRObjectID testID = MCRObjectID.getInstance("junit_test_00000001");
        mcrObject.setId(testID);
        normalizer.normalize(mcrObject);
        assertEquals(testID, mcrObject.getId());

        // test with zero ID (should be replaced)
        MCRObjectID testID2 = MCRObjectID.getInstance("junit_test_00000000");
        mcrObject.setId(testID2);
        normalizer.normalize(mcrObject);
        assertNotEquals(testID2, mcrObject.getId());
    }

}
