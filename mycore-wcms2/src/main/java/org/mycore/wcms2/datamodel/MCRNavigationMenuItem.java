package org.mycore.wcms2.datamodel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "menu")
@XmlAccessorType(XmlAccessType.NONE)
public class MCRNavigationMenuItem extends MCRNavigationI18nItem
    implements MCRNavigationBaseItem, MCRNavigationItemContainer {

    @XmlAttribute(required = true)
    private String id;

    @XmlAttribute
    private String dir;

    // children
    @XmlElementRefs({
        @XmlElementRef(type = MCRNavigationItem.class),
        @XmlElementRef(type = MCRNavigationGroup.class),
        @XmlElementRef(type = MCRNavigationInsertItem.class)
    })
    //    @XmlAnyElement(lax = true)
    private List<MCRNavigationBaseItem> children;

    public MCRNavigationMenuItem() {
        this.children = new ArrayList<MCRNavigationBaseItem>();
    }

    public String getId() {
        return id;
    }

    public String getDir() {
        return dir;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void addItem(MCRNavigationItem item) {
        this.children.add(item);
    }

    public void addInsertItem(MCRNavigationInsertItem insertItem) {
        this.children.add(insertItem);
    }

    public List<MCRNavigationBaseItem> getChildren() {
        return this.children;
    }

}
