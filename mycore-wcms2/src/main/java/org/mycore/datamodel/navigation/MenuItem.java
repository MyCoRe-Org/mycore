package org.mycore.datamodel.navigation;

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
public class MenuItem extends I18nItem implements NavigationItem, ItemContainer {

    @XmlAttribute(required = true)
    private String id;
    @XmlAttribute
    private String dir;
    // children
    @XmlElementRefs ({
        @XmlElementRef(type = Item.class),
        @XmlElementRef(type = InsertItem.class)
    })
//    @XmlAnyElement(lax = true)
    private List<NavigationItem> children;

    public MenuItem() {
        this.children = new ArrayList<NavigationItem>();
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

    public void addItem(Item item) {
        this.children.add(item);
    }
    public void addInsertItem(InsertItem insertItem) {
        this.children.add(insertItem);
    }
    public List<NavigationItem> getChildren() {
        return this.children;
    }

}
