package org.mycore.mods.classification;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSClassificationMappingNormalizer;
import org.mycore.mods.MCRMODSWrapper;

public class MCRMODSGeneratedClassificationNormalizerTest extends MCRJPATestCase {

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
        assertNotNull("MCRObject should be parsed successfully from resource", mcrObject);

        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(mcrObject);
        List<Element> initialClassifications = modsWrapper.getElements("mods:classification");
        assertEquals("Initial number of classifications should be 5", 5, initialClassifications.size());
        List<Element> initialGenerated = modsWrapper
            .getElements("mods:classification[contains(@generator, '"
                + MCRMODSClassificationMapper.GENERATOR_SUFFIX + "')]");
        assertEquals("Initial number of generated classifications should be 4", 4, initialGenerated.size());

        MCRMODSClassificationMappingNormalizer normalizer = new MCRMODSClassificationMappingNormalizer();

        normalizer.normalize(mcrObject);

        List<Element> remainingGenerated = modsWrapper
            .getElements("mods:classification[contains(@generator, '"
                + MCRMODSClassificationMapper.GENERATOR_SUFFIX + "')]");
        assertTrue(
            "Classifications with generator suffix '" + MCRMODSClassificationMapper.GENERATOR_SUFFIX
                + "' should be removed",
            remainingGenerated.isEmpty());

        List<Element> remainingClassifications = modsWrapper.getElements("mods:classification");
        assertEquals("Only one classification (sdnb) should remain", 1, remainingClassifications.size());

        Element remainingElement = remainingClassifications.getFirst();
        assertNotNull("Remaining element should not be null", remainingElement); // Added null check
        assertEquals("The remaining classification should have authority 'sdnb'", "sdnb",
            remainingElement.getAttributeValue("authority"));
        assertEquals("The remaining classification should have value '320'", "320", remainingElement.getTextTrim());
    }

}
