package org.mycore.datamodel.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Item extends I18nItem implements NavigationItem, ItemContainer {

    @XmlType(name = "ItemType")
    public enum Type {
        intern, extern
    }
    public enum Target {
        _self, _blank
    }
    public enum Style {
        normal, bold
    }

    // general
    @XmlAttribute(required = true)
    private String href;
    @XmlAttribute
    private Type type;
    // navigation
    @XmlAttribute
    private Target target;
    @XmlAttribute
    private boolean replaceMenu;
    @XmlAttribute
    private boolean constrainPopUp;
    // layout
    @XmlAttribute
    private Style style;
    @XmlAttribute
    protected String template;
    // children
    @XmlElementRefs ({
        @XmlElementRef(type = Item.class),
        @XmlElementRef(type = InsertItem.class)
    })
    private List<NavigationItem> children;

    public Item() {
        super();
        this.children = new ArrayList<NavigationItem>();
    }

    public String getHref() {
        return href;
    }
    public boolean isReplaceMenu() {
        return replaceMenu;
    }
    public boolean isConstrainPopUp() {
        return constrainPopUp;
    }
    public Type getType() {
        return type;
    }
    public Target getTarget() {
        return target;
    }
    public Style getStyle() {
        return style;
    }
    public String getTemplate() {
        return template;
    }

    public void setHref(String href) {
        this.href = href;
    }
    public void setType(Type type) {
        this.type = type;
    }
    public void setTarget(Target target) {
        this.target = target;
    }
    public void setReplaceMenu(boolean replaceMenu) {
        this.replaceMenu = replaceMenu;
    }
    public void setConstrainPopUp(boolean constrainPopUp) {
        this.constrainPopUp = constrainPopUp;
    }
    public void setStyle(Style style) {
        this.style = style;
    }
    public void setTemplate(String template) {
        this.template = template;
    }

    public void addItem(Item item) {
        this.children.add(item);
    }
    public void addInsertItem(InsertItem insertItem) {
        this.children.add(insertItem);
    }
    @Override
    public List<NavigationItem> getChildren() {
        return this.children;
    }
}
