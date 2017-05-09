package org.mycore.frontend.xeditor.validation;

import java.util.List;

import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.xeditor.MCRBinding;
import org.w3c.dom.Node;

public class MCRXPathTestRule extends MCRValidationRule {

    private String xPathExpression;

    public MCRXPathTestRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
        this.xPathExpression = getAttributeValue("test");
    }

    @Override
    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        boolean isValid = true; // all nodes must validate
        List<Object> boundNodes = binding.getBoundNodes();
        for (int i = 0; i < boundNodes.size(); i++) {
            Object node = boundNodes.get(i);

            String absPath = MCRXPathBuilder.buildXPath(node);
            if (results.hasError(absPath)) // do not validate already invalid nodes
                continue;

            MCRBinding nodeBinding = new MCRBinding(i + 1, binding);
            MCRXPathEvaluator evaluator = nodeBinding.getXPathEvaluator();
            boolean result = evaluator.test(xPathExpression);
            nodeBinding.detach();

            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }
}