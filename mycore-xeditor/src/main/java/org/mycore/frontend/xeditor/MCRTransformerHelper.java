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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.xpath.NodeSet;
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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author Frank Lützenkirchen
 */
public class MCRTransformerHelper {

    private final MCREditorSession editorSession;

    private MCRBinding currentBinding;

    public int anchorID;

    private boolean withinSelectElement;

    private boolean withinSelectMultiple;

    MCRTransformerHelper(MCREditorSession editorSession) {
        this.editorSession = editorSession;
    }

    public void addNamespace(String prefix, String uri) {
        MCRConstants.registerNamespace(Namespace.getNamespace(prefix, uri));
    }

    void readSourceXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        editorSession.setEditedXML(uri);
    }

    void setCancelURL(String cancelURL) {
        editorSession.setCancelURL(cancelURL);
    }

    void initializePostprocessor(Map<String, String> attributeMap) {
        editorSession.getPostProcessor().setAttributes(attributeMap);
    }

    void setPostProcessor(String clazz) {
        try {
            MCRXEditorPostProcessor instance = ((MCRXEditorPostProcessor) MCRClassTools.forName(clazz)
                .getDeclaredConstructor()
                .newInstance());
            editorSession.setPostProcessor(instance);
        } catch (ReflectiveOperationException e) {
            throw new MCRException("Could not initialize Post-Processor with class" + clazz, e);
        }
    }

    public String replaceParameters(String uri) {
        return getXPathEvaluator().replaceXPaths(uri, false);
    }

    void bind(Map<String, String> attributes) throws JaxenException {
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

    void unbind() {
        setCurrentBinding(currentBinding.getParent());
    }

    public String getAbsoluteXPath() {
        return currentBinding.getAbsoluteXPath();
    }

    public String getValue() {
        return currentBinding.getValue();
    }

    public boolean hasValue(String value) {
        editorSession.getSubmission().mark2checkResubmission(currentBinding);
        return currentBinding.hasValue(value);
    }

    void toggleWithinSelectElement(String attrMultiple) {
        withinSelectElement = !withinSelectElement;
        withinSelectMultiple = Objects.equals(attrMultiple, "multiple");
    }

    public boolean isWithinSelectElement() {
        return withinSelectElement;
    }

    public boolean isWithinSelectMultiple() {
        return withinSelectMultiple;
    }

    public String replaceXPaths(String text) {
        return getXPathEvaluator().replaceXPaths(text, false);
    }

    public String replaceXPathOrI18n(String expression) {
        return getXPathEvaluator().replaceXPathOrI18n(expression);
    }

    public String evaluateXPath(String xPathExpression) {
        return getXPathEvaluator().evaluateXPath(xPathExpression);
    }

    String output(String attrValue, String attrI18N) {
        if (!StringUtils.isEmpty(attrI18N)) {
            String key = replaceParameters(attrI18N);

            if (StringUtils.isEmpty(attrValue)) {
                return MCRTranslation.translate(key);
            } else {
                String value = evaluateXPath(attrValue);
                return MCRTranslation.translate(key, value);
            }

        } else if (!StringUtils.isEmpty(attrValue)) {
            return replaceXPathOrI18n(attrValue);
        } else {
            return getValue();
        }
    }

    public boolean test(String xPathExpression) {
        return getXPathEvaluator().test(xPathExpression);
    }

    private MCRXPathEvaluator getXPathEvaluator() {
        if (currentBinding != null) {
            return currentBinding.getXPathEvaluator();
        } else {
            return new MCRXPathEvaluator(editorSession.getVariables(), (Parent) null);
        }
    }

    List<Element> repeat(String xPath, int minRepeats, int maxRepeats, String method)
        throws JaxenException {
        MCRRepeatBinding repeat = new MCRRepeatBinding(xPath, currentBinding, minRepeats, maxRepeats, method);
        setCurrentBinding(repeat);

        List<Element> repeats = new ArrayList<Element>();
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

    int getNumRepeats() {
        return getCurrentRepeat().getBoundNodes().size();
    }

    int getMaxRepeats() {
        return getCurrentRepeat().getMaxRepeats();
    }

    public int getRepeatPosition() {
        return getCurrentRepeat().getRepeatPosition();
    }

    public int bindRepeatPosition() {
        setCurrentBinding(getCurrentRepeat().bindRepeatPosition());
        editorSession.getValidator().setValidationMarker(currentBinding);
        return nextAnchorID();
    }

    public String getSwapParameter(String action) throws JaxenException {
        boolean direction = Objects.equals(action, "down") ? MCRSwapTarget.MOVE_DOWN : MCRSwapTarget.MOVE_UP;
        return MCRSwapTarget.getSwapParameter(getCurrentRepeat(), direction);
    }

    public String getInsertParameter() throws JaxenException {
        return MCRInsertTarget.getInsertParameter(getCurrentRepeat());
    }

    private int nextAnchorID() {
        return ++anchorID;
    }

    public int getAnchorID() {
        return anchorID;
    }

    public int previousAnchorID() {
        return (anchorID == 0 ? 1 : anchorID - 1);
    }

    void loadResource(String uri, String name) {
        Element resource = MCRURIResolver.obtainInstance().resolve(replaceXPaths(uri));
        editorSession.getVariables().put(name, resource);
    }

    public void addValidationRule(Node ruleElement) {
        editorSession.getValidator().addRule(currentBinding.getAbsoluteXPath(), ruleElement);
    }

    public boolean hasValidationError() {
        return editorSession.getValidator().hasError(currentBinding);
    }

    public Node getFailedValidationRule() {
        return editorSession.getValidator().getFailedRule(currentBinding).getRuleElement();
    }

    public NodeSet getFailedValidationRules() {
        NodeSet nodeSet = new NodeSet();
        for (MCRValidator failedRule : editorSession.getValidator().getFailedRules()) {
            nodeSet.addNode(failedRule.getRuleElement());
        }
        return nodeSet;
    }

    public String getSubselectParam(String href) {
        return currentBinding.getAbsoluteXPath() + ":" + MCRSubselectTarget.encode(href);
    }

    public NodeSet getAdditionalParameters() throws ParserConfigurationException {
        org.w3c.dom.Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        NodeSet nodeSet = new NodeSet();

        Map<String, String[]> parameters = editorSession.getRequestParameters();
        for (String name : parameters.keySet()) {
            for (String value : parameters.get(name)) {
                if ((value != null) && !value.isEmpty()) {
                    nodeSet.addNode(buildAdditionalParameterElement(dom, name, value));
                }
            }
        }

        String xPaths2CheckResubmission = editorSession.getSubmission().getXPaths2CheckResubmission();
        if (!xPaths2CheckResubmission.isEmpty()) {
            nodeSet.addNode(buildAdditionalParameterElement(dom, MCREditorSubmission.PREFIX_CHECK_RESUBMISSION,
                xPaths2CheckResubmission));
        }

        Map<String, String> defaultValues = editorSession.getSubmission().getDefaultValues();
        for (String xPath : defaultValues.keySet()) {
            nodeSet.addNode(buildAdditionalParameterElement(dom, MCREditorSubmission.PREFIX_DEFAULT_VALUE + xPath,
                defaultValues.get(xPath)));
        }

        editorSession.setBreakpoint("After transformation to HTML");
        nodeSet.addNode(buildAdditionalParameterElement(dom, MCREditorSessionStore.XEDITOR_SESSION_PARAM,
            editorSession.getCombinedSessionStepID()));

        return nodeSet;
    }

    private org.w3c.dom.Element buildAdditionalParameterElement(org.w3c.dom.Document doc, String name, String value) {
        org.w3c.dom.Element element = doc.createElement("input");
        element.setAttribute("type", "hidden");
        element.setAttribute("name", name);
        element.setAttribute("value", value);
        return element;
    }

    void addCleanupRule(String xPath, String relevantIf) {
        editorSession.getXMLCleaner().addRule(xPath, relevantIf);
    }

    void declareParameter(String name, String defaultValue) {
        Object currentValue = editorSession.getVariables().get(name);

        if ((currentValue == null) || Objects.equals(currentValue, "")) {
            editorSession.getVariables().put(name, defaultValue == null ? "" : defaultValue);
        }
    }
}
