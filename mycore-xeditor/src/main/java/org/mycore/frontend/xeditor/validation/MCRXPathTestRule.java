package org.mycore.frontend.xeditor.validation;

import java.util.List;

import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRXPathEvaluator;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MCRXPathTestRule extends MCRValidationRule {

    private String xPathExpression;

    public MCRXPathTestRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
        NamedNodeMap attributes = ruleElement.getAttributes();
        this.xPathExpression = attributes.getNamedItem("test").getNodeValue();
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
            MCRXPathEvaluator evaluator = new MCRXPathEvaluator(nodeBinding);
            boolean result = evaluator.test(xPathExpression);
            nodeBinding.detach();

            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }
}