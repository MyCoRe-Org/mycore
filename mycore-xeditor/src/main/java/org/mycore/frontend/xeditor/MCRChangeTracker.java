package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Parent;
import org.jdom2.ProcessingInstruction;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class MCRChangeTracker {

    public static final MCRAddAttribute ADD_ATTRIBUTE = new MCRAddAttribute();

    public static final MCRAddElement ADD_ELEMENT = new MCRAddElement();

    public static final MCRRemoveAttribute REMOVE_ATTRIBUTE = new MCRRemoveAttribute();

    public static final MCRRemoveElement REMOVE_ELEMENT = new MCRRemoveElement();

    public static final MCRSetAttributeValue SET_ATTRIBUTE_VALUE = new MCRSetAttributeValue();

    public static final MCRSetElementText SET_TEXT = new MCRSetElementText();

    public static final String PREFIX = "xed-";

    private int counter = 0;

    public void track(ProcessingInstruction pi) {
        pi.setTarget(PREFIX + ++counter + "-" + pi.getTarget());
    }

    public void undoChanges(Document doc) {
        undoChanges(doc, 0);
    }

    public void undoChanges(Document doc, int stepNumber) {
        while (counter > stepNumber) {
            ProcessingInstruction pi = findNextProcessingInstruction(doc);
            MCRChangeType.getType(pi.getTarget()).undo(pi);
            pi.detach();
        }
    }

    private ProcessingInstruction findNextProcessingInstruction(Document doc) {
        String typePrefix = PREFIX + (counter--) + "-";
        for (ProcessingInstruction instruction : doc.getDescendants(Filters.processinginstruction())) {
            String target = instruction.getTarget();

            if (target.startsWith(typePrefix)) {
                instruction.setTarget(target.substring(typePrefix.length()));
                return instruction;
            }
        }
        throw new RuntimeException("Lost processing instruction for undo, not found: " + typePrefix);
    }

    public static void removeChangeTracking(Document doc) {
        for (Iterator<ProcessingInstruction> iter = doc.getDescendants(Filters.processinginstruction()).iterator(); iter.hasNext();) {
            if (iter.next().getTarget().startsWith(PREFIX))
                iter.remove();
        }
    }
}

abstract class MCRChangeType {

    private static Map<String, MCRChangeType> changeTypes = new HashMap<String, MCRChangeType>();

    private static final XMLOutputter RAW_OUTPUTTER = new XMLOutputter(Format.getRawFormat());

    public static MCRChangeType getType(String id) {
        return changeTypes.get(id);
    }

    protected MCRChangeType() {
        changeTypes.put(getID(), this);
    }

    public abstract String getID();

    public abstract void undo(ProcessingInstruction pi);

    protected String element2text(Element element) {
        return RAW_OUTPUTTER.outputString(element);
    }

    protected Element text2element(String text) {
        try {
            return new SAXBuilder().build(new StringReader(text)).detachRootElement();
        } catch (JDOMException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String attribute2text(Attribute attribute) {
        Element x = new Element("x").setAttribute(attribute.clone());
        String text = element2text(x);
        return text.substring(3, text.length() - 2).trim();
    }

    protected Attribute text2attribute(String text) {
        text = "<x " + text + " />";
        return text2element(text).getAttributes().get(0).detach();
    }
}

class MCRAddElement extends MCRChangeType {

    public String getID() {
        return "added-this-element";
    }

    public ProcessingInstruction added(Element element) {
        ProcessingInstruction pi = new ProcessingInstruction(getID());
        element.addContent(0, pi);
        return pi;
    }

    public void undo(ProcessingInstruction pi) {
        pi.getParentElement().detach();
    }
}

class MCRRemoveElement extends MCRChangeType {

    public String getID() {
        return "removed-element";
    }

    public ProcessingInstruction remove(Element element) {
        String elementAsText = element2text(element);
        ProcessingInstruction pi = new ProcessingInstruction(getID(), elementAsText);
        Parent parent = element.getParent();
        int index = parent.indexOf(element);
        parent.addContent(index, pi);
        element.detach();
        return pi;
    };

    public void undo(ProcessingInstruction pi) {
        Parent parent = pi.getParent();
        int index = parent.indexOf(pi);
        Element child = text2element(pi.getData());
        parent.addContent(index, child);
    }
}

class MCRRemoveAttribute extends MCRChangeType {

    public String getID() {
        return "removed-attribute";
    }

    public ProcessingInstruction remove(Attribute attribute) {
        String attributeAsText = attribute2text(attribute);
        ProcessingInstruction pi = new ProcessingInstruction(getID(), attributeAsText);
        attribute.getParent().addContent(0, pi);
        attribute.detach();
        return pi;
    }

    public void undo(ProcessingInstruction pi) {
        Element parent = pi.getParentElement();
        Attribute attribute = text2attribute(pi.getData());
        parent.setAttribute(attribute);
    }
}

class MCRAddAttribute extends MCRChangeType {

    public String getID() {
        return "added-attribute";
    }

    public ProcessingInstruction added(Attribute attribute) {
        String attributeAsText = attribute2text(attribute);
        ProcessingInstruction pi = new ProcessingInstruction(getID(), attributeAsText);
        attribute.getParent().addContent(0, pi);
        return pi;
    }

    public void undo(ProcessingInstruction pi) {
        Attribute attribute = text2attribute(pi.getData());
        Element parent = pi.getParentElement();
        parent.removeAttribute(attribute.getName(), attribute.getNamespace());
    }
}

class MCRSetAttributeValue extends MCRChangeType {

    public String getID() {
        return "set-attribute-value";
    }

    public ProcessingInstruction set(Attribute attribute, String value) {
        String attributeAsText = attribute2text(attribute);
        attribute.setValue(value);
        ProcessingInstruction pi = new ProcessingInstruction(getID(), attributeAsText);
        attribute.getParent().addContent(0, pi);
        return pi;
    }

    public void undo(ProcessingInstruction pi) {
        Attribute attribute = text2attribute(pi.getData());
        Element parent = pi.getParentElement();
        parent.removeAttribute(attribute.getName(), attribute.getNamespace());
        parent.setAttribute(attribute);
    }
}

class MCRSetElementText extends MCRChangeType {

    public String getID() {
        return "set-text";
    }

    public ProcessingInstruction set(Element element, String text) {
        Element clone = element.clone();

        for (Iterator<Attribute> attributes = clone.getAttributes().iterator(); attributes.hasNext();) {
            attributes.next();
            attributes.remove();
        }

        String elementAsText = element2text(clone);
        ProcessingInstruction pi = new ProcessingInstruction(getID(), elementAsText);
        element.setText(text);
        element.addContent(0, pi);
        return pi;
    }

    public void undo(ProcessingInstruction pi) {
        Element element = pi.getParentElement();
        element.setContent(text2element(pi.getData()).cloneContent());
    }
}
