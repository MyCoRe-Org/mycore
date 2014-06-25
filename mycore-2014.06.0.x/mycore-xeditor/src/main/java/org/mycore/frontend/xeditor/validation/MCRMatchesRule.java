package org.mycore.frontend.xeditor.validation;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MCRMatchesRule extends MCRValidationRule {

    private String pattern;

    public MCRMatchesRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
        NamedNodeMap attributes = ruleElement.getAttributes();
        this.pattern = attributes.getNamedItem("matches").getNodeValue();
    }

    @Override
    protected boolean isValid(String value) {
        return value.matches(pattern);
    }
}