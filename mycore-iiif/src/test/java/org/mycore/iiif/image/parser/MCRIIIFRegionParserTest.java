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

package org.mycore.iiif.image.parser;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;

public class MCRIIIFRegionParserTest {

    public static final int IMAGE_WIDTH = 500;

    public static final int IMAGE_HEIGHT = 400;

    @org.junit.Test
    public void testPercentRegionUsesImageHeight() {
        Map<String, MCRIIIFImageSourceRegion> validCoords = new Hashtable<>();

        // the height of a percent region has to be calculated from the image height, not from the image width
        validCoords.put("pct:0,0,50,50", new MCRIIIFImageSourceRegion(0, 0, 250, 200));
        validCoords.put("pct:10,10,20,20", new MCRIIIFImageSourceRegion(50, 40, 150, 120));

        for (Map.Entry<String, MCRIIIFImageSourceRegion> entry : validCoords.entrySet()) {
            MCRIIIFImageSourceRegion result = new MCRIIIFRegionParser(entry.getKey(), IMAGE_WIDTH, IMAGE_HEIGHT)
                .parseImageRegion();
            Assert.assertEquals(entry.getKey(), entry.getValue(), result);
        }
    }

    @org.junit.Test
    public void testPercentRegionUpToImageBorderIsValid() {
        // percent * (size / 100.0) can be slightly bigger than the exact value, e.g. 100 * (1005 / 100.0) =
        // 1005.0000000000001, which was rounded up to 1006 and marked the region as invalid
        MCRIIIFRegionParser parser = new MCRIIIFRegionParser("pct:0,0,100,100", 1005, 1005);
        parser.parseImageRegion();
        Assert.assertTrue("pct:0,0,100,100 (1005x1005)", parser.isCompleteValid());

        for (int size = 1; size <= 2000; size++) {
            for (int pct = 0; pct < 100; pct++) {
                String region = "pct:" + pct + "," + pct + "," + (100 - pct) + "," + (100 - pct);
                // landscape and portrait, so that width and height are both checked with every size
                assertCompleteValid(region, size, 2001);
                assertCompleteValid(region, 2001, size);
            }
        }
    }

    private static void assertCompleteValid(String region, int width, int height) {
        MCRIIIFRegionParser parser = new MCRIIIFRegionParser(region, width, height);
        parser.parseImageRegion();
        if (!parser.isCompleteValid()) {
            Assert.fail(region + " (" + width + "x" + height + ") is not valid");
        }
    }

    @org.junit.Test
    public void testParseImageRegion() {
        Map<String, MCRIIIFImageSourceRegion> validCoords = new Hashtable<>();

        validCoords.put("0,0,100,100", new MCRIIIFImageSourceRegion(0, 0, 100, 100));
        validCoords.put("100,100,200,200", new MCRIIIFImageSourceRegion(100, 100, 300, 300));
        validCoords.put("0,0,500,400", new MCRIIIFImageSourceRegion(0, 0, 500, 400));
        validCoords.put("0,0,1,400", new MCRIIIFImageSourceRegion(0, 0, 1, 400));
        validCoords.put("pct:10,10,90,90", new MCRIIIFImageSourceRegion(50, 40, 500, 400));
        validCoords.put("pct:0,0,100,100", new MCRIIIFImageSourceRegion(0, 0, 500, 400));

        for (Map.Entry<String, MCRIIIFImageSourceRegion> entry : validCoords.entrySet()) {
            MCRIIIFImageSourceRegion result = new MCRIIIFRegionParser(entry.getKey(), IMAGE_WIDTH, IMAGE_HEIGHT)
                .parseImageRegion();
            Assert.assertEquals(entry.getValue(), result);
        }

    }

    @org.junit.Test
    public void testRegionAtImageBorder() {
        Map<String, MCRIIIFImageSourceRegion> validCoords = new Hashtable<>();

        // x2 and y2 are exclusive: a region that reaches the image border must keep its last pixel column and row
        validCoords.put("full", new MCRIIIFImageSourceRegion(0, 0, 500, 400));
        validCoords.put("0,0,500,400", new MCRIIIFImageSourceRegion(0, 0, 500, 400));
        validCoords.put("400,300,100,100", new MCRIIIFImageSourceRegion(400, 300, 500, 400));
        validCoords.put("400,300,200,200", new MCRIIIFImageSourceRegion(400, 300, 500, 400));
        validCoords.put("pct:0,0,100,100", new MCRIIIFImageSourceRegion(0, 0, 500, 400));

        for (Map.Entry<String, MCRIIIFImageSourceRegion> entry : validCoords.entrySet()) {
            MCRIIIFImageSourceRegion result = new MCRIIIFRegionParser(entry.getKey(), IMAGE_WIDTH, IMAGE_HEIGHT)
                .parseImageRegion();
            Assert.assertEquals(entry.getKey(), entry.getValue(), result);
        }
    }

    @org.junit.Test
    public void testRegionBeyondImageBorderIsCroppedAndValid() {
        Map<String, MCRIIIFImageSourceRegion> croppedCoords = new Hashtable<>();

        // IIIF Image API 2.1, 4.1: return an image cropped at the image's edge (status 200)
        croppedCoords.put("400,300,200,200", new MCRIIIFImageSourceRegion(400, 300, 500, 400));
        croppedCoords.put("0,0,1000,1000", new MCRIIIFImageSourceRegion(0, 0, 500, 400));
        croppedCoords.put("pct:50,50,100,100", new MCRIIIFImageSourceRegion(250, 200, 500, 400));

        for (Map.Entry<String, MCRIIIFImageSourceRegion> entry : croppedCoords.entrySet()) {
            MCRIIIFRegionParser parser = new MCRIIIFRegionParser(entry.getKey(), IMAGE_WIDTH, IMAGE_HEIGHT);
            Assert.assertEquals(entry.getKey(), entry.getValue(), parser.parseImageRegion());
            Assert.assertTrue(entry.getKey(), parser.isCompleteValid());
        }
    }

    @org.junit.Test
    public void testInvalidRegion() {
        // IIIF Image API 2.1, 4.1: zero width or height, or entirely outside of the image (status 400)
        for (String region : new String[] { "500,0,10,10", "0,400,10,10", "pct:100,0,10,10", "0,0,0,100",
            "0,0,100,0", "-10,0,100,100", "0,-10,100,100" }) {
            MCRIIIFRegionParser parser = new MCRIIIFRegionParser(region, IMAGE_WIDTH, IMAGE_HEIGHT);
            Assert.assertThrows(region, IllegalArgumentException.class, parser::parseImageRegion);
        }
    }
}
