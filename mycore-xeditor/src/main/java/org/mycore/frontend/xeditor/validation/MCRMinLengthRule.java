package org.mycore.frontend.xeditor.validation;

import org.w3c.dom.Node;

public class MCRMinLengthRule extends MCRValidationRule {

    private int minLength;

    public MCRMinLengthRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
        minLength = Integer.parseInt(getAttributeValue("minLength"));
    }

    @Override
    protected boolean isValid(String value) {
        return minLength <= value.length();
    }
}