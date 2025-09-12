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

import java.io.IOException;
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
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREditorSessionStore;
import org.mycore.frontend.xeditor.MCREditorSubmission;
import org.mycore.frontend.xeditor.MCRXEditorPostProcessor;
import org.mycore.frontend.xeditor.target.MCRSubselectTarget;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

/**
 * @author Frank Lützenkirchen
 */
public class MCRTransformerHelper {

    private static final char COLON = ':';

    private static final String ATTR_URL = "url";
    private static final String ATTR_URI = "uri";
    private static final String ATTR_REF = "ref";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_XPATH = "xpath";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_I18N = "i18n";
    private static final String ATTR_HREF = "xed:href";
    private static final String ATTR_TARGET = "xed:target";
    private static final String ATTR_STYLE = "style";
    private static final String ATTR_RELEVANT_IF = "relevant-if";
    private static final String ATTR_DEFAULT = "default";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_TEST = "test";

    private static final String TYPE_CHECKBOX = "checkbox";
    private static final String TYPE_RADIO = "radio";

    private static final String VALUE_CHECKED = "checked";

    private static final String PREDICATE_IS_FIRST = "[1]";

    final MCREditorSession editorSession;

    MCRBinding currentBinding;

    MCRValidationTransformerHelper validationTransformer = new MCRValidationTransformerHelper(this);

    MCRSelectTransformerHelper selectTransformer = new MCRSelectTransformerHelper(this);

    MCRRepeatTransformerHelper repeatTransformer = new MCRRepeatTransformerHelper(this);

    public MCRTransformerHelper(MCREditorSession editorSession) {
        this.editorSession = editorSession;
    }

    void handleForm(MCRTransformerHelperCall call) {
        call.registerDeclaredNamespaces();
    }

    void handleSource(MCRTransformerHelperCall call)
        throws JDOMException, IOException, SAXException, TransformerException {
        String uri = call.getAttributeValue(ATTR_URI);
        editorSession.setEditedXML(uri);
    }

    void handleCancel(MCRTransformerHelperCall call) {
        String cancelURL = call.getAttributeValue(ATTR_URL);
        editorSession.setCancelURL(cancelURL);
    }

    void handlePostProcessor(MCRTransformerHelperCall call) {
        String clazz = call.getAttributeValueOrDefault(ATTR_CLASS, null);
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
        editorSession.getPostProcessor().setAttributes(call.getAttributeMap());
    }

    private String replaceParameters(String uri) {
        return getXPathEvaluator().replaceXPaths(uri, false);
    }

    void handleBind(MCRTransformerHelperCall call) throws JaxenException {
        call.registerDeclaredNamespaces();

        String xPath = call.getAttributeValue("xpath");
        String initialValue = call.getAttributeValueOrDefault("initially", null);
        String name = call.getAttributeValueOrDefault("name", null);

        bind(xPath, initialValue, name);

        String setAttr = call.getAttributeValueOrDefault("set", null);
        if (setAttr != null) {
            setValues(setAttr);
        }

        String setDefault = call.getAttributeValueOrDefault("default", null);
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

    void setCurrentBinding(MCRBinding binding) {
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

    boolean hasValue(String value) {
        editorSession.getSubmission().mark2checkResubmission(currentBinding);
        return currentBinding.hasValue(value);
    }

    void handleReplaceXPaths(MCRTransformerHelperCall call) {
        call.getAttributeMap().keySet().removeIf(key -> key.contains(String.valueOf(COLON)));
        call.getAttributeMap()
            .forEach((name, value) -> call.getReturnElement().setAttribute(name, replaceXPaths(value)));
    }

    private String replaceXPaths(String text) {
        return getXPathEvaluator().replaceXPaths(text, false);
    }

    void handleOutput(MCRTransformerHelperCall call) {
        String value = call.getAttributeValueOrDefault(ATTR_VALUE, null);
        String i18n = call.getAttributeValueOrDefault(ATTR_I18N, null);
        String output = output(value, i18n);
        call.getReturnElement().setText(output);
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

    void handleTest(MCRTransformerHelperCall call) {
        String xPathExpression = call.getAttributeValue(ATTR_TEST);
        boolean testResult = getXPathEvaluator().test(xPathExpression);
        call.getReturnElement().setText(Boolean.toString(testResult));
    }

    private MCRXPathEvaluator getXPathEvaluator() {
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

    void handleGetAdditionalParameters(MCRTransformerHelperCall call) {
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

        call.getReturnElement().addContent(div);
    }

    private Element buildAdditionalParameterElement(String name, String value) {
        Element input = new Element("input");
        input.setAttribute("type", "hidden");
        input.setAttribute("name", name);
        input.setAttribute("value", value);
        return input;
    }

    void handleCleanupRule(MCRTransformerHelperCall call) {
        String xPath = call.getAttributeValue(ATTR_XPATH);
        String relevantIf = call.getAttributeValue(ATTR_RELEVANT_IF);
        editorSession.getXMLCleaner().addRule(xPath, relevantIf);
    }

    void handleParam(MCRTransformerHelperCall call) {
        String name = call.getAttributeValue(ATTR_NAME);
        String defaultValue = call.getAttributeValueOrDefault(ATTR_DEFAULT, null);

        Object currentValue = editorSession.getVariables().get(name);

        if ((currentValue == null) || Objects.equals(currentValue, "")) {
            editorSession.getVariables().put(name, defaultValue == null ? "" : defaultValue);
        }
    }

    void handlePreload(MCRTransformerHelperCall call) {
        replaceParameters(call, ATTR_URI);
    }

    void handleInclude(MCRTransformerHelperCall call) {
        replaceParameters(call, ATTR_URI, ATTR_REF);
    }

    private void replaceParameters(MCRTransformerHelperCall call, String... attributesToHandle) {
        for (String attrName : attributesToHandle) {
            String attrValue = call.getAttributeValue(attrName);
            if (attrValue != null) {
                call.getReturnElement().setAttribute(attrName, replaceParameters(attrValue));
            }
        }
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
