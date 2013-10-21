/*
 * $Revision: 28114 $ 
 * $Date: 2013-10-11 18:04:09 +0200 (Fr, 11 Okt 2013) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.xeditor.validation;

import static org.junit.Assert.*;

import org.jaxen.JaxenException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCRNodeBuilder;

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
        for (int i = 0; i < attributes.length;)
            rule.setAttribute(attributes[i++], attributes[i++]);
        new Document(rule);
        org.w3c.dom.Element ruleAsDOMElement = new DOMOutputter().output(rule);
        session.getValidator().addRule(baseXPath, ruleAsDOMElement);
    }

    private void checkResult(MCREditorSession session, String xPath, String marker) throws JaxenException, JDOMException {
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
    public void testLegacyRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[year='1899'][year='2013'][year[3]]");
        addRule(session, "/document/year", "min", "2000", "type", "integer");

        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        checkResult(session, "/document/year", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/year[2]", MCRValidationResults.MARKER_SUCCESS);
        checkResult(session, "/document/year[3]", MCRValidationResults.MARKER_DEFAULT);
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
    public void testMatchesRules() throws JaxenException, JDOMException {
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
}
