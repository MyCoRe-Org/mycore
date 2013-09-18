package org.mycore.frontend.xeditor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

public class MCREditorStep implements Cloneable {

    private final static Logger LOGGER = Logger.getLogger(MCREditorStep.class);

    private String label;

    private Document editedXML;

    private Map<String, String[]> submittedValues;

    private Set<String> xPathsOfDisplayedFields = new HashSet<String>();

    public MCREditorStep(Document editedXML) {
        this.editedXML = editedXML;
        MCRUsedNamespaces.addNamespacesFrom(editedXML.getRootElement());
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public void setSubmittedValues(Map<String, String[]> values) throws JaxenException, JDOMException {
        this.submittedValues = values;

        for (String xPath : values.keySet())
            if (xPath.startsWith("/"))
                setSubmittedValues(xPath, values.get(xPath));

        emptyNotResubmittedNodes();
    }

    public Map<String, String[]> getSubmittedValues() {
        return submittedValues;
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
            markAsResubmittedFromInputField(boundNodes.get(i));
        }
    }

    public void emptyNotResubmittedNodes() throws JDOMException, JaxenException {
        for (String xPath : xPathsOfDisplayedFields) {
            bind(xPath).setValue("");
            xPathsOfDisplayedFields.remove(xPath);
        }
    }

    @Override
    public MCREditorStep clone() {
        Document xml = editedXML.getDocument().clone();
        MCREditorStep copy = new MCREditorStep(xml);
        copy.xPathsOfDisplayedFields.addAll(this.xPathsOfDisplayedFields);
        return copy;
    }
}
