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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.xeditor.target.MCRInsertTarget;
import org.mycore.frontend.xeditor.target.MCRSubselectTarget;
import org.mycore.frontend.xeditor.target.MCRSwapTarget;
import org.mycore.frontend.xeditor.validation.MCRValidator;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

/**
 * @author Frank Lützenkirchen
 */
public class MCRTransformerHelper {

    private static final char COLON = ':';

    private static final String CONTROL_INSERT = "insert";
    private static final String CONTROL_APPEND = "append";
    private static final String CONTROL_REMOVE = "remove";
    private static final String CONTROL_UP = "up";
    private static final String CONTROL_DOWN = "down";

    private static final String DEFAULT_CONTROLS =
        String.join(" ", CONTROL_INSERT, CONTROL_REMOVE, CONTROL_UP, CONTROL_DOWN);

    private static final String ATTR_URL = "url";
    private static final String ATTR_URI = "uri";
    private static final String ATTR_REF = "ref";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_XPATH = "xpath";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_I18N = "i18n";
    private static final String ATTR_HREF = "xed:href";
    private static final String ATTR_TARGET = "xed:target";
    private static final String ATTR_STYLE = "style";
    private static final String ATTR_RELEVANT_IF = "relevant-if";
    private static final String ATTR_MULTIPLE = "multiple";
    private static final String ATTR_DEFAULT = "default";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_TEST = "test";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_MAX = "max";
    private static final String ATTR_MIN = "min";

    private static final String TYPE_CHECKBOX = "checkbox";
    private static final String TYPE_RADIO = "radio";

    private static final String VALUE_SELECTED = "selected";
    private static final String VALUE_CHECKED = "checked";

    private static final String PREDICATE_IS_FIRST = "[1]";

    private static final String PREFIX_XMLNS = "xmlns:";

    private final MCREditorSession editorSession;

    private MCRBinding currentBinding;

    private int anchorID;

    private boolean withinSelectElement;

    MCRTransformerHelper(MCREditorSession editorSession) {
        this.editorSession = editorSession;
    }

    void handleForm(Map<String, String> attributes) {
        registerAdditionalNamespaces(attributes);
    }

    private void registerAdditionalNamespaces(Map<String, String> attributes) {
        attributes.forEach((key, value) -> {
            if (key.startsWith(PREFIX_XMLNS)) {
                String prefix = key.substring(PREFIX_XMLNS.length());
                String uri = attributes.get(key);
                MCRConstants.registerNamespace(Namespace.getNamespace(prefix, uri));
            }
        });
        attributes.keySet().removeIf(key -> key.startsWith(PREFIX_XMLNS));
    }

    void handleSource(Map<String, String> attributes)
        throws JDOMException, IOException, SAXException, TransformerException {
        String uri = attributes.get(ATTR_URI);
        editorSession.setEditedXML(uri);
    }

    void handleCancel(Map<String, String> attributes) {
        String cancelURL = attributes.get(ATTR_URL);
        editorSession.setCancelURL(cancelURL);
    }

    void handlePostProcessor(Map<String, String> attributes) {
        String clazz = attributes.getOrDefault(ATTR_CLASS, null);
        if (clazz != null) {
            try {
                MCRXEditorPostProcessor instance = ((MCRXEditorPostProcessor) MCRClassTools.forName(clazz)
                    .getDeclaredConstructor()
                    .newInstance());
                editorSession.setPostProcessor(instance);
            } catch (ReflectiveOperationException e) {
                throw new MCRException("Could not initialize Post-Processor with class" + clazz, e);
            }
        }
        editorSession.getPostProcessor().setAttributes(attributes);
    }

    private String replaceParameters(String uri) {
        return getXPathEvaluator().replaceXPaths(uri, false);
    }

    void handleBind(Map<String, String> attributes) throws JaxenException {
        registerAdditionalNamespaces(attributes);

        String xPath = attributes.get("xpath");
        String initialValue = attributes.getOrDefault("initially", null);
        String name = attributes.getOrDefault("name", null);

        bind(xPath, initialValue, name);

        String setAttr = attributes.getOrDefault("set", null);
        if (setAttr != null) {
            setValues(setAttr);
        }

        String setDefault = attributes.getOrDefault("default", null);
        if (setDefault != null) {
            setDefault(setDefault);
        }
    }

    private void bind(String xPath, String initialValue, String name)
        throws JaxenException {
        if (editorSession.getEditedXML() == null) {
            createEmptyDocumentFromXPath(xPath);
        }

        if (currentBinding == null) {
            currentBinding = editorSession.getRootBinding();
        }

        if (initialValue != null) {
            initialValue = replaceXPaths(initialValue);
        }
        setCurrentBinding(new MCRBinding(xPath, initialValue, name, currentBinding));
    }

    private void setCurrentBinding(MCRBinding binding) {
        this.currentBinding = binding;
        editorSession.getValidator().setValidationMarker(currentBinding);
    }

    private void createEmptyDocumentFromXPath(String xPath) throws JaxenException {
        Element root = createRootElement(xPath);
        editorSession.setEditedXML(new Document(root));
        editorSession.setBreakpoint("Starting with empty XML document");
    }

    private Element createRootElement(String xPath) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        LocationPath lp = (LocationPath) (baseXPath.getRootExpr());
        NameStep nameStep = (NameStep) (lp.getSteps().getFirst());
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRConstants.getStandardNamespace(prefix);
        return new Element(nameStep.getLocalName(), ns);
    }

    private void setValues(String value) {
        currentBinding.setValues(replaceXPaths(value));
    }

    private void setDefault(String value) {
        value = replaceXPaths(value);
        currentBinding.setDefault(value);
        editorSession.getSubmission().markDefaultValue(currentBinding.getAbsoluteXPath(), value);
    }

    void handleUnbind() {
        setCurrentBinding(currentBinding.getParent());
    }

    public String getAbsoluteXPath() {
        return currentBinding.getAbsoluteXPath();
    }

    private boolean hasValue(String value) {
        editorSession.getSubmission().mark2checkResubmission(currentBinding);
        return currentBinding.hasValue(value);
    }

    void handleSelect(Map<String, String> attributes, Element result) {
        withinSelectElement = !withinSelectElement;

        String attrMultiple = attributes.getOrDefault(ATTR_MULTIPLE, null);
        boolean withinSelectMultiple = Objects.equals(attrMultiple, "multiple");

        if (withinSelectElement) {
            setXPath(result, withinSelectMultiple);
        }
    }

    void handleOption(Map<String, String> attributes, Element result) {
        if (withinSelectElement) {
            String value = attributes.getOrDefault(ATTR_VALUE, attributes.get(ATTR_TEXT));

            if ((!Strings.isEmpty(value)) && hasValue(value)) {
                result.setAttribute(VALUE_SELECTED, VALUE_SELECTED);
            }
        }
    }

    public String replaceXPaths(String text) {
        return getXPathEvaluator().replaceXPaths(text, false);
    }

    void handleOutput(Map<String, String> attributes, Element result) {
        String value = attributes.getOrDefault(ATTR_VALUE, null);
        String i18n = attributes.getOrDefault(ATTR_I18N, null);
        String output = output(value, i18n);
        result.setText(output);
    }

    private String output(String attrValue, String attrI18N) {
        if (!StringUtils.isEmpty(attrI18N)) {
            String key = replaceParameters(attrI18N);

            if (StringUtils.isEmpty(attrValue)) {
                return MCRTranslation.translate(key);
            } else {
                String value = getXPathEvaluator().evaluateXPath(attrValue);
                return MCRTranslation.translate(key, value);
            }

        } else if (!StringUtils.isEmpty(attrValue)) {
            return getXPathEvaluator().replaceXPathOrI18n(attrValue);
        } else {
            return currentBinding.getValue();
        }
    }

    void handleTest(Map<String, String> attributes, Element result) {
        String xPathExpression = attributes.get(ATTR_TEST);
        boolean testResult = getXPathEvaluator().test(xPathExpression);
        result.setText(Boolean.toString(testResult));
    }

    private MCRXPathEvaluator getXPathEvaluator() {
        if (currentBinding != null) {
            return currentBinding.getXPathEvaluator();
        } else {
            return new MCRXPathEvaluator(editorSession.getVariables(), (Parent) null);
        }
    }

    void handleRepeat(Map<String, String> attributes, Element result)
        throws JaxenException {
        registerAdditionalNamespaces(attributes);

        String xPath = attributes.get(ATTR_XPATH);
        int minRepeats = Integer.parseInt(attributes.getOrDefault(ATTR_MIN, "0"));
        int maxRepeats = Integer.parseInt(attributes.getOrDefault(ATTR_MAX, "0"));
        String method = attributes.get(ATTR_METHOD);
        List<Element> repeats = repeat(xPath, minRepeats, maxRepeats, method);
        result.addContent(repeats);
    }

    private List<Element> repeat(String xPath, int minRepeats, int maxRepeats, String method)
        throws JaxenException {
        MCRRepeatBinding repeat = new MCRRepeatBinding(xPath, currentBinding, minRepeats, maxRepeats, method);
        setCurrentBinding(repeat);

        List<Element> repeats = new ArrayList<>();
        repeat.getBoundNodes().forEach(node -> repeats.add(new Element("repeat")));
        return repeats;
    }

    private MCRRepeatBinding getCurrentRepeat() {
        MCRBinding binding = currentBinding;
        while (!(binding instanceof MCRRepeatBinding)) {
            binding = binding.getParent();
        }
        return (MCRRepeatBinding) binding;
    }

    void handleControls(Map<String, String> attributes, Element result)
        throws JaxenException {
        int pos = getCurrentRepeat().getRepeatPosition();
        int num = getCurrentRepeat().getBoundNodes().size();
        int max = getCurrentRepeat().getMaxRepeats();

        String text = attributes.getOrDefault(ATTR_TEXT, DEFAULT_CONTROLS);
        for (String token : text.split("\\s+")) {
            if ((CONTROL_APPEND.equals(token) && (pos < num)) ||
                (CONTROL_UP.equals(token) && (pos == 1)) ||
                (CONTROL_DOWN.equals(token) && (pos == num)) ||
                ((CONTROL_INSERT.equals(token) || CONTROL_APPEND.equals(token)) && (num == max))) {
                continue;
            }

            Element control = new Element("control").setText(token);

            StringBuilder name = new StringBuilder();
            name.append("_xed_submit_").append(token).append(COLON);

            if (CONTROL_APPEND.equals(token) || CONTROL_INSERT.equals(token)) {
                name.append(MCRInsertTarget.getInsertParameter(getCurrentRepeat()));
            } else if (CONTROL_REMOVE.equals(token)) {
                name.append(getAbsoluteXPath());
            } else if (CONTROL_UP.equals(token) || CONTROL_DOWN.equals(token)) {
                name.append(getSwapParameter(token));
            }

            name.append("|rep-");

            if (CONTROL_REMOVE.equals(token) && (pos > 1)) {
                name.append(previousAnchorID());
            } else {
                name.append(anchorID);
            }

            control.setAttribute(ATTR_NAME, name.toString());
            result.addContent(control);
        }
    }

    private String getSwapParameter(String action) throws JaxenException {
        boolean direction = Objects.equals(action, CONTROL_DOWN) ? MCRSwapTarget.MOVE_DOWN : MCRSwapTarget.MOVE_UP;
        return MCRSwapTarget.getSwapParameter(getCurrentRepeat(), direction);
    }

    void handleBindRepeatPosition(Element result) {
        setCurrentBinding(getCurrentRepeat().bindRepeatPosition());
        editorSession.getValidator().setValidationMarker(currentBinding);

        Element anchor = new Element("a");
        String id = "rep-" + ++anchorID;
        anchor.setAttribute("id", id);
        result.addContent(anchor);
    }

    private int previousAnchorID() {
        return (anchorID == 0 ? 1 : anchorID - 1);
    }

    void handleLoadResource(Map<String, String> attributes) {
        String uri = attributes.get(ATTR_URI);
        String name = attributes.get(ATTR_NAME);

        Element resource = MCRURIResolver.obtainInstance().resolve(replaceXPaths(uri));
        editorSession.getVariables().put(name, resource);
    }

    void handleDisplayValidationMessages(Element result) {
        for (MCRValidator failedRule : editorSession.getValidator().getFailedRules()) {
            result.addContent(failedRule.getRuleElement().clone());
        }
    }

    void handleDisplayValidationMessage(Element result) {
        if (hasValidationError()) {
            Element failedRule = editorSession.getValidator().getFailedRule(currentBinding).getRuleElement();
            result.addContent(failedRule.clone());
        }
    }

    public boolean hasValidationError() {
        return editorSession.getValidator().hasError(currentBinding);
    }

    void handleSubmitButton(Map<String, String> attributes, Element result) {
        String target = attributes.get(ATTR_TARGET);
        String href = attributes.get(ATTR_HREF);

        StringBuilder name = new StringBuilder();
        name.append("_xed_submit_").append(target);

        if ("subselect".equals(target)) {
            name.append(COLON).append(currentBinding.getAbsoluteXPath()).append(COLON)
                .append(MCRSubselectTarget.encode(href));
        } else if (Strings.isNotBlank(href)) {
            name.append(COLON).append(href);
        }

        result.setAttribute(ATTR_NAME, name.toString());
    }

    void handleGetAdditionalParameters(Element result) {
        Element div = new Element("div").setAttribute(ATTR_STYLE, "visibility:hidden");

        Map<String, String[]> parameters = editorSession.getRequestParameters();
        for (String name : parameters.keySet()) {
            for (String value : parameters.get(name)) {
                if ((value != null) && !value.isEmpty()) {
                    div.addContent(buildAdditionalParameterElement(name, value));
                }
            }
        }

        String xPaths2CheckResubmission = editorSession.getSubmission().getXPaths2CheckResubmission();
        if (!xPaths2CheckResubmission.isEmpty()) {
            div.addContent(buildAdditionalParameterElement(MCREditorSubmission.PREFIX_CHECK_RESUBMISSION,
                xPaths2CheckResubmission));
        }

        Map<String, String> defaultValues = editorSession.getSubmission().getDefaultValues();
        for (String xPath : defaultValues.keySet()) {
            div.addContent(buildAdditionalParameterElement(MCREditorSubmission.PREFIX_DEFAULT_VALUE + xPath,
                defaultValues.get(xPath)));
        }

        editorSession.setBreakpoint("After transformation to HTML");
        div.addContent(buildAdditionalParameterElement(MCREditorSessionStore.XEDITOR_SESSION_PARAM,
            editorSession.getCombinedSessionStepID()));

        result.addContent(div);
    }

    private Element buildAdditionalParameterElement(String name, String value) {
        Element input = new Element("input");
        input.setAttribute("type", "hidden");
        input.setAttribute("name", name);
        input.setAttribute("value", value);
        return input;
    }

    void handleCleanupRule(Map<String, String> attributes) {
        String xPath = attributes.get(ATTR_XPATH);
        String relevantIf = attributes.get(ATTR_RELEVANT_IF);
        editorSession.getXMLCleaner().addRule(xPath, relevantIf);
    }

    void handleParam(Map<String, String> attributes) {
        String name = attributes.get(ATTR_NAME);
        String defaultValue = attributes.getOrDefault(ATTR_DEFAULT, null);

        Object currentValue = editorSession.getVariables().get(name);

        if ((currentValue == null) || Objects.equals(currentValue, "")) {
            editorSession.getVariables().put(name, defaultValue == null ? "" : defaultValue);
        }
    }

    void handlePreload(Map<String, String> attributes, Element result) {
        replaceParameters(attributes, result, ATTR_URI);
    }

    void handleInclude(Map<String, String> attributes, Element result) {
        replaceParameters(attributes, result, ATTR_URI, ATTR_REF);
    }

    private void replaceParameters(Map<String, String> attributes, Element result,
        String... attributesToHandle) {
        for (String attribute : attributesToHandle) {
            if (attributes.containsKey(attribute)) {
                result.setAttribute(attribute, replaceParameters(attributes.get(attribute)));
            }
        }
    }

    void handleTextarea(Element result) {
        result.setAttribute(ATTR_NAME, getAbsoluteXPath());

        String value = currentBinding.getValue();
        if (value != null) {
            result.setText(value);
        }
    }

    void handleInput(Map<String, String> attributes, Element result) {
        String type = attributes.get(ATTR_TYPE);

        setXPath(result, TYPE_CHECKBOX.equals(type));

        if (TYPE_RADIO.equals(type) || TYPE_CHECKBOX.equals(type)) {
            String value = attributes.get(ATTR_VALUE);
            if (hasValue(value)) {
                result.setAttribute(VALUE_CHECKED, VALUE_CHECKED);
            }
        } else {
            result.setAttribute(ATTR_VALUE, currentBinding.getValue());
        }
    }

    private void setXPath(Element result, boolean fixPathForMultiple) {
        String xPath = getAbsoluteXPath();
        if (fixPathForMultiple && xPath.endsWith(PREDICATE_IS_FIRST)) {
            xPath = xPath.substring(0, xPath.length() - 3);
        }
        result.setAttribute(ATTR_NAME, xPath);
    }
}
