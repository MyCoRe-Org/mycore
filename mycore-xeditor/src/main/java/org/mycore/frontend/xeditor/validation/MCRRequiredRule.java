package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.xeditor.MCRBinding;

public class MCRRequiredRule extends MCRValidationRule {

    public MCRRequiredRule(String baseXPath, String relativeXPath) {
        super(baseXPath, relativeXPath);
    }

    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        String absPath = binding.getAbsoluteXPath();
        if (results.hasError(absPath)) // do not validate already invalid nodes
            return true;

        boolean isValid = false;

        // at least one value must exist
        for (Object node : binding.getBoundNodes())
            if (!MCRBinding.getValue(node).isEmpty())
                isValid = true;

        results.mark(absPath, isValid);
        return isValid;
    }
}