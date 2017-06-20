package org.mycore.frontend.xeditor.tracker;

import java.io.IOException;
import java.io.StringReader;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.ProcessingInstruction;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;

public class MCRChangeData {

    protected String type;

    protected int pos;

    protected Element context;

    protected String text;

    protected ProcessingInstruction pi;

    public MCRChangeData(String type, String text, int pos, Element context) {
        this.type = type;
        this.text = text;
        this.pos = pos;
        this.context = context;
    }

    public MCRChangeData(String type, Attribute attribute) {
        this(type, attribute2text(attribute), 0, attribute.getParent());
    }

    public MCRChangeData(String type, Element data, int pos, Element context) {
        this(type, element2text(data), pos, context);
    }

    public ProcessingInstruction getProcessingInstruction() {
        if (pi == null) {
            String data = RAW_OUTPUTTER.outputString(new Text(text));
            this.pi = new ProcessingInstruction(type, data);
        }
        return pi;
    }

    public MCRChangeData(ProcessingInstruction pi, String prefix) {
        this.pi = pi;
        this.context = pi.getParentElement();
        this.pos = context.indexOf(pi);
        this.type = pi.getTarget().substring(prefix.length());

        String xml = "<x>" + pi.getData() + "</x>";
        this.text = text2element(xml).getText();
    }

    public String getType() {
        return type;
    }

    public Element getContext() {
        return context;
    }

    public int getPosition() {
        return pos;
    }

    public String getText() {
        return text;
    }

    public Element getElement() {
        return text2element(text);
    }

    public Attribute getAttribute() {
        return text2attribute(text);
    }

    private static final XMLOutputter RAW_OUTPUTTER = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8"));

    private static String element2text(Element element) {
        return RAW_OUTPUTTER.outputString(element);
    }

    private Element text2element(String text) {
        try {
            return new SAXBuilder().build(new StringReader(text)).detachRootElement();
        } catch (JDOMException | IOException ex) {
            throw new MCRException("Exception in text2element: " + text, ex);
        }
    }

    private static String attribute2text(Attribute attribute) {
        Element x = new Element("x").setAttribute(attribute.clone());
        String text = element2text(x);
        return text.substring(3, text.length() - 2).trim();
    }

    public Attribute text2attribute(String text) {
        text = "<x " + text + " />";
        return text2element(text).getAttributes().get(0).detach();
    }
}
