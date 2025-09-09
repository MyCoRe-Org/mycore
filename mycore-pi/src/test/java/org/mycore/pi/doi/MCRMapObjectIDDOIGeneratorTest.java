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

package org.mycore.pi.doi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mycore.pi.doi.MCRDigitalObjectIdentifier.TEST_DOI_PREFIX;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.PI.Generator.MapObjectIDDOI", classNameOf = MCRMapObjectIDDOIGenerator.class),
    @MCRTestProperty(key = "MCR.PI.Generator.MapObjectIDDOI.Prefix.junit_test", string = TEST_DOI_PREFIX),
    @MCRTestProperty(key = "MCR.PI.Generator.MapObjectIDDOI.Prefix.my_test", string = TEST_DOI_PREFIX + "/my.")
})
public class MCRMapObjectIDDOIGeneratorTest {

    MCRMapObjectIDDOIGenerator doiGenerator;

    @BeforeEach
    public void setUp() throws Exception {
        doiGenerator = MCRConfiguration2.getInstanceOfOrThrow(
            MCRMapObjectIDDOIGenerator.class, "MCR.PI.Generator.MapObjectIDDOI");
    }

    @Test
    public void generate() throws Exception {
        MCRObjectID testID1 = MCRObjectID.getInstance("junit_test_00004711");
        MCRObject mcrObject1 = new MCRObject();
        mcrObject1.setId(testID1);
        MCRObjectID testID2 = MCRObjectID.getInstance("my_test_00000815");
        MCRObject mcrObject2 = new MCRObject();
        mcrObject2.setId(testID2);
        assertEquals(TEST_DOI_PREFIX + "/4711", doiGenerator.generate(mcrObject1, null).asString());
        assertEquals(TEST_DOI_PREFIX + "/my.815", doiGenerator.generate(mcrObject2, null).asString());
    }

    @Test
    public void missingMappingTest() {
        assertThrows(
            MCRPersistentIdentifierException.class,
            () -> {
                MCRObjectID testID = MCRObjectID.getInstance("brandNew_test_00000001");
                MCRObject mcrObject = new MCRObject();
                mcrObject.setId(testID);
                doiGenerator.generate(mcrObject, null);
            });
    }

}
