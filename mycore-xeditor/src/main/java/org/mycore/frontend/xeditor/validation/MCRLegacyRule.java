package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.editor.validation.MCRValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MCRLegacyRule extends MCRValidationRule {

    private MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();

    public MCRLegacyRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);

        NamedNodeMap attributes = ruleElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            validator.setProperty(attribute.getNodeName(), attribute.getNodeValue());
        }
    }

    @Override
    protected boolean isValid(String value) {
        return validator.isValid(value);
    }
}