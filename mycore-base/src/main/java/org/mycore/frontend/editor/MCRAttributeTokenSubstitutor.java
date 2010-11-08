package org.mycore.frontend.editor;

import java.util.List;

import org.jdom.Element;

public class MCRAttributeTokenSubstitutor {

    private Element xml;

    private MCRTokenSubstitutor tokenSubstitutor;

    public MCRAttributeTokenSubstitutor(Element xml, MCRParameters parameters) {
        this.tokenSubstitutor = new MCRTokenSubstitutor(parameters);
        this.xml = xml;
    }

    public String substituteTokens(String elementName, String attributeName, String defaultValue) {
        for (Element child : (List<Element>) (xml.getChildren(elementName))) {
            String value = child.getAttributeValue(attributeName, "");
            if (value.isEmpty())
                continue;

            value = tokenSubstitutor.substituteTokens(value);
            if (containsNoTokens(value))
                return value;
        }
        return defaultValue;
    }

    private static boolean containsNoTokens(String text) {
        return !text.contains("{");
    }
}
