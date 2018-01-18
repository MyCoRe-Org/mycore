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

package org.mycore.pi.purl;

import static org.mycore.pi.MCRPIRegistrationService.GENERATOR_CONFIG_PREFIX;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRIDPURLGeneratorTest extends MCRTestCase {

    private static final String TEST_BASE_1 = "http://purl.myurl.de/$ID";

    private static final String TEST_BASE_2 = "http://purl.myurl.de/$ID/$ID/$ID";

    private static final String GENERATOR_1 = "IDPURLGenerator";

    private static final String GENERATOR_2 = GENERATOR_1 + "2";

    @Test
    public void generate() throws MCRPersistentIdentifierException {
        MCRObjectID testID = MCRObjectID.getInstance("my_test_00000001");

        MCRIDPURLGenerator generator1 = new MCRIDPURLGenerator(GENERATOR_1);
        Assert.assertEquals("", generator1.generate(testID, "").asString(), "http://purl.myurl.de/my_test_00000001");

        MCRIDPURLGenerator generator2 = new MCRIDPURLGenerator(GENERATOR_2);
        Assert.assertEquals("", generator2.generate(testID, "").asString(),
            "http://purl.myurl.de/my_test_00000001/my_test_00000001/my_test_00000001");

    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());

        testProperties.put(GENERATOR_CONFIG_PREFIX + GENERATOR_1, MCRIDPURLGenerator.class.getName());
        testProperties.put(GENERATOR_CONFIG_PREFIX + GENERATOR_1 + ".BaseURLTemplate", TEST_BASE_1);

        testProperties.put(GENERATOR_CONFIG_PREFIX + GENERATOR_2, MCRIDPURLGenerator.class.getName());
        testProperties.put(GENERATOR_CONFIG_PREFIX + GENERATOR_2 + ".BaseURLTemplate", TEST_BASE_2);

        return testProperties;
    }
}
