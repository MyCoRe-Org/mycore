package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Element;

public class MCRAddedElement implements MCRChange {

    public static MCRChangeData added(Element element) {
        return new MCRChangeData("added-this-element", "", 0, element);
    }

    public void undo(MCRChangeData data) {
        data.getContext().detach();
    }
}
