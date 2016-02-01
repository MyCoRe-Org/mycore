package org.mycore.iiif.parser;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.iiif.model.MCRIIIFImageTargetRotation;


public class MCRIIIFRotationParserTest {

    @Test
    public void testParse() throws Exception {
        Map<String, MCRIIIFImageTargetRotation> testValues = new Hashtable<>();

        testValues.put("0", new MCRIIIFImageTargetRotation(false, 0));
        testValues.put("!0", new MCRIIIFImageTargetRotation(true, 0));
        testValues.put("90", new MCRIIIFImageTargetRotation(false, 90));
        testValues.put("!90", new MCRIIIFImageTargetRotation(true, 90));
        testValues.put("180", new MCRIIIFImageTargetRotation(false, 180));
        testValues.put("!180", new MCRIIIFImageTargetRotation(true, 180));
        testValues.put("270", new MCRIIIFImageTargetRotation(false, 270));
        testValues.put("!270", new MCRIIIFImageTargetRotation(true, 270));

        testValues.forEach((rotationString, expectedResult)->{
            Assert.assertEquals(new MCRIIIFRotationParser(rotationString).parse(), expectedResult);
        });
    }
}