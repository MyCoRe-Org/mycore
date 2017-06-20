package org.mycore.wcms2.datamodel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.NONE)
public class MCRNavigationItem extends MCRNavigationI18nItem implements MCRNavigationBaseItem,
    MCRNavigationItemContainer {

    @XmlType(name = "ItemType")
    public enum Type {
        intern, extern
    }

    public enum Target {
        _self, _blank
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

    @XmlAttribute
    protected String template;

    @XmlAttribute
    protected String style;

    // children
    @XmlElementRefs({ @XmlElementRef(type = MCRNavigationItem.class),
        @XmlElementRef(type = MCRNavigationInsertItem.class) })
    private List<MCRNavigationBaseItem> children;

    public MCRNavigationItem() {
        super();
        this.children = new ArrayList<MCRNavigationBaseItem>();
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

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getStyle() {
        return style;
    }

    public void addItem(MCRNavigationItem item) {
        this.children.add(item);
    }

    public void addInsertItem(MCRNavigationInsertItem insertItem) {
        this.children.add(insertItem);
    }

    @Override
    public List<MCRNavigationBaseItem> getChildren() {
        return this.children;
    }
}
