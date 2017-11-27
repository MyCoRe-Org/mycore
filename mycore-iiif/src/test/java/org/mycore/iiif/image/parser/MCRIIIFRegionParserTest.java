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

package org.mycore.iiif.image.parser;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;

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
            MCRIIIFImageSourceRegion result = new MCRIIIFRegionParser(entry.getKey(), IMAGE_WIDTH, IMAGE_HEIGHT)
                .parseImageRegion();
            Assert.assertEquals(entry.getValue(), result);
        }

    }
}
