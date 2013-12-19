package org.mycore.frontend.editor.validation.xml;

import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRCombinedValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.value.MCRMaxLengthValidator;
import org.mycore.frontend.editor.validation.value.MCRMinLengthValidator;

public class MCRCombinedElementValidatorTest extends MCRValidatorTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        validator = MCRValidatorBuilder.buildPredefinedCombinedElementValidator();
    }

    @Test
    public void testEmptyCombinedValidator() {
        assertFalse(validator.hasRequiredProperties());
        assertTrue(validator.isValid(new Element("foo")));
    }

    @Test
    public void testSingleValidator() {
        validator.setProperty("xsl", "string-length(name())=3");
        assertTrue(validator.isValid(null));
        assertTrue(validator.isValid(new Element("foo")));
        assertFalse(validator.isValid(new Element("foobar")));
    }

    @Test
    public void testMultipleValidators() {
        validator.setProperty("xsl", "string-length(name())=3");
        validator.setProperty("class", this.getClass().getName());
        validator.setProperty("method", "firstCharEqualsLastChar");
        assertFalse(validator.isValid(new Element("abcd")));
        assertFalse(validator.isValid(new Element("ab")));
        assertTrue(validator.isValid(new Element("aba")));
        assertFalse(validator.isValid(new Element("abc")));
    }

    @Ignore
    public static boolean firstCharEqualsLastChar(Element xml) {
        String name = xml.getName();
        return name.charAt(0) == name.charAt(name.length() - 1);
    }
}
