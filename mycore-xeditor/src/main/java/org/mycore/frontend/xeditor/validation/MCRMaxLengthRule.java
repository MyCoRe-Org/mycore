package org.mycore.frontend.xeditor.validation;

import org.w3c.dom.Node;

public class MCRMaxLengthRule extends MCRValidationRule {

    private int maxLength;

    public MCRMaxLengthRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
        maxLength = Integer.parseInt(getAttributeValue("maxLength"));
    }

    @Override
    protected boolean isValid(String value) {
        return value.length() <= maxLength;
    }
}