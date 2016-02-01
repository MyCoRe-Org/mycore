package org.mycore.iiif.parser;


import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.mycore.iiif.model.MCRIIIFImageSourceRegion;

public class MCRIIIFRegionParserTest {


    public static final int IMAGE_WIDTH = 500;
    public static final int IMAGE_HEIGHT = 400;

    @org.junit.Test
    public void testParseImageRegion() throws Exception {
        Map<String, MCRIIIFImageSourceRegion> validCoords = new Hashtable<>();

        validCoords.put("0,0,100,100", new MCRIIIFImageSourceRegion(0, 0, 100, 100));
        validCoords.put("100,100,200,200", new MCRIIIFImageSourceRegion(100, 100, 300, 300));
        validCoords.put("0,0,500,400", new MCRIIIFImageSourceRegion(0, 0, 499, 399));
        validCoords.put("0,0,1,400", new MCRIIIFImageSourceRegion(0, 0, 1, 399));
        validCoords.put("pct:10,10,90,90", new MCRIIIFImageSourceRegion(50, 40, 499, 399));
        validCoords.put("pct:0,0,100,100", new MCRIIIFImageSourceRegion(0, 0, 499, 399));


        for (Map.Entry<String, MCRIIIFImageSourceRegion> entry : validCoords.entrySet()) {
            MCRIIIFImageSourceRegion result = new MCRIIIFRegionParser(entry.getKey(), IMAGE_WIDTH, IMAGE_HEIGHT).parseImageRegion();
            Assert.assertEquals(entry.getValue(), result);
        }


    }
}