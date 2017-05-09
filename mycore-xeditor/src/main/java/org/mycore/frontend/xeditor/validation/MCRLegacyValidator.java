package org.mycore.frontend.xeditor.validation;

import java.util.stream.IntStream;

import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MCRLegacyValidator extends MCRValidator {

    private org.mycore.frontend.editor.validation.MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();

    @Override
    public boolean hasRequiredAttributes() {
        NamedNodeMap attributes = getRuleElement().getAttributes();
        return IntStream.range(0, attributes.getLength())
            .anyMatch(i -> "min max type class method".contains(attributes.item(i).getNodeName()));
    }

    @Override
    public void configure() {
        NamedNodeMap attributes = getRuleElement().getAttributes();
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