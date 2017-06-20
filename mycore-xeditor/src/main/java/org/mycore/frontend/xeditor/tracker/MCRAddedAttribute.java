package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Attribute;

public class MCRAddedAttribute implements MCRChange {

    public static MCRChangeData added(Attribute attribute) {
        return new MCRChangeData("added-attribute", attribute);
    }

    public void undo(MCRChangeData data) {
        Attribute attribute = data.getAttribute();
        data.getContext().removeAttribute(attribute.getName(), attribute.getNamespace());
    }
}
