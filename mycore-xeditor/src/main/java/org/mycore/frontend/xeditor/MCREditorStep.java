package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.mycore.common.content.MCRSourceContent;
import org.xml.sax.SAXException;

public class MCREditorStep {

    private final static Logger LOGGER = Logger.getLogger(MCREditorStep.class);

    private Document editedXML;

    private Set<String> xPathsOfDisplayedFields = new HashSet<String>();

    public MCREditorStep(Document editedXML) {
        this.editedXML = editedXML;
        MCRUsedNamespaces.addNamespacesFrom(editedXML.getRootElement());
    }

    public static MCREditorStep loadFromURI(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        LOGGER.info("Reading edited XML from " + uri);
        Document xml = MCRSourceContent.getInstance(uri).asXML();
        return new MCREditorStep(xml);
    }

    public static MCREditorStep createFromXPath(String xPath) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        LocationPath lp = (LocationPath) (baseXPath.getRootExpr());
        NameStep nameStep = (NameStep) (lp.getSteps().get(0));
        String name = nameStep.getLocalName();
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRUsedNamespaces.getNamespace(prefix);
        Element root = new Element(name, ns);
        Document document = new Document(root);
        return new MCREditorStep(document);
    }

    public Document getDocument() {
        return editedXML;
    }

    public MCRBinding getRootBinding() throws JDOMException {
        return new MCRBinding(editedXML);
    }

    public MCRBinding bind(String xPath) throws JaxenException, JDOMException {
        return new MCRBinding(xPath, getRootBinding());
    }

    public void markAsTransformedToInputField(Object node) {
        String xPath = MCRXPathBuilder.buildXPath(node);
        LOGGER.debug("mark as used " + xPath);
        xPathsOfDisplayedFields.add(xPath);
    }

    private void markAsResubmittedFromInputField(Object node) {
        String xPath = MCRXPathBuilder.buildXPath(node);
        LOGGER.debug("set value of " + xPath);
        xPathsOfDisplayedFields.remove(xPath);
    }

    public void setSubmittedValues(String xPath, String[] values) throws JDOMException, JaxenException {
        MCRBinding binding = bind(xPath);
        List<Object> boundNodes = binding.getBoundNodes();

        while (boundNodes.size() < values.length) {
            Element newElement = binding.cloneBoundElement(boundNodes.size() - 1);
            markAsTransformedToInputField(newElement);
        }

        for (int i = 0; i < values.length; i++) {
            String value = values[i] == null ? "" : values[i].trim();
            binding.setValue(i, value);
            if (!value.isEmpty())
                markAsResubmittedFromInputField(boundNodes.get(i));
        }
    }

    public void removeDeletedNodes() throws JDOMException, JaxenException {
        for (String xPath : xPathsOfDisplayedFields)
            bind(xPath).detachBoundNodes();
    }

    public void forgetDisplayedFields() {
        xPathsOfDisplayedFields.clear();
    }
}
