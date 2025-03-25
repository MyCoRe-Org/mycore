package org.mycore.datamodel.metadata.normalization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRObjectIDNormalizerTest extends MCRTestCase {

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
