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

package org.mycore.mods.identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MCRVZGURIDetectorTest {

    private Hashtable<URI, String> testData;

    @BeforeEach
    public void prepareTest() throws URISyntaxException {
        testData = new Hashtable<>();

        testData.put(new URI("http://uri.gbv.de/document/gvk:ppn:834532662"), "gvk:ppn:834532662");
        testData.put(new URI("http://uri.gbv.de/document/opac-de-28:ppn:826913938"), "opac-de-28:ppn:826913938");
        testData.put(new URI("http://gso.gbv.de/DB=2.1/PPNSET?PPN=357619811"), "gvk:ppn:357619811");
    }

    @Test
    public void testDetect() {
        MCRGBVURLDetector detector = new MCRGBVURLDetector();
        testData.forEach((key, value) -> {
            Optional<Map.Entry<String, String>> maybeDetectedGND = detector.detect(key);

            assertTrue(maybeDetectedGND.isPresent(), "Should have a detected PPN");

            maybeDetectedGND.ifPresent(
                gnd -> assertEquals("ppn", gnd.getKey(), "Should have detected the right type!"));
            maybeDetectedGND.ifPresent(
                gnd -> assertEquals(gnd.getValue(), value, "Should have detected the right value!"));
        });
    }
}
