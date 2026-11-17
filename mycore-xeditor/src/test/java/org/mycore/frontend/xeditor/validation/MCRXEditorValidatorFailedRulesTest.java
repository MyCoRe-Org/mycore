/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.test.MyCoReTest;

@MyCoReTest
class MCRXEditorValidatorFailedRulesTest {

    private static final AtomicInteger EXTERNAL_VALIDATIONS = new AtomicInteger();

    @BeforeEach
    void resetExternalValidations() {
        EXTERNAL_VALIDATIONS.set(0);
    }

    @Test
    void skipsFollowingRulesAfterFailureByDefault() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[value='abc']");
        org.w3c.dom.Element firstRule = addRule(session, "/document/value", "test", "false()");
        addRule(session, "/document/value", "test", "false()");
        addRule(session, "/document/value", "class", getClass().getName(), "method", "recordValidation");

        assertFalse(session.getValidator().isValid());
        assertEquals(List.of(firstRule), getFailedRuleElements(session));
        assertEquals(0, EXTERNAL_VALIDATIONS.get());
        checkResult(session, "/document/value", MCRValidationResults.MARKER_ERROR);
    }

    @Test
    void failedRuleGuardsExternalMethodByDefault() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[value='abc']");
        org.w3c.dom.Element matchesRule = addRule(session, "/document/value", "matches", "[0-9]+");
        addRule(session, "/document/value", "class", getClass().getName(), "method", "isPositiveInteger");

        assertFalse(session.getValidator().isValid());
        assertEquals(List.of(matchesRule), getFailedRuleElements(session));
    }

    @Test
    void forceCheckEvaluatesExternalMethodWithInvalidValue() throws JDOMException, JaxenException {
        MCREditorSession session = buildSession("document[value='abc']");
        addRule(session, "/document/value", "matches", "[0-9]+");
        addRule(session, "/document/value", "class", getClass().getName(), "method", "isPositiveInteger",
            "force-check", "true");

        assertThrows(MCRException.class, () -> session.getValidator().isValid());
    }

    @Test
    void requiredMatchesAndTestOnNumberFieldReportOnlyFirstFailureByDefault() throws JaxenException, JDOMException {
        assertNumberFieldFailures("", false, "required");
        assertNumberFieldFailures("abc", false, "matches");
        assertNumberFieldFailures("0", false, "test");
        assertNumberFieldFailures("5", false);
    }

    @Test
    void requiredMatchesAndTestOnNumberFieldReportAllFailuresWithForceCheck()
        throws JaxenException, JDOMException {
        // matches and the external validator skip empty values, the XPath test does not
        assertNumberFieldFailures("", true, "required", "test");
        assertNumberFieldFailures("abc", true, "matches", "test");
        assertNumberFieldFailures("0", true, "test");
        assertNumberFieldFailures("5", true);
    }

    @Test
    void forceCheckReportsDistinctRulesInDeclarationOrder() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[value='abc']");
        org.w3c.dom.Element firstRule = addRule(session, "/document/value", "test", "false()", "message", "first");
        org.w3c.dom.Element secondRule = addRule(session, "/document/value", "test", "false()", "message", "second",
            "force-check", "true");

        assertFalse(session.getValidator().isValid());

        List<MCRValidator> failedRules = List.copyOf(session.getValidator().getFailedRules());
        assertEquals(2, failedRules.size());
        assertSame(firstRule, failedRules.get(0).getRuleElement());
        assertSame(secondRule, failedRules.get(1).getRuleElement());

        MCRBinding binding = new MCRBinding("/document/value", false, session.getRootBinding());
        assertSame(failedRules.get(0), session.getValidator().getFailedRule(binding));
        binding.detach();
    }

    @Test
    void forceCheckEvaluatesDistinctRuleAndKeepsErrorMarker() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[value='abc']");
        addRule(session, "/document/value", "test", "false()");
        addRule(session, "/document/value", "class", getClass().getName(), "method", "recordValidation",
            "force-check", "true");

        assertFalse(session.getValidator().isValid());
        assertEquals(1, EXTERNAL_VALIDATIONS.get());
        checkResult(session, "/document/value", MCRValidationResults.MARKER_ERROR);
    }

    @Test
    void reportsOnlyOneFailureForMultipleValidatorsFromSameRule() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[value='abc']");
        org.w3c.dom.Element rule = addRule(session, "/document/value", "matches", "[0-9]+", "test", "false()",
            "force-check", "true");

        assertFalse(session.getValidator().isValid());

        List<MCRValidator> failedRules = List.copyOf(session.getValidator().getFailedRules());
        assertEquals(1, failedRules.size());
        assertSame(rule, failedRules.get(0).getRuleElement());
    }

    @Test
    void reportsOneRuleOnceForEachFailedBoundNode() throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[value='a'][value='b']");
        org.w3c.dom.Element rule = addRule(session, "/document/value", "test", "false()");

        assertFalse(session.getValidator().isValid());

        List<MCRValidator> failedRules = List.copyOf(session.getValidator().getFailedRules());
        assertEquals(2, failedRules.size());
        assertSame(failedRules.get(0), failedRules.get(1));
        assertSame(rule, failedRules.get(0).getRuleElement());
        checkResult(session, "/document/value[1]", MCRValidationResults.MARKER_ERROR);
        checkResult(session, "/document/value[2]", MCRValidationResults.MARKER_ERROR);
    }

    public static boolean recordValidation(String value) {
        EXTERNAL_VALIDATIONS.incrementAndGet();
        return true;
    }

    /** Typical external method that relies on preceding rules: throws {@link NumberFormatException} otherwise. */
    public static boolean isPositiveInteger(String value) {
        return Integer.parseInt(value) > 0;
    }

    private void assertNumberFieldFailures(String value, boolean forceCheck, String... expectedFailures)
        throws JaxenException, JDOMException {
        MCREditorSession session = buildSession("document[number='" + value + "']");
        String forceCheckValue = String.valueOf(forceCheck);
        addRule(session, "/document/number", "required", "true", "message", "required");
        addRule(session, "/document/number", "matches", "[0-9]+", "message", "matches",
            "force-check", forceCheckValue);
        addRule(session, "/document/number", "test", "number(.) > 0", "message", "test",
            "force-check", forceCheckValue);

        boolean isValid = session.getValidator().isValid();

        List<String> failures = getFailedRuleElements(session).stream()
            .map(rule -> rule.getAttribute("message"))
            .toList();
        assertEquals(List.of(expectedFailures), failures, "failed rules for value '" + value + "'");
        assertEquals(expectedFailures.length == 0, isValid);
        if (expectedFailures.length > 0) {
            checkResult(session, "/document/number", MCRValidationResults.MARKER_ERROR);
        } else {
            assertTrue(session.getValidator().getFailedRules().isEmpty());
        }
    }

    private List<org.w3c.dom.Element> getFailedRuleElements(MCREditorSession session) {
        return session.getValidator().getFailedRules().stream()
            .map(rule -> (org.w3c.dom.Element) rule.getRuleElement())
            .toList();
    }

    private MCREditorSession buildSession(String template) throws JaxenException {
        MCREditorSession session = new MCREditorSession();
        Document editedXML = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        session.setEditedXML(editedXML);
        return session;
    }

    private org.w3c.dom.Element addRule(MCREditorSession session, String baseXPath, String... attributes)
        throws JDOMException {
        Element rule = new Element("validation-rule");
        for (int i = 0; i < attributes.length;) {
            rule.setAttribute(attributes[i++], attributes[i++]);
        }
        new Document(rule);
        org.w3c.dom.Element ruleAsDOMElement = new DOMOutputter().output(rule);
        session.getValidator().addRule(baseXPath, ruleAsDOMElement);
        return ruleAsDOMElement;
    }

    private void checkResult(MCREditorSession session, String xPath, String marker)
        throws JaxenException, JDOMException {
        MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
        session.getValidator().setValidationMarker(binding);
        assertEquals(marker, session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_MARKER));
        binding.detach();
    }
}
