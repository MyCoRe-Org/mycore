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

package org.mycore.iiif.image;

import org.junit.Assert;
import org.junit.Test;

public class MCRIIIFImageUtilTest {

    @Test
    public void encodeImageIdentifier() {
        //examples from https://iiif.io/api/image/2.1/#uri-encoding-and-decoding
        Assert.assertEquals("id1", MCRIIIFImageUtil.encodeImageIdentifier("id1"));
        Assert.assertEquals("bb157hs6068", MCRIIIFImageUtil.encodeImageIdentifier("bb157hs6068"));
        Assert.assertEquals("ark:%2F12025%2F654xz321", MCRIIIFImageUtil.encodeImageIdentifier("ark:/12025/654xz321"));
        Assert.assertEquals("urn:foo:a123,456", MCRIIIFImageUtil.encodeImageIdentifier("urn:foo:a123,456"));
        Assert.assertEquals("http:%2F%2Fexample.com%2F%3F54%23a", MCRIIIFImageUtil.encodeImageIdentifier("http://example.com/?54#a"));
        Assert.assertEquals("Mot%C3%B6rhead", MCRIIIFImageUtil.encodeImageIdentifier("Motörhead"));
        Assert.assertEquals("mycore_derivate_00000001:%2Ftest.tiff", MCRIIIFImageUtil.encodeImageIdentifier("mycore_derivate_00000001:/test.tiff"));
    }

}
