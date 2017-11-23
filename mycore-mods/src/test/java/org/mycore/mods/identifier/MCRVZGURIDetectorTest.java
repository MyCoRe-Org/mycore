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

package org.mycore.mods.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MCRVZGURIDetectorTest {

    private Hashtable<URI, String> testData;

    @Before
    public void prepareTest() throws URISyntaxException {
        testData = new Hashtable<>();

        testData.put(new URI("http://uri.gbv.de/document/gvk:ppn:834532662"), "gvk:ppn:834532662");
        testData.put(new URI("http://uri.gbv.de/document/opac-de-28:ppn:826913938"), "opac-de-28:ppn:826913938");
        testData.put(new URI("http://gso.gbv.de/DB=2.1/PPNSET?PPN=357619811"), "gvk:ppn:357619811");
    }

    @Test
    public void testDetect() throws Exception {
        MCRGBVURLDetector detector = new MCRGBVURLDetector();
        testData.forEach((key, value) -> {
            Optional<Map.Entry<String, String>> maybeDetectedGND = detector.detect(key);

            Assert.assertTrue("Should have a detected PPN", maybeDetectedGND.isPresent());

            maybeDetectedGND.ifPresent(
                gnd -> Assert.assertEquals("Should have detected the right type!", gnd.getKey(), "ppn"));
            maybeDetectedGND.ifPresent(
                gnd -> Assert.assertEquals("Should have detected the right value!", gnd.getValue(), value));
        });
    }
}
