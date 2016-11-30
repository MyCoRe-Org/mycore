package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public class MCRXSLConditionValueValidatorTest extends MCRValidatorTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        validator = new MCRXSLConditionValueValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("xsl", "true");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testValidPattern() {
        validator.setProperty("xsl", "starts-with(.,'http://')");
        assertTrue(validator.isValid("http://www.foo.bar"));
    }

    @Test
    public void testInvalidPattern() {
        validator.setProperty("xsl", "starts-with(.,'http://')");
        assertFalse(validator.isValid("somethingDifferent"));
    }
}
