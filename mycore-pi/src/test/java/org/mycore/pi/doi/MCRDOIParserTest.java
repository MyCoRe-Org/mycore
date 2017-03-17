package org.mycore.pi.doi;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.pi.MCRPersistentIdentifier;

public class MCRDOIParserTest {

    public static final String EXAMPLE_DOI2_PREFIX = "10.22032.0";

    public static final String EXAMPLE_DOI2_SUFFIX = "hj34";

    private static final String EXAMPLE_DOI1_PREFIX = "10.1000";

    private static final String EXAMPLE_DOI1_SUFFIX = "123456";

    private static final String EXAMPLE_DOI1 = EXAMPLE_DOI1_PREFIX + "/" + EXAMPLE_DOI1_SUFFIX;

    private static final String EXAMPLE_DOI2 = EXAMPLE_DOI2_PREFIX + "/" + EXAMPLE_DOI2_SUFFIX;

    private MCRDOIParser parser;

    @Before
    public void setUp() {
        parser = new MCRDOIParser();
    }

    @Test
    public void parseRegularDOITest() {
        testDOI(EXAMPLE_DOI1, EXAMPLE_DOI1_PREFIX, EXAMPLE_DOI1_SUFFIX);

    }

    @Test
    /**
     * MCR-1562
     */
    public void parseRegistrantCodeDOI() {
        testDOI(EXAMPLE_DOI2, EXAMPLE_DOI2_PREFIX, EXAMPLE_DOI2_SUFFIX);
    }

    private void testDOI(String doi, String expectedPrefix, String expectedSuffix) {
        Optional<MCRPersistentIdentifier> parsedDOIOptional = parser.parse(doi);

        Assert.assertTrue("DOI should be parsable!", parsedDOIOptional.isPresent());

        MCRDigitalObjectIdentifier parsedDOI = (MCRDigitalObjectIdentifier) parsedDOIOptional.get();

        Assert.assertEquals("DOI Prefix should match!", expectedPrefix,
            parsedDOI.getPrefix());

        Assert.assertEquals("DOU Suffix should match", expectedSuffix,
            parsedDOI.getSuffix());
    }

}
