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
package org.mycore.mods.classification;

import java.io.InputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSClassificationMappingNormalizer;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.mapping.MCRMODSGeneratorClassificationMapper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRMODSGeneratedClassificationNormalizerTest {

    private static final String TEST_RESOURCE_PATH =
        "MCRMODSGeneratedClassificationNormalizerTest/mir_mods_00000004.xml";

    @Test
    public void testNormalize() throws Exception {
        MCRObject mcrObject;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TEST_RESOURCE_PATH)) {
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(inputStream);
            mcrObject = new MCRObject(document);
        }
        assertNotNull(mcrObject, "MCRObject should be parsed successfully from resource");

        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(mcrObject);
        List<Element> initialClassifications = modsWrapper.getElements("mods:classification");
        assertEquals(5, initialClassifications.size(), "Initial number of classifications should be 5");
        List<Element> initialGenerated = modsWrapper
            .getElements("mods:classification[contains(@generator, '"
                + MCRMODSGeneratorClassificationMapper.GENERATOR_SUFFIX + "')]");
        assertEquals(4, initialGenerated.size(), "Initial number of generated classifications should be 4");

        MCRMODSClassificationMappingNormalizer normalizer = new MCRMODSClassificationMappingNormalizer();

        normalizer.normalize(mcrObject);

        List<Element> remainingGenerated = modsWrapper
            .getElements("mods:classification[contains(@generator, '"
                + MCRMODSGeneratorClassificationMapper.GENERATOR_SUFFIX + "')]");
        assertTrue(remainingGenerated.isEmpty(), "Classifications with generator suffix '"
            + MCRMODSGeneratorClassificationMapper.GENERATOR_SUFFIX + "' should be removed");

        List<Element> remainingClassifications = modsWrapper.getElements("mods:classification");
        assertEquals(1, remainingClassifications.size(), "Only one classification (sdnb) should remain");

        Element remainingElement = remainingClassifications.getFirst();
        assertNotNull(remainingElement, "Remaining element should not be null"); // Added null check
        assertEquals("sdnb", remainingElement.getAttributeValue("authority"),
                "The remaining classification should have authority 'sdnb'");
        assertEquals("320", remainingElement.getTextTrim(), "The remaining classification should have value '320'");
    }

}
