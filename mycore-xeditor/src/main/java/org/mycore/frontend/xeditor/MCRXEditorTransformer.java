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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.util.IteratorIterable;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRWrappedContent;
import org.mycore.common.content.MCRXMLContent;
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
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformer {

    private static final Logger LOGGER = LogManager.getLogger(MCRXEditorTransformer.class);

    static final Namespace NS_XED = Namespace.getNamespace("xed", "http://www.mycore.de/xeditor");

    int anchorID = 0;

    private MCREditorSession editorSession;

    MCRBinding currentBinding;

    MCRParameterCollector parameters;

    MCRIncludeHandler includer = new MCRIncludeHandler();

    ElementDispatcher dispatcher = new ElementDispatcher(this);

    public MCRXEditorTransformer(MCREditorSession editorSession, MCRParameterCollector parameters) {
        this.editorSession = editorSession;
        this.parameters = parameters;
    }

    boolean shouldBuildFilterXSL() {
        return "true".equals(parameters.getParameter("buildFilterXSL", "false"));
    }

    public MCRContent transform(MCRContent editorSource)
        throws IOException, JDOMException, SAXException {
        getEditorSession().getValidator().clearRules();
        getEditorSession().getSubmission().clear();

        MCRContent resultFromStep1 = transformXEDandHTML(editorSource);
        MCRContent resultFromStep2 = cleanupXEDandCustomizeUsingXSL(resultFromStep1);

        getEditorSession().getValidator().clearValidationResults();
        return removeObsoleteXEDNamespace(resultFromStep2);
    }

    private MCRJDOMContent transformXEDandHTML(MCRContent editorSource)
        throws JDOMException, IOException, SAXException {
        Document editorXML = editorSource.asXML();

        IteratorIterable<Element> found = editorXML.getDescendants(new ElementFilter("form", NS_XED));
        if (found.hasNext()) {
            try {
                dispatcher.transform(found.next());
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        MCRJDOMContent resultFromStep1 = new MCRJDOMContent(editorXML);
        resultFromStep1.setFormat(Format.getRawFormat());
        debugTransformation("step 1", resultFromStep1);
        return resultFromStep1;
    }

    private MCRContent cleanupXEDandCustomizeUsingXSL(MCRContent resultFromStep1) throws IOException {
        MCRParameterizedTransformer step2Transformer = (MCRParameterizedTransformer) (MCRContentTransformerFactory
            .getTransformer("xeditor"));
        MCRContent resultFromStep2 = step2Transformer.transform(resultFromStep1, parameters);
        if (resultFromStep2 instanceof MCRWrappedContent
            && resultFromStep2.getClass().getName().contains(MCRXSLTransformer.class.getName())) {
            //lazy transformation make JUnit tests fail
            resultFromStep2 = ((MCRWrappedContent) resultFromStep2).getBaseContent();
        }

        LOGGER.debug("step 2: " + resultFromStep2.getClass().getName());
        if (resultFromStep2 instanceof MCRXMLContent) {
            ((MCRXMLContent) resultFromStep2).setFormat(Format.getRawFormat());
        }
        debugTransformation("step 2", resultFromStep2);
        return resultFromStep2;
    }

    private MCRJDOMContent removeObsoleteXEDNamespace(MCRContent resultFromStep2)
        throws JDOMException, IOException, SAXException {
        Document doc = resultFromStep2.asXML();
        doc.getDescendants(new ElementFilter()).forEach(e -> e.removeNamespaceDeclaration(NS_XED));

        MCRJDOMContent resultFromStep3 = new MCRJDOMContent(doc);
        //resultFromStep3.setFormat(Format.getRawFormat());
        debugTransformation("step 3", resultFromStep3);
        return resultFromStep3;
    }

    private void debugTransformation(String step, MCRContent content) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(step + ":\n" + content.asString());
        }
    }

    void bind(String xPath, String initialValue, String name) throws JDOMException, JaxenException {
        if (getEditorSession().getEditedXML() == null) {
            createEmptyDocumentFromXPath(xPath);
        }

        if (currentBinding == null) {
            currentBinding = getEditorSession().getRootBinding();
        }

        setCurrentBinding(new MCRBinding(xPath, initialValue, name, currentBinding));
    }

    private void createEmptyDocumentFromXPath(String xPath) throws JaxenException, JDOMException {
        Element root = buildRootElement(xPath);
        getEditorSession().setEditedXML(new Document(root));
        getEditorSession().setBreakpoint("Starting with empty XML document");
    }

    private Element buildRootElement(String xPath) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        LocationPath lp = (LocationPath) (baseXPath.getRootExpr());
        NameStep nameStep = (NameStep) (lp.getSteps().get(0));
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRConstants.getStandardNamespace(prefix);
        Element root = new Element(nameStep.getLocalName(), ns);
        return root;
    }

    void setDefault(String value) throws JaxenException, JDOMException {
        String xPath = getAbsoluteXPath();
        if (shouldBuildFilterXSL()) {
            String suffix = (currentBinding.getBoundNode() instanceof Element ? "/@" : "") + "_values_";
            new MCRBinding(xPath + suffix, value, null, getEditorSession().getRootBinding());
        }

        currentBinding.setDefault(value);
        getEditorSession().getSubmission().markDefaultValue(xPath, value);
    }

    void setCurrentBinding(MCRBinding binding) {
        this.currentBinding = binding;
        getEditorSession().getValidator().setValidationMarker(currentBinding);
    }

    void unbind() {
        setCurrentBinding(currentBinding.getParent());
    }

    String getAbsoluteXPath() {
        return currentBinding.getAbsoluteXPath();
    }

    /** This is called to check for matching select/option or checkbox values **/
    boolean hasValue(String value) {
        getEditorSession().getSubmission().mark2checkResubmission(currentBinding);
        return currentBinding.hasValue(value);
    }

    MCRXPathEvaluator getXPathEvaluator() {
        if (currentBinding != null) {
            return currentBinding.getXPathEvaluator();
        } else {
            return new MCRXPathEvaluator(getEditorSession().getVariables(), (Parent) null);
        }
    }

    MCRRepeatBinding getCurrentRepeat() {
        MCRBinding binding = currentBinding;
        while (!(binding instanceof MCRRepeatBinding)) {
            binding = binding.getParent();
        }
        return (MCRRepeatBinding) binding;
    }

    boolean hasValidationError() {
        return getEditorSession().getValidator().hasError(currentBinding);
    }

    boolean hasValidationError(String xPath) throws JaxenException, JDOMException {
        if (!xPath.isEmpty()) {
            bind(xPath, null, null);
        }
        boolean hasError = hasValidationError();
        if (!xPath.isEmpty()) {
            unbind();
        }
        return hasError;
    }

    MCREditorSession getEditorSession() {
        return editorSession;
    }
}

class ElementDispatcher {

    private Map<String, ElementTransformer> handlers = new HashMap<String, ElementTransformer>();

    private ElementTransformer defaultHandler = new ElementTransformer();

    ElementDispatcher(MCRXEditorTransformer transformer) {
        handlers.put("xed:form", new XedForm());
        handlers.put("xed:source", new XedSource());
        handlers.put("xed:cancel", new XedCancel());
        handlers.put("xed:post-processor", new XedPostProcessor());
        handlers.put("xed:bind", new XedBind());
        handlers.put("xed:repeat", new XedRepeat());
        handlers.put("xed:repeated", new IgnoringTransformer());
        handlers.put("xed:controls", new XedControls());
        handlers.put("xed:if", new XedIf());
        handlers.put("xed:choose", new XedChoose());
        handlers.put("xed:multi-lang", new XedMultiLang());
        handlers.put("xed:lang", new IgnoringTransformer());
        handlers.put("xed:output", new XedOutput());
        handlers.put("xed:load-resource", new XedLoadResource());
        handlers.put("xed:preload", new XedPreload());
        handlers.put("xed:include", new XedInclude());
        handlers.put("xed:template", new IgnoringTransformer());
        handlers.put("xed:validate", new XedValidate());
        handlers.put("xed:display-validation-message", new XedDisplayValidationMessage());
        handlers.put("xed:display-validation-messages", new XedDisplayValidationMessages());
        handlers.put("xed:cleanup-rule", new XedCleanupRule());
        handlers.put("xed:param", new XedParam());
        handlers.put("input", new HtmlInput());
        handlers.put("textarea", new HtmlTextarea());
        handlers.put("button", new HtmlSubmitButton());
        handlers.put("select", new HtmlSelect());
        handlers.put("option", new HtmlOption());

        Stream.concat(handlers.values().stream(), Stream.of(defaultHandler))
            .forEach(h -> h.setEditorTransformer(transformer));
    }

    void transform(Element e) throws Exception {
        String qName = e.getQualifiedName();
        handlers.getOrDefault(qName, defaultHandler).transform(e);
    }
}

class ElementTransformer {

    MCRXEditorTransformer transformer;

    void setEditorTransformer(MCRXEditorTransformer transformer) {
        this.transformer = transformer;
    }

    void transform(Element e) throws Exception {
        traverse(e);
    }

    void traverse(Element e) throws Exception {
        handleAttributes(e);
        handleChildren(e);
    }

    void handleAttributes(Element parent) {
        parent.getAttributes()
            .forEach(a -> a.setValue(transformer.getXPathEvaluator().replaceXPaths(a.getValue(), false)));
    }

    void handleChildren(Element parent)
        throws Exception {
        for (Element e : parent.getChildren()) {
            transformer.dispatcher.transform(e);
        }
    }
}

class IgnoringTransformer extends ElementTransformer {
    @Override
    void transform(Element e) {
        // Ignore this element and do nothing
    }
}

/** Handles <xed:source uri="" /> **/
class XedSource extends ElementTransformer {
    @Override
    void transform(Element e) throws JDOMException, IOException, SAXException, TransformerException {
        transformer.getEditorSession().setEditedXML(e.getAttributeValue("uri"));
    }
}

/** Handles <xed:cancel url="" /> **/
class XedCancel extends ElementTransformer {
    @Override
    void transform(Element e) {
        transformer.getEditorSession().setCancelURL(e.getAttributeValue("url"));
    }
}

/** Handles <xed:post-processor class="" xsl="" /> **/
class XedPostProcessor extends ElementTransformer {
    @Override
    void transform(Element e) {
        setPostProcessorClass(e);

        HashMap<String, String> attributeMap = new HashMap<>();
        for (Attribute a : e.getAttributes()) {
            attributeMap.put(a.getName(), a.getValue());
        }

        transformer.getEditorSession().getPostProcessor().setAttributes(attributeMap);
    }

    private void setPostProcessorClass(Element e) {
        String clazz = e.getAttributeValue("class", "");
        if (clazz.isBlank()) {
            return;
        }

        try {
            MCRXEditorPostProcessor instance = ((MCRXEditorPostProcessor) MCRClassTools.forName(clazz)
                .getDeclaredConstructor()
                .newInstance());
            transformer.getEditorSession().setPostProcessor(instance);
        } catch (ReflectiveOperationException ex) {
            throw new MCRException("Could not initialize Post-Processor with class" + clazz, ex);
        }
    }
}

/** <xed:bind xpath="" initially="value"|default="value"|set="value" name="" /> **/
class XedBind extends ElementTransformer {
    @Override
    void transform(Element e)
        throws Exception {

        String xPath = e.getAttributeValue("xpath");
        String initialValue = transformer.getXPathEvaluator().replaceXPaths(e.getAttributeValue("initially", ""),
            false);
        String name = e.getAttributeValue("name", (String) null);

        transformer.bind(xPath, initialValue, name);

        if (e.getAttribute("set") != null) {
            String value = transformer.getXPathEvaluator().replaceXPaths(e.getAttributeValue("set", ""), false);
            transformer.currentBinding.setValues(value);
        }

        if (e.getAttribute("default") != null) {
            String value = transformer.getXPathEvaluator().replaceXPaths(e.getAttributeValue("default", ""), false);
            transformer.setDefault(value);
        }

        handleChildren(e);

        transformer.unbind();
    }
}

/** Handles <xed:form /> **/
class XedForm extends ElementTransformer {
    @Override
    void transform(Element e)
        throws Exception {
        e.setNamespace(null);
        registerAdditionalNamespaces(e);

        // method="post" is default, may be overwritten by xed:form/@method
        String method = e.getAttributeValue("method", "post");
        e.setAttribute("method", method);

        if ("output".equals(method)) {
            traverse(e);
        } else {
            String servletsBaseURL = transformer.parameters.getParameter("ServletsBaseURL", "");
            String httpSession = transformer.parameters.getParameter("HttpSession", "");
            String action = servletsBaseURL + "XEditor" + httpSession;
            e.setAttribute("action", action);

            traverse(e);

            e.addContent(passAdditionalParameters());
        }
    }

    private void registerAdditionalNamespaces(Element e) {
        for (Namespace ns : e.getAdditionalNamespaces()) {
            MCRConstants.registerNamespace(ns);
        }
    }

    private Element passAdditionalParameters() {
        MCREditorSession session = transformer.getEditorSession();

        Map<String, String> aps = new LinkedHashMap<String, String>();

        Map<String, String[]> parameters = session.getRequestParameters();
        parameters.entrySet().forEach(entry -> {
            String name = entry.getKey();
            Arrays.stream(entry.getValue()).filter(value -> value != null && !value.isEmpty())
                .forEach(value -> aps.put(name, value));
        });

        String xPaths2CheckResubmission = session.getSubmission().getXPaths2CheckResubmission();
        if (!xPaths2CheckResubmission.isEmpty()) {
            aps.put(MCREditorSubmission.PREFIX_CHECK_RESUBMISSION, xPaths2CheckResubmission);
        }

        session.getSubmission().getDefaultValues().forEach(
            (xPath, value) -> aps.put(MCREditorSubmission.PREFIX_DEFAULT_VALUE + xPath, value));

        transformer.getEditorSession().setBreakpoint("After transformation to HTML");

        aps.put(MCREditorSessionStore.XEDITOR_SESSION_PARAM, session.getCombinedSessionStepID());

        Element div = new Element("div");
        div.setAttribute("style", "visibility:hidden");
        aps.forEach((name, value) -> div.addContent(buildHiddenInput(name, value)));
        return div;
    }

    private Element buildHiddenInput(String name, String value) {
        Element element = new Element("input");
        element.setAttribute("type", "hidden");
        element.setAttribute("name", name);
        element.setAttribute("value", value);
        return element;
    }
}

/** xed:preload uri="" static="true|false" **/
class XedPreload extends ElementTransformer {
    @Override
    void transform(Element e) throws TransformerException, TransformerFactoryConfigurationError {
        String uri = transformer.getXPathEvaluator().replaceXPaths(e.getAttributeValue("uri"), false);
        String sStatic = e.getAttributeValue("static");
        transformer.includer.preloadFromURIs(uri, sStatic);
    }
}

/** xed:include uri="" ref="" static="true|false" **/
class XedInclude extends ElementTransformer {
    @Override
    void transform(Element e) throws Exception {
        String uri = e.getAttributeValue("uri", "");
        String ref = e.getAttributeValue("ref", "");
        String sStatic = e.getAttributeValue("static", "");

        Element resolved = null;

        if (uri.isEmpty()) {
            resolved = transformer.includer.resolve(ref);

            if (resolved == null) {
                resolved = lookup(e.getDocument().getRootElement(), ref);
            }
        } else {
            resolved = transformer.includer.resolve(uri, sStatic);

            if (!ref.isEmpty()) {
                resolved = lookup(resolved, ref);
            }
        }

        if (resolved != null) {
            e.addContent(resolved.cloneContent());
            traverse(e);
        }
    }

    private Element lookup(Element container, String ref) {
        String xPath = "descendant::*[@id='" + ref + "']";
        XPathFactory f = XPathFactory.instance();
        return f.compile(xPath, Filters.element(), null, MCRXEditorTransformer.NS_XED).evaluateFirst(container);
    }
}

/** xed:load.resource uri="" name="" **/
class XedLoadResource extends ElementTransformer {
    @Override
    void transform(Element e) {
        String uri = transformer.getXPathEvaluator().replaceXPaths(e.getAttributeValue("uri"), false);
        String name = e.getAttributeValue("name");
        Element resource = MCRURIResolver.instance().resolve(uri);
        transformer.getEditorSession().getVariables().put(name, resource);
    }
}

/** xed:output value="" i18n="" **/
class XedOutput extends ElementTransformer {
    @Override
    void transform(Element e) {
        String value = transformer.getXPathEvaluator().replaceXPathOrI18n(e.getAttributeValue("value", ""));
        String i18n = e.getAttributeValue("i18n", "");

        if (i18n.isEmpty()) {
            if (value.isEmpty()) {
                value = transformer.currentBinding.getValue();
            }
        } else {
            i18n = transformer.getXPathEvaluator().replaceXPaths(i18n, false);
            value = value.isEmpty() ? MCRTranslation.translate(i18n) : MCRTranslation.translate(i18n, value);
        }

        e.setText(value);
    }
}

/** HTML input */
class HtmlInput extends HtmlSubmitButton {

    private static final String HTML_INPUT_TYPES = "text|password|hidden|file|color|date|datetime|datetime-local|"
        + "email|month|number|range|search|tel|time|url|week|radio|checkbox";

    @Override
    void transform(Element e) throws Exception {
        String type = e.getAttributeValue("type", "text");
        if (type.matches(HTML_INPUT_TYPES)) {
            String xPath = transformer.getAbsoluteXPath();

            if ("checkbox".equals(type) && xPath.endsWith("[1]")) {
                // If we are bound to the first element, it means we are bound to all elements -> MCR-2140
                xPath = xPath.substring(0, xPath.length() - 3);
            }

            e.setAttribute("name", xPath);

            if ("radio".equals(type) || "checkbox".equals(type)) {
                if (transformer.hasValue(e.getAttributeValue("value"))) {
                    e.setAttribute("checked", "checked");
                }
            } else {
                e.setAttribute("value", transformer.currentBinding.getValue());
            }

            handleAttributes(e);
        } else {
            super.transform(e);
        }
    }
}

/** html input/button[@type='submit|image'] | **/
class HtmlSubmitButton extends ElementTransformer {
    @Override
    void transform(Element e) throws Exception {
        if (e.getAttributeValue("type", "").matches("submit|image")) {
            String target = e.getAttributeValue("target", MCRXEditorTransformer.NS_XED);
            e.removeAttribute("target", MCRXEditorTransformer.NS_XED);

            if (!target.isEmpty()) {
                String href = e.getAttributeValue("href", MCRXEditorTransformer.NS_XED, "");
                e.removeAttribute("href", MCRXEditorTransformer.NS_XED);

                String name = "_xed_submit_" + target;
                if ("subselect".equals(target)) {
                    name += ":" + transformer.getAbsoluteXPath() + ":" + MCRSubselectTarget.encode(href);
                } else if (!href.isEmpty()) {
                    name += ":" + href;
                }

                e.setAttribute("name", name);
            }
        }

        handleAttributes(e);
    }
}

/** html textarea **/
class HtmlTextarea extends ElementTransformer {
    @Override
    void transform(Element e) {
        e.setAttribute("name", transformer.getAbsoluteXPath());
        e.setText(transformer.currentBinding.getValue());
    }
}

/** html select **/
class HtmlSelect extends ElementTransformer {
    @Override
    void transform(Element e)
        throws Exception {
        boolean multiple = "multiple".equals(e.getAttributeValue("multiple"));

        String xPath = transformer.getAbsoluteXPath();
        if (multiple && xPath.endsWith("[1]")) {
            // If we are bound to the first element, it means we are bound to all elements -> MCR-2140
            xPath = xPath.substring(0, xPath.length() - 3);
        }
        e.setAttribute("name", xPath);

        traverse(e);
    }
}

/** html select/option **/
class HtmlOption extends ElementTransformer {
    @Override
    void transform(Element e)
        throws Exception {
        if (isWithinSelect(e)) {
            String value = e.getAttributeValue("value", e.getTextTrim());
            if ((!value.isEmpty()) && transformer.hasValue(value)) {
                e.setAttribute("selected", "selected");
            }
        }
        traverse(e);
    }

    private boolean isWithinSelect(Element e) {
        Element parent = e.getParentElement();
        if (parent == null) {
            return false;
        }
        if (parent.getName().equals("select")) {
            return true;
        }
        return isWithinSelect(parent);
    }
}

/** xed:if test="" **/
class XedIf extends ElementTransformer {
    @Override
    void transform(Element e) throws Exception {
        if (transformer.getXPathEvaluator().test(e.getAttributeValue("test"))) {
            traverse(e);
        } else {
            e.removeContent();
        }
    }
}

/** xed:choose/xed:when[@test=""]|xed:otherwise **/
class XedChoose extends ElementTransformer {
    @Override
    void transform(Element e)
        throws Exception {
        Element otherwise = e.getChild("otherwise", MCRXEditorTransformer.NS_XED);
        if (otherwise != null) {
            otherwise.setName("when").setAttribute("test", "boolean(true)");
        }

        boolean found = false;
        for (Element when : e.getChildren("when", MCRXEditorTransformer.NS_XED)) {
            if (found || !transformer.getXPathEvaluator().test(when.getAttributeValue("test"))) {
                when.removeContent();
            } else {
                found = true;
                traverse(when);
            }
        }
    }
}

/** xed:multi-lang/xed:lang[@xml:lang=''] **/
class XedMultiLang extends ElementTransformer {

    String currentLang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();

    String defaultLang = MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse(MCRConstants.DEFAULT_LANG);

    @Override
    void transform(Element e) throws Exception {
        List<Element> children = e.getChildren("lang", MCRXEditorTransformer.NS_XED);
        Element choosen = children
            .stream().filter(l -> currentLang.equals(l.getAttributeValue("lang", MCRConstants.XML_NAMESPACE)))
            .findFirst()
            .orElse(
                children.stream()
                    .filter(l -> defaultLang.equals(l.getAttributeValue("lang", MCRConstants.XML_NAMESPACE)))
                    .findFirst()
                    .orElse(children.get(0)));

        for (Element lang : children) {
            if (lang.equals(choosen)) {
                traverse(lang);
            } else {
                lang.removeContent();
            }
        }
    }
}

/** xed:repeat min="" max="" xpath="" method="build|clone" **/
class XedRepeat extends ElementTransformer {
    @Override
    void transform(Element e) throws Exception {
        int minRepeats = Integer.parseInt(e.getAttributeValue("min", "1"));
        int maxRepeats = Integer.parseInt(e.getAttributeValue("max", "0"));
        int numRepeatsToBuild = transformer.shouldBuildFilterXSL() ? 1 : minRepeats;

        String xPath = e.getAttributeValue("xpath", "");
        String method = e.getAttributeValue("method", "");

        MCRRepeatBinding repeat = new MCRRepeatBinding(xPath, transformer.currentBinding, numRepeatsToBuild, maxRepeats,
            method);
        int numRepeats = repeat.getBoundNodes().size();
        transformer.setCurrentBinding(repeat);

        Element repeatedTemplate = new Element("repeated", MCRXEditorTransformer.NS_XED);
        repeatedTemplate.addContent(e.removeContent());

        for (int i = 1; i <= numRepeats; i++) {
            e.addContent(new Element("a").setAttribute("id", "rep-" + ++transformer.anchorID));

            MCRBinding repeatedBinding = repeat.bindRepeatPosition();
            transformer.setCurrentBinding(repeatedBinding);
            transformer.getEditorSession().getValidator().setValidationMarker(repeatedBinding);

            Element repeated = repeatedTemplate.clone();
            e.addContent(repeated);
            traverse(repeated);

            transformer.unbind();
        }
        transformer.unbind();
    }
}

/** xed:controls **/
class XedControls extends ElementTransformer {
    @Override
    void transform(Element e) throws JaxenException {
        String controls = e.getText().isBlank() ? "insert remove up down" : e.getText();
        e.setText(null);

        MCRRepeatBinding rb = transformer.getCurrentRepeat();
        int pos = rb.getRepeatPosition();
        int num = rb.getBoundNodes().size();
        int max = rb.getMaxRepeats();

        for (String control : controls.split("\\s+")) {
            if ("append".equals(control) && (pos < num)) {
                continue;
            } else if ("up".equals(control) && (pos == 1)) {
                continue;
            } else if ("down".equals(control) && (pos == num)) {
                continue;
            } else if ("insert".equals(control) && (num == max)) {
                continue;
            } else if ("append".equals(control) && (num == max)) {
                continue;
            } else {
                e.addContent(buildControl(control, rb));
            }
        }
    }

    private Element buildControl(String control, MCRRepeatBinding currentRepeat) throws JaxenException {
        Element c = new Element("control", MCRXEditorTransformer.NS_XED);
        c.setText(control);

        String name = "_xed_submit_" + control + ":";
        if ("append".equals(control) || "insert".equals(control)) {
            name += MCRInsertTarget.getInsertParameter(currentRepeat);
        }
        if ("remove".equals(control)) {
            name += transformer.getAbsoluteXPath();
        }
        if ("up".equals(control)) {
            name += MCRSwapTarget.getSwapParameter(currentRepeat, MCRSwapTarget.MOVE_UP);
        }
        if ("down".equals(control)) {
            name += MCRSwapTarget.getSwapParameter(currentRepeat, MCRSwapTarget.MOVE_DOWN);
        }

        boolean firstToRemove = "remove".equals(control) && (currentRepeat.getRepeatPosition() > 1);
        name += "|rep-" + (firstToRemove ? transformer.anchorID - 1 : transformer.anchorID);
        c.setAttribute("name", name);

        return c;
    }
}

/** xed:validate **/
class XedValidate extends ElementTransformer {
    @Override
    void transform(Element e) throws JaxenException, JDOMException {
        transformer.getEditorSession().getValidator().addRule(transformer.getAbsoluteXPath(), e);

        if (e.getAttributeValue("display").contains("here")
            && transformer.hasValidationError(e.getAttributeValue("xpath", ""))) {
            e.setAttribute("hasError", "true");
        }
    }
}

/** xed:display-validation-message **/
class XedDisplayValidationMessage extends ElementTransformer {
    @Override
    void transform(Element e) {
        if (transformer.hasValidationError()) {
            for (MCRValidator failedRule : transformer.getEditorSession().getValidator().getFailedRules()) {
                if (failedRule.getAttributeValue("display").contains("local")) {
                    e.addContent(failedRule.getRuleElement().clone().setAttribute("hasError", "true"));
                }
            }
        }
    }
}

/** xed:display-validation-messages **/
class XedDisplayValidationMessages extends ElementTransformer {
    @Override
    void transform(Element e) throws JaxenException, JDOMException {
        for (MCRValidator failedRule : transformer.getEditorSession().getValidator().getFailedRules()) {
            if (failedRule.getAttributeValue("display").contains("global")) {
                e.addContent(failedRule.getRuleElement().clone().setAttribute("hasError", "true"));
            }
        }
    }
}

/** xed:cleanup-rule xpath="" relevantIf="" **/
class XedCleanupRule extends ElementTransformer {
    @Override
    void transform(Element e) {
        String xPath = e.getAttributeValue("xpath");
        String relevantIf = e.getAttributeValue("relevantIf");
        transformer.getEditorSession().getXMLCleaner().addRule(xPath, relevantIf);
    }
}

/** xed:param name="" default="" **/
class XedParam extends ElementTransformer {
    @Override
    void transform(Element e) {
        String name = e.getAttributeValue("name");
        Object currentValue = transformer.getEditorSession().getVariables().get(name);

        if ((currentValue == null) || "".equals(currentValue)) {
            String defaultValue = e.getAttributeValue("default", "");
            transformer.getEditorSession().getVariables().put(name, defaultValue);
        }
    }
}
