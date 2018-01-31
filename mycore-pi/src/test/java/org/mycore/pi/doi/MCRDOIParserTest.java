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

package org.mycore.pi.doi;

import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRDOIParserTest extends MCRTestCase {

    public static final String EXAMPLE_DOI2_PREFIX = "10.22032.0";

    public static final String EXAMPLE_DOI2_SUFFIX = "hj34";

    private static final String EXAMPLE_DOI1_PREFIX = "10.1000";

    private static final String EXAMPLE_DOI1_SUFFIX = "123456";

    private static final String EXAMPLE_DOI1 = EXAMPLE_DOI1_PREFIX + "/" + EXAMPLE_DOI1_SUFFIX;

    private static final String EXAMPLE_DOI2 = EXAMPLE_DOI2_PREFIX + "/" + EXAMPLE_DOI2_SUFFIX;

    private static MCRDOIParser parser;

    private static void testDOI(String doi, String expectedPrefix, String expectedSuffix) {
        Optional<MCRDigitalObjectIdentifier> parsedDOIOptional = parser.parse(doi);

        Assert.assertTrue("DOI should be parsable!", parsedDOIOptional.isPresent());

        MCRDigitalObjectIdentifier parsedDOI = parsedDOIOptional.get();

        Assert.assertEquals("DOI Prefix should match!", expectedPrefix,
            parsedDOI.getPrefix());

        Assert.assertEquals("DOI Suffix should match", expectedSuffix,
            parsedDOI.getSuffix());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
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

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
