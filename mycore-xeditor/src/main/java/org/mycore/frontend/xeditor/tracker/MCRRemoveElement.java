package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Element;

public class MCRRemoveElement implements MCRChange {

    public static MCRChangeData remove(Element element) {
        Element parent = element.getParentElement();
        MCRChangeData data = new MCRChangeData("removed-element", element, parent.indexOf(element), parent);
        element.detach();
        return data;
    }

    public void undo(MCRChangeData data) {
        data.getContext().addContent(data.getPosition(), data.getElement());
    }
}
