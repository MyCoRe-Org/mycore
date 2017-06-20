package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Attribute;

public class MCRRemoveAttribute implements MCRChange {

    public static MCRChangeData remove(Attribute attribute) {
        MCRChangeData data = new MCRChangeData("removed-attribute", attribute);
        attribute.detach();
        return data;
    }

    public void undo(MCRChangeData data) {
        data.getContext().setAttribute(data.getAttribute());
    }
}
