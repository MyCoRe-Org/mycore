package org.mycore.frontend.xeditor.validation;

import org.w3c.dom.Node;

public class MCRMatchesRule extends MCRValidationRule {

    private String pattern;

    public MCRMatchesRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
        this.pattern = getAttributeValue("matches");
    }

    @Override
    protected boolean isValid(String value) {
        return value.matches(pattern);
    }
}