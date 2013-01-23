package org.mycore.frontend.editor.def;

import org.jdom2.Element;

public abstract class MCRTransformerBase implements MCRTransformer {

    protected int getAttributeValue(Element element, String attributeName, int defaultValue) {
        String value = element.getAttributeValue(attributeName, "");
        if (value.isEmpty())
            return defaultValue;
        else
            return Integer.parseInt(value);
    }

    public abstract void transform(Element element) throws Exception;
}
