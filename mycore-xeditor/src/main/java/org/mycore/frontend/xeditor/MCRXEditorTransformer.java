/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xpath.NodeSet;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XNodeSetForDOM;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.xeditor.target.MCRSubselectTarget;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformer {

    private MCREditorSession editorSession;

    private MCRBinding currentBinding;

    private MCRParameterCollector transformationParameters;

    public MCRXEditorTransformer(MCREditorSession editorSession, MCRParameterCollector transformationParameters) {
        this.editorSession = editorSession;
        this.transformationParameters = transformationParameters;
    }

    public MCRContent transform(MCRContent editorSource) throws IOException, JDOMException, SAXException {
        editorSession.getValidator().removeValidationRules();

        MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer("xeditor");
        if (transformer instanceof MCRParameterizedTransformer) {
            String key = MCRXEditorTransformerStore.storeTransformer(this);
            transformationParameters.setParameter("XEditorTransformerKey", key);
            return ((MCRParameterizedTransformer) transformer).transform(editorSource, transformationParameters);
        } else {
            throw new MCRException("Xeditor needs parameterized MCRContentTransformer: " + transformer);
        }
    }

    public static MCRXEditorTransformer getTransformer(String key) {
        return MCRXEditorTransformerStore.getAndRemoveTransformer(key);
    }

    public void addNamespace(String prefix, String uri) {
        MCRUsedNamespaces.addNamespace(Namespace.getNamespace(prefix, uri));
    }

    public void readSourceXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        editorSession.setEditedXML(uri);
    }

    public void setCancelURL(String cancelURL) {
        editorSession.setCancelURL(cancelURL);
    }

    public void setPostProcessorXSL(String stylesheet) {
        editorSession.getPostProcessor().setStylesheet(stylesheet);
    }

    public String replaceParameters(String uri) {
        return editorSession.replaceParameters(uri);
    }

    public void bind(String xPath, String defaultValue, String name) throws JDOMException, JaxenException {
        if (editorSession.getEditedXML() == null)
            createEmptyDocumentFromXPath(xPath);

        if (currentBinding == null)
            currentBinding = editorSession.getRootBinding();

        currentBinding = new MCRBinding(xPath, defaultValue, name, currentBinding);
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
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRUsedNamespaces.getNamespace(prefix);
        return new Element(nameStep.getLocalName(), ns);
    }

    public void unbind() {
        currentBinding = currentBinding.getParent();
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

    public String replaceXPaths(String text) {
        return currentBinding.replaceXPaths(text);
    }

    public String replaceXPathOrI18n(String expression) {
        return currentBinding.replaceXPathOrI18n(expression);
    }

    public String evaluateXPath(String xPathExpression) {
        return currentBinding.evaluateXPath(xPathExpression);
    }

    public String repeat(String xPath, int minRepeats, int maxRepeats) throws JDOMException, JaxenException {
        MCRRepeatBinding repeat = new MCRRepeatBinding(xPath, currentBinding, minRepeats, maxRepeats);
        currentBinding = repeat;
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
        currentBinding = getCurrentRepeat().bindRepeatPosition();
    }

    public String getSwapParameter(int posA, int posB) {
        return getCurrentRepeat().getSwapParameter(posA, posB);
    }

    public void addValidationRule(NodeIterator attributes) {
        editorSession.getValidator().addValidationRule(currentBinding.getAbsoluteXPath(), attributes);
    }

    public boolean validationFailed() {
        return editorSession.getValidator().failed();
    }

    public boolean currentIsInvalid() {
        return editorSession.getValidator().failed(currentBinding.getAbsoluteXPath());
    }

    private List<String> subSelectHRefs = new ArrayList<String>();

    public String saveSubSelectHRef(String href) {
        subSelectHRefs.add(href);
        return currentBinding.getAbsoluteXPath() + ":" + subSelectHRefs.size();
    }

    public XNodeSet getAdditionalParameters(ExpressionContext context) throws ParserConfigurationException, TransformerException {
        org.w3c.dom.Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        NodeSet nodeSet = new NodeSet();

        Map<String, String[]> parameters = editorSession.getRequestParameters();
        for (String name : parameters.keySet())
            for (String value : parameters.get(name))
                if ((value != null) && !value.isEmpty())
                    nodeSet.addNode(buildAdditionalParameterElement(dom, name, value));

        for (String xPath : editorSession.getSubmission().getXPaths2CheckResubmission())
            nodeSet.addNode(buildAdditionalParameterElement(dom, "_xed_check", xPath));

        int i = 1;
        for (String href : subSelectHRefs)
            nodeSet.addNode(buildAdditionalParameterElement(dom, MCRSubselectTarget.PARAM_SUBSELECT_HREF + i++, href));

        nodeSet.addNode(buildAdditionalParameterElement(dom, "_xed_session", editorSession.getCombinedSessionStepID()));

        return new XNodeSetForDOM((NodeList) nodeSet, context.getXPathContext());
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
}
