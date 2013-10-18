package org.mycore.frontend.xeditor.validation;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.MCRBinding;
import org.w3c.dom.Node;

public abstract class MCRValidationRule {

    private Node ruleElement;

    private String xPath;

    public MCRValidationRule(String baseXPath, Node ruleElement) {
        Node relativeXPath = ruleElement.getAttributes().getNamedItem("xPath");
        this.xPath = relativeXPath != null ? relativeXPath.getNodeValue() : baseXPath;
        this.ruleElement = ruleElement;
    }

    public boolean validate(MCRValidationResults results, MCRBinding root) throws JaxenException, JDOMException {
        MCRBinding binding = new MCRBinding(xPath, false, root);
        boolean isValid = validateBinding(results, binding);
        binding.detach();
        return isValid;
    }

    public abstract boolean validateBinding(MCRValidationResults results, MCRBinding binding);
}