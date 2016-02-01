package org.mycore.iiif.parser;


import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.mycore.iiif.model.MCRIIIFImageTargetSize;

public class MCRIIIFScaleParserTest {
    public static final int IMAGE_WIDTH = 500;
    public static final int IMAGE_HEIGHT = 400;

    @org.junit.Test
    public void testParseTargetScale() throws Exception {

        Map<String, MCRIIIFImageTargetSize> testValues = new Hashtable<>();
        testValues.put("!1100,800", new MCRIIIFImageTargetSize(1000, 800));
        testValues.put("!1000,900", new MCRIIIFImageTargetSize(1000, 800));
        testValues.put("200,200", new MCRIIIFImageTargetSize(200, 200));
        testValues.put(",200", new MCRIIIFImageTargetSize(250, 200));
        testValues.put(",800", new MCRIIIFImageTargetSize(1000, 800));
        testValues.put("1000,", new MCRIIIFImageTargetSize(1000, 800));
        testValues.put("pct:200", new MCRIIIFImageTargetSize(1000, 800));
        testValues.put("pct:50", new MCRIIIFImageTargetSize(250, 200));

        testValues.forEach((scale, expectedResult) -> {
            Assert.assertEquals(expectedResult, new MCRIIIFScaleParser(scale, IMAGE_WIDTH, IMAGE_HEIGHT).parseTargetScale());
        });

    }
}