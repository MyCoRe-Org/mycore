package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.jdom.Element;
import org.jdom.Namespace;
import org.junit.Test;
import org.junit.Before;

public class MCRXSLConditionElementValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
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
        assertTrue(validator.isValid(null));
    }
    private Element buildTestElement(String lang) {
        Element input = new Element("foo");
        Element label = new Element("label").setAttribute("lang", lang, Namespace.XML_NAMESPACE);
        input.addContent(label);
        return input;
    }
}
