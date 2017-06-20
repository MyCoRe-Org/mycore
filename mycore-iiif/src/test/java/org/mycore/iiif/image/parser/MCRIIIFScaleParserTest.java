/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.iiif.image.parser;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.mycore.iiif.image.model.MCRIIIFImageTargetSize;

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
            Assert.assertEquals(expectedResult,
                new MCRIIIFScaleParser(scale, IMAGE_WIDTH, IMAGE_HEIGHT).parseTargetScale());
        });

    }
}
