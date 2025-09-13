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

package org.mycore.frontend.xeditor.transformer;

import java.util.Arrays;
import java.util.List;

import org.jdom2.Element;
import org.mycore.frontend.xeditor.validation.MCRValidator;

/**
 * Helps transforming xed:validate and other elements to display validation results. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRValidationTransformerHelper extends MCRTransformerHelperBase {

    private static final String METHOD_VALIDATE = "validate";
    private static final String METHOD_HAS_VALIDATION_ERROR = "hasValidationError";
    private static final String METHOD_DISPLAY_VALIDATION_MESSAGE = "display-validation-message";
    private static final String METHOD_DISPLAY_VALIDATION_MESSAGES = "display-validation-messages";

    private static final String ATTR_DISPLAY = "display";

    private static final String VALUE_LOCAL = "local";
    private static final String VALUE_GLOBAL = "global";

    @Override
    public List<String> getSupportedMethods() {
        return Arrays.asList(METHOD_VALIDATE, METHOD_HAS_VALIDATION_ERROR, METHOD_DISPLAY_VALIDATION_MESSAGE,
            METHOD_DISPLAY_VALIDATION_MESSAGES);
    }

    @Override
    public void handle(MCRTransformerHelperCall call) {
        switch (call.getMethod()) {
            case METHOD_VALIDATE:
                handleValidationRule(call);
                break;
            case METHOD_HAS_VALIDATION_ERROR:
                handleHasValidationError(call);
                break;
            case METHOD_DISPLAY_VALIDATION_MESSAGE:
                handleDisplayValidationMessage(call);
                break;
            case METHOD_DISPLAY_VALIDATION_MESSAGES:
                handleDisplayValidationMessages(call);
                break;
            default:
                ;
        }
    }

    private void handleValidationRule(MCRTransformerHelperCall call) {
        call.getReturnElement().setAttribute("baseXPath", getCurrentBinding().getAbsoluteXPath());
    }

    private void handleDisplayValidationMessages(MCRTransformerHelperCall call) {
        getSession().getValidator().getFailedRules().stream()
            .map(MCRValidator::getRuleElement)
            .filter(rule -> rule.getAttributeValue(ATTR_DISPLAY, "").contains(VALUE_GLOBAL))
            .forEach(call.getReturnElement()::addContent);
    }

    private void handleDisplayValidationMessage(MCRTransformerHelperCall call) {
        if (hasValidationError()) {
            Element failedRuleElement =
                getSession().getValidator().getFailedRule(getCurrentBinding()).getRuleElement();
            if (failedRuleElement.getAttributeValue(ATTR_DISPLAY, "").contains(VALUE_LOCAL)) {
                call.getReturnElement().addContent(failedRuleElement.clone());
            }
        }
    }

    private void handleHasValidationError(MCRTransformerHelperCall call) {
        call.getReturnElement().setText(String.valueOf(hasValidationError()));
    }

    private boolean hasValidationError() {
        return getSession().getValidator().hasError(getCurrentBinding());
    }
}
