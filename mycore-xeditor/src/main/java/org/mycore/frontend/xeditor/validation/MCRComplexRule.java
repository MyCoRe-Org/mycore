package org.mycore.frontend.xeditor.validation;

import java.util.Map;

import org.mycore.frontend.editor.validation.MCRValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRXPathBuilder;

public class MCRComplexRule extends MCRValidationRule {

    private MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();

    public MCRComplexRule(String baseXPath, String relativeXPath, Map<String, String> attributes) {
        super(baseXPath, relativeXPath);

        for (String name : attributes.keySet())
            validator.setProperty(name, attributes.get(name));
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

            boolean result = validator.isValid(value);
            results.mark(absPath, result);
            isValid = isValid && result;
        }
        return isValid;
    }
}