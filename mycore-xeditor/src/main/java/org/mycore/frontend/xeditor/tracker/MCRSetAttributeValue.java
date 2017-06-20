package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Attribute;

public class MCRSetAttributeValue implements MCRChange {

    public static MCRChangeData setValue(Attribute attribute, String value) {
        MCRChangeData data = new MCRChangeData("set-attribute", attribute);
        attribute.setValue(value);
        return data;
    }

    public void undo(MCRChangeData data) {
        Attribute attribute = data.getAttribute();
        data.getContext().removeAttribute(attribute.getName(), attribute.getNamespace());
        data.getContext().setAttribute(attribute);
    }
}
