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

package org.mycore.frontend.xeditor.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorValidatorTest extends MCRTestCase {

    private MCREditorSession buildSession(String template) throws JaxenException, JDOMException {
        MCREditorSession session = new MCREditorSession();
        Document editedXML = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        session.setEditedXML(editedXML);
        return session;
    }

    private void addRule(MCREditorSession session, String baseXPath, String... attributes) throws JDOMException {
        Element rule = new Element("validation-rule");
        for (int i = 0; i < attributes.length;) {
            rule.setAttribute(attributes[i++], attributes[i++]);
        }
        new Document(rule);
        org.w3c.dom.Element ruleAsDOMElement = new DOMOutputter().output(rule);
        session.getValidator().addRule(baseXPath, ruleAsDOMElement);
    }

    private void checkResult(MCREditorSession session, String xPath, String marker)
        throws JaxenException, JDOMException {
        MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
        session.getValidator().setValidationMarker(binding);
        assertEquals(marker, session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_MARKER));
    }

    @Test
    public void testNoValidationRules() throws JDOMException, JaxenException {
        MCREditorSession session = buildSession("document");

        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
    }

    @Test
    public void testRequiredRule() throws JDOMException, JaxenException {
        MCREditorSession session = buildSession("document[title]");
        addRule(session, "/document/title", "required", "true");

        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/title", MCRValidationResults.MARKER_ERROR);

        session = buildSession("document[title='foo']");
        addRule(session, "/document/title", "required", "true");

        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/title", MCRValidationResults.MARKER_SUCCESS);

        session = buildSession("document[title][title[2]='foo']");
        addRule(session, "/document/title", "required", "true");

        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/title", MCRValidationResults.MARKER_SUCCESS);
    }

    @Test
    public void testMinIntegerRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[year='1899'][year='2013'][year[3]]");
        addRule(session, "/document/year", "min", "2000", "type", "integer");

        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        checkResult(session, "/document/year", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/year[2]", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/year[3]", MCRValidationResults.MARKER_DEFAULT);
    }

    @Test
    public void testMaxDecimalRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[price='10.99'][price='20.00'][price[3]]");
        addRule(session, "/document/price", "max", "15.0", "type", "decimal", "locale", "en");

        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        checkResult(session, "/document/price", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/price[2]", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/price[3]", MCRValidationResults.MARKER_DEFAULT);
    }

    @Test
    public void testMaxDateRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[year='2017'][year='2117'][year[3]]");
        addRule(session, "/document/year", "max", "2017", "type", "date", "format", "yyyy");

        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        checkResult(session, "/document/year", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/year[2]", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/year[3]", MCRValidationResults.MARKER_DEFAULT);
    }

    @Test
    public void testDateFormatRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[date]");
        addRule(session, "/document", "xpath", "//date", "type", "date", "format", "yyyy-MM-dd");
        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/date", MCRValidationResults.MARKER_DEFAULT);

        session = buildSession("document[date='2017-04-28']");
        addRule(session, "/document", "xpath", "//date", "type", "date", "format", "yyyy-MM-dd");
        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/date", MCRValidationResults.MARKER_SUCCESS);

        session = buildSession("document[date='28.04.2017']");
        addRule(session, "/document", "xpath", "//date", "type", "date", "format", "yyyy-MM-dd");
        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/date", MCRValidationResults.MARKER_ERROR);

        session = buildSession("document[date='28.04.2017'][date[2]='2017-04-28']");
        addRule(session, "/document", "xpath", "//date", "type", "date", "format", "yyyy-MM-dd;dd.MM.yyyy");
        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/date[1]", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/date[2]", MCRValidationResults.MARKER_SUCCESS);
    }

    @Test
    public void testLengthRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[text='12345']");
        addRule(session, "/document", "xpath", "//text", "minLength", "3", "maxLength", "5");
        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/text", MCRValidationResults.MARKER_SUCCESS);

        session = buildSession("document[text='12345']");
        addRule(session, "/document", "xpath", "//text", "maxLength", "4");
        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/text", MCRValidationResults.MARKER_ERROR);
    }

    @Test
    public void testInvalidation() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[year='1899']");
        addRule(session, "/document/year", "min", "2000", "type", "integer");
        addRule(session, "/document/*", "max", "2010", "type", "integer");
        assertFalse(session.getValidator().isValid());
        checkResult(session, "/document/year", MCRValidationResults.MARKER_ERROR);

    }

    @Test
    public void testGlobalRules() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[year='1899']");
        addRule(session, "/", "xpath", "//year", "min", "2000", "type", "integer");
        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/year", MCRValidationResults.MARKER_ERROR);
    }

    @Test
    public void testMatchesRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[isbn]");
        addRule(session, "/document", "xpath", "//isbn", "matches", "^(97(8|9))?\\d{9}(\\d|X)$");
        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/isbn", MCRValidationResults.MARKER_DEFAULT);

        session = buildSession("document[isbn='9780672317248']");
        addRule(session, "/document", "xpath", "//isbn", "matches", "^(97(8|9))?\\d{9}(\\d|X)$");
        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/isbn", MCRValidationResults.MARKER_SUCCESS);

        session = buildSession("document[isbn='0-672-31724-9']");
        addRule(session, "/document", "xpath", "//isbn", "matches", "^(97(8|9))?\\d{9}(\\d|X)$");
        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
        checkResult(session, "/document/isbn", MCRValidationResults.MARKER_ERROR);
    }

    @Test
    public void testXPathTestRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[author='Jim'][author='Charles'][author='John']");
        addRule(session, "/document/author", "test", "contains(.,'J')");
        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        checkResult(session, "/document/author[1]", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/author[2]", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/author[3]", MCRValidationResults.MARKER_SUCCESS);

        session = buildSession("document[validFrom='2011'][validTo='2009']");
        addRule(session, "/document/validTo", "test", "(string-length(.) = 0) or (number(.) >= number(../validFrom))");
        assertFalse(session.getValidator().isValid());
        checkResult(session, "/document/validFrom", MCRValidationResults.MARKER_DEFAULT);
        checkResult(session, "/document/validTo", MCRValidationResults.MARKER_ERROR);

        session = buildSession("document[validFrom='2011'][validTo]");
        addRule(session, "/document/validTo", "test", "(string-length(.) = 0) or (number(.) >= number(../validFrom))");
        assertTrue(session.getValidator().isValid());

        session = buildSession("document[password='secret'][passwordRepeated='sacred']");
        addRule(session, "/document", "test", "password = passwordRepeated");
        assertFalse(session.getValidator().isValid());

        session = buildSession("document[password='secret'][passwordRepeated='secret']");
        addRule(session, "/document", "test", "password = passwordRepeated");
        assertTrue(session.getValidator().isValid());

        session = buildSession("document[service='printOnDemand']");
        session.getVariables().put("allowedServices", "oai rss");
        addRule(session, "/document/service", "test", "contains($allowedServices,.)");
        assertFalse(session.getValidator().isValid());
        checkResult(session, "/document/service", MCRValidationResults.MARKER_ERROR);

        session = buildSession("document[service='oai']");
        session.getVariables().put("allowedServices", "oai rss");
        addRule(session, "/document/service", "test", "contains($allowedServices,.)");
        assertTrue(session.getValidator().isValid());
        checkResult(session, "/document/service", MCRValidationResults.MARKER_SUCCESS);
    }

    @Test
    public void testExternalMethodRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[author='Jim'][author[2]='Charles'][author[3]]");
        addRule(session, "/document/author", "class", getClass().getName(), "method", "nameStartsWithJ");
        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        checkResult(session, "/document/author[1]", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/author[2]", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/author[3]", MCRValidationResults.MARKER_DEFAULT);

        session = buildSession(
            "document[author[1][first='John'][last='Doe']][author[2][first='James'][last='Watt']][author[3]]");
        addRule(session, "/document/author", "class", getClass().getName(), "method", "authorIsJohnDoe");
        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        checkResult(session, "/document/author[1]", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/author[2]", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/author[3]", MCRValidationResults.MARKER_SUCCESS);
    }

    public static boolean nameStartsWithJ(String name) {
        return name.startsWith("J");
    }

    public static boolean authorIsJohnDoe(Element author) {
        if (author.getChildren().isEmpty()) {
            return true;
        } else {
            return "John".equals(author.getChildText("first")) && "Doe".equals(author.getChildText("last"));
        }
    }
}
