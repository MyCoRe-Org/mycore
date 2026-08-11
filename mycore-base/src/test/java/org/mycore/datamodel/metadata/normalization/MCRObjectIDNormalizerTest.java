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
