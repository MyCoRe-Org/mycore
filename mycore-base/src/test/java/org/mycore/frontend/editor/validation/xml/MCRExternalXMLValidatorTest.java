package org.mycore.frontend.editor.validation.xml;

import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.xml.MCRExternalXMLValidator;

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
        assertTrue(validator.isValid((Object[])null));
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
