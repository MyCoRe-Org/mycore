package org.mycore.frontend.editor;

import java.util.Random;

import org.jdom.Element;

public class MCREditor {
    private Element xml;

    private String id;

    public MCREditor(Element xml) {
        this.xml = xml;
        this.id = buildID();
        xml.setAttribute("session", id);
    }

    public Element getXML() {
        return xml;
    }

    public String getSessionID() {
        return id;
    }

    private static Random random = new Random();

    private static synchronized String buildID() {
        StringBuffer sb = new StringBuffer();
        sb.append(Long.toString(System.nanoTime(), 36));
        sb.append(Long.toString(random.nextLong(), 36));
        sb.reverse();

        return sb.toString();
    }
}
