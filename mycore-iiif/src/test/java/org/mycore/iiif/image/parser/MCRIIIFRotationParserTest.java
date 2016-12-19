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
import org.junit.Test;
import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;


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
