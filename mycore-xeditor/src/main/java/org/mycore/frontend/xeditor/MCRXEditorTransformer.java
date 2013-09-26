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
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.jdom2.Verifier;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.services.i18n.MCRTranslation;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformer {

    private final static Logger LOGGER = Logger.getLogger(MCRXEditorTransformer.class);

    private MCREditorSession editorSession;

    private MCRParameterCollector transformationParameters;

    private MCRBinding currentBinding;

    public MCRXEditorTransformer(MCREditorSession editorSession, MCRParameterCollector transformationParameters) {
        this.editorSession = editorSession;
        this.transformationParameters = transformationParameters;
        removeIllegalVariables(transformationParameters.getParameterMap());
    }

    private void removeIllegalVariables(Map<String, Object> xPathVariables) {
        for (Iterator<Entry<String, Object>> entries = xPathVariables.entrySet().iterator(); entries.hasNext();) {
            String name = entries.next().getKey();
            String result = Verifier.checkXMLName(name);
            if (result != null) {
                LOGGER.warn("Illegally named transformation parameter, removing " + name);
                entries.remove();
            }
        }
    }

    public MCRContent transform(MCRContent editorSource) throws IOException, JDOMException, SAXException {
        editorSession.getValidator().removeValidationRules();

        MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer("xeditor");
        if (transformer instanceof MCRParameterizedTransformer) {
            String key = MCRXEditorTransformerStore.storeTransformer(this);
            transformationParameters.setParameter("XEditorTransformerKey", key);
            MCRContent result = ((MCRParameterizedTransformer) transformer).transform(editorSource, transformationParameters);
            return result;
        } else {
            throw new MCRException("Xeditor needs parameterized MCRContentTransformer: " + transformer);
        }
    }

    public static MCRXEditorTransformer getTransformer(String key) {
        return MCRXEditorTransformerStore.getAndRemoveTransformer(key);
    }

    public String getCombinedSessionStepID() {
        editorSession.setBreakpoint("After transformation to HTML");
        return editorSession.getID() + "-" + editorSession.getChangeTracker().getChangeCounter();
    }

    public void addNamespace(String prefix, String uri) {
        MCRUsedNamespaces.addNamespace(Namespace.getNamespace(prefix, uri));
    }

    public void readSourceXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        if ((editorSession.getEditedXML() != null) || (uri = replaceParameters(uri)).contains("{"))
            return;

        LOGGER.info("Reading edited XML from " + uri);
        Document xml = MCRSourceContent.getInstance(uri).asXML();
        editorSession.setEditedXML(xml);
        editorSession.setBreakpoint("Reading XML from " + uri);
    }

    public void setCancelURL(String url) throws JDOMException, IOException, SAXException, TransformerException {
        url = replaceParameters(url);
        if (!url.contains("{"))
            editorSession.setCancelURL(url);
    }

    public void setPostProcessorXSL(String stylesheet) {
        editorSession.getPostProcessor().setStylesheet(stylesheet);
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

    public String getControlsParameter() throws UnsupportedEncodingException {
        return getCurrentRepeat().getControlsParameter();
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

    private final static Pattern PATTERN_URI = Pattern.compile("\\{\\$(.+)\\}");

    public String replaceParameters(String uri) {
        Matcher m = PATTERN_URI.matcher(uri);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group(1);
            String value = transformationParameters.getParameter(token, null);
            m.appendReplacement(sb, value == null ? m.group().replace("$", "\\$") : value);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private final static Pattern PATTERN_XPATH = Pattern.compile("\\{([^\\}]+)\\}");

    public String replaceXPaths(String text) {
        Matcher m = PATTERN_XPATH.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find())
            m.appendReplacement(sb, replaceXPathOrI18n(m.group(1)));
        m.appendTail(sb);
        return sb.toString();
    }

    public String replaceXPathOrI18n(String expression) {
        if (expression.startsWith("i18n:")) {
            String key = expression.substring(5);
            int pos = key.indexOf(",");
            if (pos != -1) {
                String xPath = key.substring(pos + 1);
                String value = evaluateXPath(xPath);
                key = key.substring(0, pos);
                return MCRTranslation.translate(key, value);
            } else
                return MCRTranslation.translate(key);
        } else
            return evaluateXPath(expression);
    }

    public String evaluateXPath(String xPathExpression) {
        xPathExpression = "string(" + xPathExpression + ")";
        try {
            Map<String, Object> variables = currentBinding.buildXPathVariables();
            variables.putAll(transformationParameters.getParameterMap());

            List<Namespace> namespaces = MCRUsedNamespaces.getNamespaces();
            XPathFactory factory = XPathFactory.instance();
            XPathExpression<Object> xPath = factory.compile(xPathExpression, Filters.fpassthrough(), variables, namespaces);
            return xPath.evaluateFirst(currentBinding.getBoundNodes()).toString();
        } catch (Exception ex) {
            LOGGER.warn("unable to evaluate XPath: " + xPathExpression);
            LOGGER.debug(ex);
            return "";
        }
    }

    public XNodeSet getRequestParameters(ExpressionContext context) throws ParserConfigurationException, TransformerException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        NodeSet ns = new NodeSet();
        Map<String, String[]> parameters = editorSession.getRequestParameters();
        for (String name : parameters.keySet()) {
            for (String value : parameters.get(name)) {
                if ((value != null) && !value.isEmpty()) {
                    org.w3c.dom.Element element = doc.createElement("param");
                    element.setAttribute("name", name);
                    element.setTextContent(value);
                    ns.addNode(element);
                }
            }
        }
        return new XNodeSetForDOM((NodeList) ns, context.getXPathContext());
    }

    public XNodeSet getXPaths2CheckResubmission(ExpressionContext context) throws ParserConfigurationException, TransformerException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        NodeSet ns = new NodeSet();

        for (String xPath : editorSession.getSubmission().getXPaths2CheckResubmission()) {
            org.w3c.dom.Element element = doc.createElement("resubmit");
            element.setTextContent(xPath);
            ns.addNode(element);
        }
        return new XNodeSetForDOM((NodeList) ns, context.getXPathContext());
    }

    public void addCleanupRule(String xPath, String relevantIf) {
        editorSession.getXMLCleaner().addRule(xPath, relevantIf);
    }
}
