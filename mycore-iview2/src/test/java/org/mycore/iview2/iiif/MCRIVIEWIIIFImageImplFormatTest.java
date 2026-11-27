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
package org.mycore.iview2.iiif;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

public class MCRIVIEWIIIFImageImplFormatTest {

    @Test
    public void testSupportedFormatsAreWritable() {
        for (String format : MCRIVIEWIIIFImageImpl.SUPPORTED_FORMATS) {
            assertTrue(ImageIO.getImageWritersBySuffix(format).hasNext(),
                "No image writer for supported format " + format);
        }
    }

    @Test
    public void testReadOnlyFormatIsNotSupported() {
        List<String> readerSuffixes = Arrays.asList(ImageIO.getReaderFileSuffixes());
        // jbig2-imageio provides an image reader only
        assumeTrue(readerSuffixes.contains("jbig2"), "jbig2 reader not on classpath");
        assertFalse(MCRIVIEWIIIFImageImpl.SUPPORTED_FORMATS.contains("jbig2"));
    }

}
