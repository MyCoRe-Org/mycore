package org.mycore.frontend.xeditor.validation;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRXPathBuilder;
import org.w3c.dom.Node;

public abstract class MCRValidationRule {

    private Node ruleElement;

    private String xPath;

    public MCRValidationRule(String baseXPath, Node ruleElement) {
        Node relativeXPath = ruleElement.getAttributes().getNamedItem("xpath");
        this.xPath = relativeXPath != null ? relativeXPath.getNodeValue() : baseXPath;
        this.ruleElement = ruleElement;
    }

    public Node getRuleElement() {
        return ruleElement;
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
            if (results.hasError(absPath)) // do not validate already invalid nodes
                continue;

            String value = MCRBinding.getValue(node);
            if (value.isEmpty()) // do not validate empty values
                continue;

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