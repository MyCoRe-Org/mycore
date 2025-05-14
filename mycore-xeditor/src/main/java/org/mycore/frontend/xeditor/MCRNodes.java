package org.mycore.frontend.xeditor;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.frontend.xeditor.tracker.MCRAddedAttribute;
import org.mycore.frontend.xeditor.tracker.MCRAddedElement;
import org.mycore.frontend.xeditor.tracker.MCRChange;
import org.mycore.frontend.xeditor.tracker.MCRRemoveAttribute;
import org.mycore.frontend.xeditor.tracker.MCRRemoveElement;
import org.mycore.frontend.xeditor.tracker.MCRSetAttributeValue;
import org.mycore.frontend.xeditor.tracker.MCRSetElementText;

public class MCRNodes {

    protected List<Object> boundNodes = new ArrayList<>();

    protected List<MCRChange> changes = new ArrayList<MCRChange>();

    public MCRNodes() {
    }

    public MCRNodes(Object node) {
        boundNodes.add(node);
    }

    public MCRNodes(List<Object> nodes) {
        boundNodes.addAll(nodes);
    }

    public List<Object> getBoundNodes() {
        return boundNodes;
    }

    public Object getBoundNode() {
        return boundNodes.get(0);
    }

    public String getValue() {
        return getValue(getBoundNode());
    }

    public static String getValue(Object node) {
        if (node instanceof Element element) {
            return element.getTextTrim();
        } else {
            return ((Attribute) node).getValue();
        }
    }

    public boolean hasValue(String value) {
        return boundNodes.stream().map(MCRNodes::getValue).anyMatch(value::equals);
    }

    public void setValue(String value) {
        setValue(getBoundNode(), value);
    }

    public void setValues(String value) {
        boundNodes.forEach(node -> setValue(node, value));
    }

    public void setValues(String[] values) {
        while (boundNodes.size() < values.length) {
            cloneBoundElement(boundNodes.size() - 1);
        }

        for (int i = 0; i < values.length; i++) {
            String value = values[i] == null ? "" : values[i].trim();
            value = MCRXMLFunctions.normalizeUnicode(value);
            value = MCRXMLHelper.removeIllegalChars(value);
            setValue(i, value);
        }
    }

    public void setValue(int index, String value) {
        setValue(boundNodes.get(index), value);
    }

    private void setValue(Object node, String value) {
        if (value.equals(getValue(node))) {
            return;
        }
        if (node instanceof Attribute attribute) {
            changes.add(new MCRSetAttributeValue(attribute, value));
        } else {
            changes.add(new MCRSetElementText((Element) node, value));
        }
    }

    public Element cloneBoundElement(int index) {
        Element template = (Element) (boundNodes.get(index));
        Element newElement = template.clone();
        Element parent = template.getParentElement();
        int indexInParent = parent.indexOf(template) + 1;

        parent.addContent(indexInParent, newElement);
        boundNodes.add(index + 1, newElement);
        trackNodeCreated(newElement);

        return newElement;
    }

    protected void trackNodeCreated(Object node) {
        if (node instanceof Element element) {
            changes.add(new MCRAddedElement(element));
        } else {
            Attribute attribute = (Attribute) node;
            changes.add(new MCRAddedAttribute(attribute));
        }
    }

    public void removeBoundNode(int index) {
        Object node = boundNodes.remove(index);
        if (node instanceof Element element) {
            changes.add(new MCRRemoveElement(element));
        } else {
            changes.add(new MCRRemoveAttribute((Attribute) node));
        }
    }

    public List<MCRChange> getChanges() {
        try {
            return new ArrayList<MCRChange>(changes);
        } finally {
            changes.clear();
        }
    }
}
