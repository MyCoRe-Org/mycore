package org.mycore.frontend.editor.validation;

import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Element;

public class MCRCondition {

    private Element xml;

    private MCRValidator validator;

    public MCRCondition(Element xml, MCRValidator validator) {
        this.xml = xml;
        this.validator = validator;

        configureValidator();
    }

    public String getID() {
        return xml.getAttributeValue("id");
    }

    public boolean isRequired() {
        return "true".equals(xml.getAttributeValue("required"));
    }

    private void configureValidator() {
        for (Attribute attribute : (List<Attribute>) (xml.getAttributes())) {
            if (!attribute.getValue().isEmpty())
                validator.setProperty(attribute.getName(), attribute.getValue());
        }
    }
}
