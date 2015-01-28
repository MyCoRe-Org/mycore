package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.xeditor.MCRBinding;
import org.w3c.dom.Node;

public class MCRRequiredRule extends MCRValidationRule {

    public MCRRequiredRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
    }

    @Override
    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        boolean isValid = false;

        // at least one value must exist
        for (Object node : binding.getBoundNodes())
            if (!MCRBinding.getValue(node).isEmpty())
                isValid = true;

        if (binding.getBoundNode() != null) {
            String absPath = binding.getAbsoluteXPath();
            if (results.hasError(absPath)) // do not validate already invalid nodes
                return true;
            results.mark(absPath, isValid, this);
        }

        return isValid;
    }
}