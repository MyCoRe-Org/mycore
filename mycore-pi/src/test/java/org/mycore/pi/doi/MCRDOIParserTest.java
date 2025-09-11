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

package org.mycore.pi.doi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRDOIParserTest {

    public static final String EXAMPLE_DOI2_PREFIX = "10.22032.0";

    public static final String EXAMPLE_DOI2_SUFFIX = "hj34";

    private static final String EXAMPLE_DOI1_PREFIX = "10.1000";

    private static final String EXAMPLE_DOI1_SUFFIX = "123456";

    private static final String EXAMPLE_DOI1 = EXAMPLE_DOI1_PREFIX + "/" + EXAMPLE_DOI1_SUFFIX;

    private static final String EXAMPLE_DOI2 = EXAMPLE_DOI2_PREFIX + "/" + EXAMPLE_DOI2_SUFFIX;

    private static MCRDOIParser parser;

    private static void testDOI(String doi, String expectedPrefix, String expectedSuffix) {
        Optional<MCRDigitalObjectIdentifier> parsedDOIOptional = parser.parse(doi);

        assertTrue(parsedDOIOptional.isPresent(), "DOI should be parsable!");

        MCRDigitalObjectIdentifier parsedDOI = parsedDOIOptional.get();

        assertEquals(expectedPrefix, parsedDOI.getPrefix(), "DOI Prefix should match!");

        assertEquals(expectedSuffix, parsedDOI.getSuffix(), "DOI Suffix should match");
    }

    @BeforeEach
    public void setUp() {
        parser = new MCRDOIParser();
    }

    @Test
    public void parseRegularDOITest() {
        testDOI(EXAMPLE_DOI1, EXAMPLE_DOI1_PREFIX, EXAMPLE_DOI1_SUFFIX);

    }

    @Test
    public void parseRegistrantCodeDOI() {
        testDOI(EXAMPLE_DOI2, EXAMPLE_DOI2_PREFIX, EXAMPLE_DOI2_SUFFIX);
    }

}
