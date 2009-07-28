package org.mycore.importer.mapping;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

public class MCRImportMetadata {

    protected String tag;
    protected String className;

    protected Boolean heritable;
    protected Boolean notinherit;

    protected List<Element> childList;

    public MCRImportMetadata(String tag) {
        this.tag = tag;
        this.childList = new ArrayList<Element>();
    }

    public Element createXML() {
        Element metadataElement = new Element(tag);
        metadataElement.setAttribute("class", className);
        if(heritable != null)
            metadataElement.setAttribute("heritable", String.valueOf(heritable));
        if(notinherit != null)
            metadataElement.setAttribute("notinherit", String.valueOf(notinherit));

        for(Element childElement : childList) {
            metadataElement.addContent(childElement);
        }
        return metadataElement;
    }

    public void addChild(Element child) {
        childList.add(child);
    }
    public List<Element> getChilds() {
        return childList;
    }

    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public boolean isHeritable() {
        return heritable;
    }
    public void setHeritable(boolean heritable) {
        this.heritable = heritable;
    }
    public boolean isNotinherit() {
        return notinherit;
    }
    public void setNotinherit(boolean notinherit) {
        this.notinherit = notinherit;
    }
  
}