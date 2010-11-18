package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.jdom.Element;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;

public class MCRExternalXMLValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRExternalXMLValidator();
        validator.setProperty("class", this.getClass().getName());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("method", "externalTest");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testExternal() {
        validator.setProperty("method", "externalTest");
        assertTrue(validator.isValid(null));
        assertFalse(validator.isValid(new Element("foo")));
        assertTrue(validator.isValid(new Element("mcrfoo")));
    }

    @Ignore
    public static boolean externalTest(Element xml) {
        if (xml == null)
            return true;
        else
            return xml.getName().contains("mcr");
    }
}
