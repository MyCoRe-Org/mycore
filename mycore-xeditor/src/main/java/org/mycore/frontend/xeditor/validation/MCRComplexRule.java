package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.editor.validation.MCRValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRXPathBuilder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MCRComplexRule extends MCRValidationRule {

    private MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();

    public MCRComplexRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);

        NamedNodeMap attributes = ruleElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            validator.setProperty(attribute.getNodeName(), attribute.getNodeValue());
        }
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
            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }
}