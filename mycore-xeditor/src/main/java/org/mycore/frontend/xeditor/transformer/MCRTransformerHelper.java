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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.Parent;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;

/**
 * @author Frank Lützenkirchen
 */
public class MCRTransformerHelper {

    private static final char COLON = ':';

    private static final String ATTR_NAME = "name";

    private static final String PREDICATE_IS_FIRST = "[1]";

    final MCREditorSession editorSession;

    MCRBinding currentBinding;

    private Map<String, MCRTransformerHelperBase> method2helper = new HashMap<>();

    public MCRTransformerHelper(MCREditorSession editorSession) {
        this.editorSession = editorSession;

        List<MCRTransformerHelperBase> helpers = Arrays.asList(
            new MCRSelectTransformerHelper(),
            new MCRValidationTransformerHelper(),
            new MCRRepeatTransformerHelper(),
            new MCRConditionTransformerHelper(),
            new MCROutputTransformerHelper(),
            new MCRPostProcessorTransformerHelper(),
            new MCRSourceTransformerHelper(),
            new MCRCancelTransformerHelper(),
            new MCRParamTransformerHelper(),
            new MCRBindTransformerHelper(),
            new MCRUnbindTransformerHelper(),
            new MCRGetAdditionalParamsTransformerHelper(),
            new MCRPreloadTransformerHelper(),
            new MCRIncludeTransformerHelper(),
            new MCRCleanupRuleTransformerHelper(),
            new MCRLoadResourceTransformerHelper(),
            new MCRInputTransformerHelper(),
            new MCRTextareaTransformerHelper(),
            new MCRFormTransformerHelper(),
            new MCRButtonTransformerHelper());

        helpers.forEach(helper -> {
            helper.init(this);
            helper.getSupportedMethods().forEach(method -> method2helper.put(method, helper));
        });
    }

    void handle(MCRTransformerHelperCall call) throws Exception {
        method2helper.get(call.getMethod()).handle(call);
    }

    String replaceParameters(String uri) {
        return getXPathEvaluator().replaceXPaths(uri, false);
    }

    void setCurrentBinding(MCRBinding binding) {
        this.currentBinding = binding;
        editorSession.getValidator().setValidationMarker(currentBinding);
    }

    boolean hasValue(String value) {
        editorSession.getSubmission().mark2checkResubmission(currentBinding);
        return currentBinding.hasValue(value);
    }

    void handleReplaceXPaths(MCRTransformerHelperCall call) {
        call.getAttributeMap().keySet().removeIf(key -> key.contains(String.valueOf(COLON)));
        call.getAttributeMap()
            .forEach((name, value) -> call.getReturnElement().setAttribute(name, replaceXPaths(value)));
    }

    String replaceXPaths(String text) {
        return getXPathEvaluator().replaceXPaths(text, false);
    }

    MCRXPathEvaluator getXPathEvaluator() {
        if (currentBinding != null) {
            return currentBinding.getXPathEvaluator();
        } else {
            return new MCRXPathEvaluator(editorSession.getVariables(), (Parent) null);
        }
    }

    void setXPath(Element result, boolean fixPathForMultiple) {
        String xPath = currentBinding.getAbsoluteXPath();
        if (fixPathForMultiple && xPath.endsWith(PREDICATE_IS_FIRST)) {
            xPath = xPath.substring(0, xPath.length() - PREDICATE_IS_FIRST.length());
        }
        result.setAttribute(ATTR_NAME, xPath);
    }
}
