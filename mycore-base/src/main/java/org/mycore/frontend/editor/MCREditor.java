package org.mycore.frontend.editor;

import org.jdom.Element;

public class MCREditor {
    private Element xml;

    public MCREditor(Element xml) {
        this.xml = xml;
    }

    public Element getXML() {
        return xml;
    }
}
