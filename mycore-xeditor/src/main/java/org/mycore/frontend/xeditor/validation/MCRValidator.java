package org.mycore.frontend.xeditor.validation;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public abstract class MCRValidator {

    private Node ruleElement;

    protected String xPath;

    public void init(String baseXPath, Node ruleElement) {
        Node relativeXPath = ruleElement.getAttributes().getNamedItem("xpath");
        this.xPath = relativeXPath != null ? relativeXPath.getNodeValue() : baseXPath;
        this.ruleElement = ruleElement;

        if (hasRequiredAttributes()) {
            configure();
        }
    }

    public abstract boolean hasRequiredAttributes();

    /** If validator uses properties to configure its behavior, override this */
    public void configure() {
    }

    public Node getRuleElement() {
        return ruleElement;
    }

    public String getAttributeValue(String name) {
        NamedNodeMap attributes = ruleElement.getAttributes();
        Node attribute = attributes.getNamedItem(name);
        return attribute == null ? null : attribute.getNodeValue();
    }

    public boolean hasAttributeValue(String name) {
        return (getAttributeValue(name) != null);
    }

    public boolean validate(MCRValidationResults results, MCRBinding root) throws JaxenException, JDOMException {
        MCRBinding binding = new MCRBinding(xPath, false, root);
        boolean isValid = validateBinding(results, binding);
        binding.detach();
        return isValid;
    }

    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        boolean isValid = true; // all nodes must validate
        for (Object node : binding.getBoundNodes()) {
            String absPath = MCRXPathBuilder.buildXPath(node);
            if (results.hasError(absPath)) {
                continue;
            }

            String value = MCRBinding.getValue(node);
            if (value.isEmpty()) {
                continue;
            }

            boolean result = isValid(value);
            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }

    protected boolean isValid(String value) {
        return true;
    }
}
