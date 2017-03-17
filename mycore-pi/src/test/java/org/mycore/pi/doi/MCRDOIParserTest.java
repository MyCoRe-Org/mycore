package org.mycore.pi.doi;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.pi.MCRPersistentIdentifier;

public class MCRDOIParserTest {

    private static final String EXAMPLE_DOI1_PREFIX = "10.1000";

    private static final String EXAMPLE_DOI1_SUFFIX = "123456";

    private static final String EXAMPLE_DOI1 = EXAMPLE_DOI1_PREFIX + "/" + EXAMPLE_DOI1_SUFFIX;

    private MCRDOIParser parser;

    @Before
    public void setUp() {
        parser = new MCRDOIParser();
    }

    @Test
    public void parseRegularDOITest() {
        Optional<MCRPersistentIdentifier> parsedDOIOptional = parser.parse(EXAMPLE_DOI1);

        Assert.assertTrue("DOI should be parsable!", parsedDOIOptional.isPresent());

        MCRDigitalObjectIdentifier parsedDOI = (MCRDigitalObjectIdentifier) parsedDOIOptional.get();

        Assert.assertEquals("DOI Prefix should match!", EXAMPLE_DOI1_PREFIX,
            parsedDOI.getPrefix());

        Assert.assertEquals("DOU Suffix should match", EXAMPLE_DOI1_SUFFIX,
            parsedDOI.getSuffix());

    }

}
