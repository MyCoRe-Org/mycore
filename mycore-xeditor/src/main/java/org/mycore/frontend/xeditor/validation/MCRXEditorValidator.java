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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.w3c.dom.Node;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorValidator {

    public static final String XED_VALIDATION_FAILED = "xed-validation-failed";

    public static final String XED_VALIDATION_MARKER = "xed-validation-marker";

    private List<MCRValidator> validationRules = new ArrayList<>();

    private MCRValidationResults results = new MCRValidationResults();

    private MCREditorSession session;

    public MCRXEditorValidator(MCREditorSession session) {
        this.session = session;
    }

    public void addRule(String baseXPath, Node ruleElement) {
        addIfConfigured(new MCRRequiredValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMinLengthValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMaxLengthValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMatchesValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRXPathTestValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRDateValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMinDateValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMaxDateValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRIntegerValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMinIntegerValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMaxIntegerValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRDecimalValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMinDecimalValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMaxDecimalValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMinStringValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRMaxStringValidator(), baseXPath, ruleElement);
        addIfConfigured(new MCRExternalValidator(), baseXPath, ruleElement);
    }

    private void addIfConfigured(MCRValidator validator, String baseXPath, Node ruleElement) {
        validator.init(baseXPath, ruleElement);
        if (validator.hasRequiredAttributes()) {
            validationRules.add(validator);
        }
    }

    public void clearRules() {
        validationRules.clear();
    }

    public boolean isValid() throws JDOMException, JaxenException {
        MCRBinding root = session.getRootBinding();
        for (MCRValidator rule : validationRules) {
            rule.validate(results, root);
        }

        session.getVariables().put(XED_VALIDATION_FAILED, String.valueOf(!results.isValid()));
        return results.isValid();
    }

    public boolean hasError(MCRBinding binding) {
        return results.hasError(binding.getAbsoluteXPath());
    }

    public MCRValidator getFailedRule(MCRBinding binding) {
        return results.getFailedRule(binding.getAbsoluteXPath());
    }

    public void setValidationMarker(MCRBinding binding) {
        String xPath = binding.getAbsoluteXPath();
        String marker = results.getValidationMarker(xPath);
        session.getVariables().put(XED_VALIDATION_MARKER, marker);
    }

    public Collection<MCRValidator> getFailedRules() {
        return results.getFailedRules();
    }

    public void clearValidationResults() {
        results = new MCRValidationResults();
        session.getVariables().remove(XED_VALIDATION_FAILED);
    }
}
