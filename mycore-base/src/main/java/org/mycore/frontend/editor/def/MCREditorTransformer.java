package org.mycore.frontend.editor.def;

import org.jdom.Element;

public abstract class MCREditorTransformer {

    protected int getAttributeValue(Element element, String attributeName, int defaultValue) {
        String value = element.getAttributeValue(attributeName, "");
        if (value.isEmpty())
            return defaultValue;
        else
            return Integer.parseInt(value);
    }
}