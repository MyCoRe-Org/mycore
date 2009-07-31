package org.mycore.importer.mapping;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

/**
 * This class is an abstraction of an surrounding metadata element. For example something
 * like &lt;names class="MCRMetaPersonName" heritable="false" notinherit="false"&gt;.
 * It saves the tag, the className, heritable, notinherit and a list of jdom child elements.
 * 
 * @author Matthias Eichner
 */
public class MCRImportMetadata {

    protected String tag;
    protected String className;

    protected Boolean heritable;
    protected Boolean notinherit;

    protected List<Element> childList;

    /**
     * Creates a new metadata element.
     * 
     * @param tag the tag of the element
     */
    public MCRImportMetadata(String tag) {
        this.tag = tag;
        this.childList = new ArrayList<Element>();
    }

    /**
     * Creates a jdom element from this instance.
     * 
     * @return a jdom element
     */
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