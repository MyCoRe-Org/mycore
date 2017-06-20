package org.mycore.frontend.xeditor.tracker;

import java.util.Iterator;

import org.jdom2.Attribute;
import org.jdom2.Element;

public class MCRSetElementText implements MCRChange {

    public static MCRChangeData setText(Element element, String text) {
        Element clone = element.clone();

        for (Iterator<Attribute> attributes = clone.getAttributes().iterator(); attributes.hasNext();) {
            attributes.next();
            attributes.remove();
        }

        MCRChangeData data = new MCRChangeData("set-text", clone, 0, element);
        element.setText(text);
        return data;
    }

    public void undo(MCRChangeData data) {
        data.getContext().setContent(data.getElement().cloneContent());
    }
}
