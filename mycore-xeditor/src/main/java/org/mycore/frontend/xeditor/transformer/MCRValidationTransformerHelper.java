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
import java.util.Collection;

import org.jdom2.Element;
import org.mycore.frontend.xeditor.validation.MCRValidator;

public class MCRValidationTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_DISPLAY = "display";

    private static final String VALUE_LOCAL = "local";
    private static final String VALUE_GLOBAL = "global";

    @Override
    public Collection<String> getSupportedMethods() {
        return Arrays.asList("validate", "hasValidationError", "display-validation-message",
            "display-validation-messages");
    }

    @Override
    public void handle(MCRTransformerHelperCall call) {
        switch (call.getMethod()) {
            case "validate":
                handleValidationRule(call);
                break;
            case "hasValidationError":
                handleHasValidationError(call);
                break;
            case "display-validation-message":
                handleDisplayValidationMessage(call);
                break;
            case "display-validation-messages":
                handleDisplayValidationMessages(call);
                break;
            default:
                ;
        }
    }

    private void handleValidationRule(MCRTransformerHelperCall call) {
        call.getReturnElement().setAttribute("baseXPath", state.currentBinding.getAbsoluteXPath());
    }

    private void handleDisplayValidationMessages(MCRTransformerHelperCall call) {
        state.editorSession.getValidator().getFailedRules().stream()
            .map(MCRValidator::getRuleElement)
            .filter(rule -> rule.getAttributeValue(ATTR_DISPLAY, "").contains(VALUE_GLOBAL))
            .forEach(call.getReturnElement()::addContent);
    }

    private void handleDisplayValidationMessage(MCRTransformerHelperCall call) {
        if (hasValidationError()) {
            Element failedRule =
                state.editorSession.getValidator().getFailedRule(state.currentBinding).getRuleElement();
            if (failedRule.getAttributeValue(ATTR_DISPLAY, "").contains(VALUE_LOCAL)) {
                call.getReturnElement().addContent(failedRule.clone());
            }
        }
    }

    private void handleHasValidationError(MCRTransformerHelperCall call) {
        call.getReturnElement().setText(String.valueOf(hasValidationError()));
    }

    private boolean hasValidationError() {
        return state.editorSession.getValidator().hasError(state.currentBinding);
    }
}
