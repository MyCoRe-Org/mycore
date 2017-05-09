package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.xeditor.MCRBinding;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MCRRequiredValidator extends MCRValidator {

    @Override
    public boolean hasRequiredAttributes() {
        NamedNodeMap attributes = getRuleElement().getAttributes();
        Node requiredAttribute = attributes.getNamedItem("required");
        return ((requiredAttribute != null) && "true".equals(requiredAttribute.getNodeValue()));
    }

    @Override
    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        if (binding.getBoundNodes().size() == 0) {
            String msg = "Condition for " + this.xPath + " can not be validated, no such XML source node";
            throw new RuntimeException(msg);
        }

        String absPath = binding.getAbsoluteXPath();
        if (results.hasError(absPath)) // do not validate already invalid nodes
            return true;

        boolean isValid = false;

        // at least one value must exist
        for (Object node : binding.getBoundNodes())
            if (!MCRBinding.getValue(node).isEmpty())
                isValid = true;

        results.mark(absPath, isValid, this);
        return isValid;
    }
}