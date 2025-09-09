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

package org.mycore.pi.purl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mycore.pi.MCRPIService.GENERATOR_CONFIG_PREFIX;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = GENERATOR_CONFIG_PREFIX + MCRIDPURLGeneratorTest.GENERATOR_1,
        classNameOf = MCRIDPURLGenerator.class),
    @MCRTestProperty(key = GENERATOR_CONFIG_PREFIX + MCRIDPURLGeneratorTest.GENERATOR_1 + ".BaseURLTemplate",
        string = MCRIDPURLGeneratorTest.TEST_BASE_1),
    @MCRTestProperty(key = GENERATOR_CONFIG_PREFIX + MCRIDPURLGeneratorTest.GENERATOR_2,
        classNameOf = MCRIDPURLGenerator.class),
    @MCRTestProperty(key = GENERATOR_CONFIG_PREFIX + MCRIDPURLGeneratorTest.GENERATOR_2 + ".BaseURLTemplate",
        string = MCRIDPURLGeneratorTest.TEST_BASE_2)
})
public class MCRIDPURLGeneratorTest {

    public static final String TEST_BASE_1 = "http://purl.myurl.de/$ID";

    public static final String TEST_BASE_2 = "http://purl.myurl.de/$ID/$ID/$ID";

    public static final String GENERATOR_1 = "IDPURLGenerator";

    public static final String GENERATOR_2 = GENERATOR_1 + "2";

    @Test
    public void generate() throws MCRPersistentIdentifierException {
        MCRObjectID testID = MCRObjectID.getInstance("my_test_00000001");
        MCRObject mcrObject = new MCRObject();
        mcrObject.setId(testID);

        MCRIDPURLGenerator generator1 = MCRConfiguration2.getInstanceOfOrThrow(
            MCRIDPURLGenerator.class, GENERATOR_CONFIG_PREFIX + GENERATOR_1);
        assertEquals("http://purl.myurl.de/my_test_00000001",
                generator1.generate(mcrObject, "").asString(), "");

        MCRIDPURLGenerator generator2 = MCRConfiguration2.getInstanceOfOrThrow(
            MCRIDPURLGenerator.class, GENERATOR_CONFIG_PREFIX + GENERATOR_2);
        assertEquals("http://purl.myurl.de/my_test_00000001/my_test_00000001/my_test_00000001",
                generator2.generate(mcrObject, "").asString(), "");

    }

}
