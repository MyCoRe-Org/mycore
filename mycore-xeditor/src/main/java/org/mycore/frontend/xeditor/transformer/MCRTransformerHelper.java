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

import org.apache.logging.log4j.util.Strings;
import org.jdom2.Element;
import org.jdom2.Parent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.target.MCRSubselectTarget;

/**
 * @author Frank Lützenkirchen
 */
public class MCRTransformerHelper {

    private static final char COLON = ':';

    private static final String ATTR_ACTION = "action";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_URI = "uri";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_XPATH = "xpath";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_HREF = "xed:href";
    private static final String ATTR_TARGET = "xed:target";
    private static final String ATTR_RELEVANT_IF = "relevant-if";

    private static final String TYPE_CHECKBOX = "checkbox";
    private static final String TYPE_RADIO = "radio";

    private static final String VALUE_CHECKED = "checked";
    private static final String VALUE_POST = "post";
    private static final String VALUE_OUTPUT = "output";

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
            new MCROutputTransformerHandler(),
            new MCRPostProcessorTransformerHelper(),
            new MCRSourceTransformerHelper(),
            new MCRCancelTransformerHelper(),
            new MCRParamTransformerHelper(),
            new MCRBindTransformerHelper(),
            new MCRUnbindTransformerHelper(),
            new MCRGetAdditionalParamsTransformerHelper(),
            new MCRPreloadTransformerHelper(),
            new MCRIncludeTransformerHandler());

        helpers.forEach(helper -> {
            helper.init(this);
            helper.getSupportedMethods().forEach(method -> method2helper.put(method, helper));
        });
    }

    void handle(MCRTransformerHelperCall call) throws Exception {
        method2helper.get(call.getMethod()).handle(call);
    }

    void handleForm(MCRTransformerHelperCall call) {
        call.registerDeclaredNamespaces();

        String method = call.getAttributeValueOrDefault(ATTR_METHOD, VALUE_POST);
        if (!VALUE_OUTPUT.equals(method)) {
            handleReplaceXPaths(call);
            
            call.getReturnElement().setAttribute(ATTR_ACTION, MCRFrontendUtil.getBaseURL() + "servlets/XEditor");
            call.getReturnElement().setAttribute(ATTR_METHOD, method);
        }
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

    void handleLoadResource(MCRTransformerHelperCall call) {
        String uri = call.getAttributeValue(ATTR_URI);
        String name = call.getAttributeValue(ATTR_NAME);

        Element resource = MCRURIResolver.obtainInstance().resolve(replaceXPaths(uri));
        editorSession.getVariables().put(name, resource);
    }

    void handleSubmitButton(MCRTransformerHelperCall call) {
        String target = call.getAttributeValue(ATTR_TARGET);
        String href = call.getAttributeValue(ATTR_HREF);

        StringBuilder name = new StringBuilder();
        name.append("_xed_submit_").append(target);

        if ("subselect".equals(target)) {
            name.append(COLON).append(currentBinding.getAbsoluteXPath()).append(COLON)
                .append(MCRSubselectTarget.encode(href));
        } else if (Strings.isNotBlank(href)) {
            name.append(COLON).append(href);
        }

        call.getReturnElement().setAttribute(ATTR_NAME, name.toString());
    }

    void handleCleanupRule(MCRTransformerHelperCall call) {
        String xPath = call.getAttributeValue(ATTR_XPATH);
        String relevantIf = call.getAttributeValue(ATTR_RELEVANT_IF);
        editorSession.getXMLCleaner().addRule(xPath, relevantIf);
    }

    void handleTextarea(MCRTransformerHelperCall call) {
        handleReplaceXPaths(call);

        call.getReturnElement().setAttribute(ATTR_NAME, currentBinding.getAbsoluteXPath());

        String value = currentBinding.getValue();
        if (value != null) {
            call.getReturnElement().setText(value);
        }
    }

    void handleInput(MCRTransformerHelperCall call) {
        String type = call.getAttributeValue(ATTR_TYPE);

        setXPath(call.getReturnElement(), TYPE_CHECKBOX.equals(type));

        if (TYPE_RADIO.equals(type) || TYPE_CHECKBOX.equals(type)) {
            String value = call.getAttributeValue(ATTR_VALUE);
            if (hasValue(value)) {
                call.getReturnElement().setAttribute(VALUE_CHECKED, VALUE_CHECKED);
            }
        } else {
            call.getReturnElement().setAttribute(ATTR_VALUE, currentBinding.getValue());
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
