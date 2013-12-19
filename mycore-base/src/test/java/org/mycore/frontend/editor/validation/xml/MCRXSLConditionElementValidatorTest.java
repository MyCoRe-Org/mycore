package org.mycore.frontend.editor.validation.xml;

import static org.junit.Assert.*;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.xml.MCRXSLConditionElementValidator;

public class MCRXSLConditionElementValidatorTest extends MCRValidatorTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        validator = new MCRXSLConditionElementValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("xsl", "true");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testValidPattern() {
        validator.setProperty("xsl", "label[lang('de')]");
        assertTrue(validator.isValid(buildTestElement("de")));
    }

    @Test
    public void testInvalidPattern() {
        validator.setProperty("xsl", "label[lang('de')]");
        assertFalse(validator.isValid(buildTestElement("en")));
        assertFalse(validator.isValid(new Element("foo")));
    }

    @Test
    public void testNull() {
        validator.setProperty("xsl", "label[lang('de')]");
        assertTrue(validator.isValid((Object[])null));
    }
    private Element buildTestElement(String lang) {
        Element input = new Element("foo");
        Element label = new Element("label").setAttribute("lang", lang, Namespace.XML_NAMESPACE);
        input.addContent(label);
        return input;
    }
}
