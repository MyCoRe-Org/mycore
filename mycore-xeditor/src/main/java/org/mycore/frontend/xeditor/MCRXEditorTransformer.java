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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
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
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRWrappedContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.xeditor.target.MCRInsertTarget;
import org.mycore.frontend.xeditor.target.MCRSubselectTarget;
import org.mycore.frontend.xeditor.target.MCRSwapTarget;
import org.mycore.frontend.xeditor.validation.MCRValidator;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformer {

    public int anchorID = 0;

    private MCREditorSession editorSession;

    private MCRBinding currentBinding;

    private MCRParameterCollector transformationParameters;

    private boolean withinSelectElement = false;

    public MCRXEditorTransformer(MCREditorSession editorSession, MCRParameterCollector transformationParameters) {
        this.editorSession = editorSession;
        this.transformationParameters = transformationParameters;
    }

    public static MCRXEditorTransformer getTransformer(String key) {
        return MCRXEditorTransformerStore.getAndRemoveTransformer(key);
    }

    public MCRContent transform(MCRContent editorSource) throws IOException, JDOMException, SAXException {
        editorSession.getValidator().clearRules();
        editorSession.getSubmission().clear();

        MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer("xeditor");
        if (transformer instanceof MCRParameterizedTransformer) {
            String key = MCRXEditorTransformerStore.storeTransformer(this);
            transformationParameters.setParameter("XEditorTransformerKey", key);
            MCRContent result = ((MCRParameterizedTransformer) transformer).transform(editorSource,
                transformationParameters);
            if (result instanceof MCRWrappedContent
                && result.getClass().getName().contains(MCRXSLTransformer.class.getName())) {
                //lazy transformation make JUnit tests fail
                result = ((MCRWrappedContent) result).getBaseContent();
            }
            editorSession.getValidator().clearValidationResults();
            return result;
        } else {
            throw new MCRException("Xeditor needs parameterized MCRContentTransformer: " + transformer);
        }
    }

    public void addNamespace(String prefix, String uri) {
        MCRConstants.registerNamespace(Namespace.getNamespace(prefix, uri));
    }

    public void readSourceXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        editorSession.setEditedXML(uri);
    }

    public void setCancelURL(String cancelURL) {
        editorSession.setCancelURL(cancelURL);
    }

    public void initializePostprocessor(Node postProcessorNode) {
        NamedNodeMap attributes = postProcessorNode.getAttributes();
        int attributesLength = attributes.getLength();
        HashMap<String, String> attributeMap = new HashMap<>();
        for (int i = 0; i < attributesLength; i++) {
            Attr item = (Attr) attributes.item(i); // this should be save because we called getAttributes earlier
            String attrName = item.getName();
            String attrValue = item.getValue();
            attributeMap.put(attrName, attrValue);
        }

        editorSession.getPostProcessor().setAttributes(attributeMap);
    }

    public void setPostProcessor(String clazz) {
        try {
            MCRXEditorPostProcessor instance = ((MCRXEditorPostProcessor) Class.forName(clazz).getDeclaredConstructor()
                .newInstance());
            editorSession.setPostProcessor(instance);
        } catch (ReflectiveOperationException e) {
            throw new MCRException("Could not initialize Post-Processor with class" + clazz, e);
        }
    }

    public String replaceParameters(String uri) {
        return getXPathEvaluator().replaceXPaths(uri, false);
    }

    public void bind(String xPath, String initialValue, String name) throws JDOMException, JaxenException {
        if (editorSession.getEditedXML() == null)
            createEmptyDocumentFromXPath(xPath);

        if (currentBinding == null)
            currentBinding = editorSession.getRootBinding();

        setCurrentBinding(new MCRBinding(xPath, initialValue, name, currentBinding));
    }

    private void setCurrentBinding(MCRBinding binding) {
        this.currentBinding = binding;
        editorSession.getValidator().setValidationMarker(currentBinding);
    }

    private void createEmptyDocumentFromXPath(String xPath) throws JaxenException, JDOMException {
        Element root = createRootElement(xPath);
        editorSession.setEditedXML(new Document(root));
        editorSession.setBreakpoint("Starting with empty XML document");
    }

    private Element createRootElement(String xPath) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        LocationPath lp = (LocationPath) (baseXPath.getRootExpr());
        NameStep nameStep = (NameStep) (lp.getSteps().get(0));
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRConstants.getStandardNamespace(prefix);
        return new Element(nameStep.getLocalName(), ns);
    }

    public void setValues(String value) {
        currentBinding.setValues(value);
    }

    public void setDefault(String value) {
        currentBinding.setDefault(value);
        editorSession.getSubmission().markDefaultValue(currentBinding.getAbsoluteXPath(), value);
    }

    public void unbind() {
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

    public void toggleWithinSelectElement() {
        withinSelectElement = !withinSelectElement;
    }

    public boolean isWithinSelectElement() {
        return withinSelectElement;
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

    public boolean test(String xPathExpression) {
        return getXPathEvaluator().test(xPathExpression);
    }

    public MCRXPathEvaluator getXPathEvaluator() {
        if (currentBinding != null)
            return currentBinding.getXPathEvaluator();
        else
            return new MCRXPathEvaluator(editorSession.getVariables(), (Parent) null);
    }

    public String repeat(String xPath, int minRepeats, int maxRepeats, String method)
        throws JDOMException, JaxenException {
        MCRRepeatBinding repeat = new MCRRepeatBinding(xPath, currentBinding, minRepeats, maxRepeats, method);
        setCurrentBinding(repeat);
        return StringUtils.repeat("a ", repeat.getBoundNodes().size());
    }

    private MCRRepeatBinding getCurrentRepeat() {
        MCRBinding binding = currentBinding;
        while (!(binding instanceof MCRRepeatBinding))
            binding = binding.getParent();
        return (MCRRepeatBinding) binding;
    }

    public int getNumRepeats() {
        return getCurrentRepeat().getBoundNodes().size();
    }

    public int getMaxRepeats() {
        return getCurrentRepeat().getMaxRepeats();
    }

    public int getRepeatPosition() {
        return getCurrentRepeat().getRepeatPosition();
    }

    public void bindRepeatPosition() throws JDOMException, JaxenException {
        setCurrentBinding(getCurrentRepeat().bindRepeatPosition());
        editorSession.getValidator().setValidationMarker(currentBinding);
    }

    public String getSwapParameter(String action) throws JaxenException {
        boolean direction = action.equals("down") ? MCRSwapTarget.MOVE_DOWN : MCRSwapTarget.MOVE_UP;
        return MCRSwapTarget.getSwapParameter(getCurrentRepeat(), direction);
    }

    public String getInsertParameter() throws JaxenException {
        return MCRInsertTarget.getInsertParameter(getCurrentRepeat());
    }

    public int nextAnchorID() {
        return ++anchorID;
    }

    public int getAnchorID() {
        return anchorID;
    }

    public int previousAnchorID() {
        return (anchorID == 0 ? 1 : anchorID - 1);
    }

    public void loadResource(String uri, String name) {
        Element resource = MCRURIResolver.instance().resolve(uri);
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
        for (MCRValidator failedRule : editorSession.getValidator().getFailedRules())
            nodeSet.addNode(failedRule.getRuleElement());
        return nodeSet;
    }

    public String getSubselectParam(String href) {
        return currentBinding.getAbsoluteXPath() + ":" + MCRSubselectTarget.encode(href);
    }

    public NodeSet getAdditionalParameters() throws ParserConfigurationException, TransformerException {
        org.w3c.dom.Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        NodeSet nodeSet = new NodeSet();

        Map<String, String[]> parameters = editorSession.getRequestParameters();
        for (String name : parameters.keySet())
            for (String value : parameters.get(name))
                if ((value != null) && !value.isEmpty())
                    nodeSet.addNode(buildAdditionalParameterElement(dom, name, value));

        String xPaths2CheckResubmission = editorSession.getSubmission().getXPaths2CheckResubmission();
        if (!xPaths2CheckResubmission.isEmpty())
            nodeSet.addNode(buildAdditionalParameterElement(dom, MCREditorSubmission.PREFIX_CHECK_RESUBMISSION,
                xPaths2CheckResubmission));

        Map<String, String> defaultValues = editorSession.getSubmission().getDefaultValues();
        for (String xPath : defaultValues.keySet())
            nodeSet.addNode(buildAdditionalParameterElement(dom, MCREditorSubmission.PREFIX_DEFAULT_VALUE + xPath,
                defaultValues.get(xPath)));

        editorSession.setBreakpoint("After transformation to HTML");
        nodeSet.addNode(buildAdditionalParameterElement(dom, MCREditorSessionStore.XEDITOR_SESSION_PARAM,
            editorSession.getCombinedSessionStepID()));

        return nodeSet;
    }

    private org.w3c.dom.Element buildAdditionalParameterElement(org.w3c.dom.Document doc, String name, String value) {
        org.w3c.dom.Element element = doc.createElement("param");
        element.setAttribute("name", name);
        element.setTextContent(value);
        return element;
    }

    public void addCleanupRule(String xPath, String relevantIf) {
        editorSession.getXMLCleaner().addRule(xPath, relevantIf);
    }

    public void declareParameter(String name, String defaultValue) {
        Object currentValue = editorSession.getVariables().get(name);

        if ((currentValue == null) || "".equals(currentValue))
            editorSession.getVariables().put(name, defaultValue == null ? "" : defaultValue);
    }
}
