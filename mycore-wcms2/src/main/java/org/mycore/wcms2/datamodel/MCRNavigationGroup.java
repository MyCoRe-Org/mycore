package org.mycore.wcms2.datamodel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "group")
@XmlAccessorType(XmlAccessType.NONE)
public class MCRNavigationGroup extends MCRNavigationI18nItem implements MCRNavigationBaseItem,
    MCRNavigationItemContainer {

    // general
    @XmlAttribute(required = true)
    @XmlID
    private String id;

    // children
    @XmlElementRefs({ @XmlElementRef(type = MCRNavigationItem.class),
        @XmlElementRef(type = MCRNavigationInsertItem.class) })
    private List<MCRNavigationBaseItem> children;

    public MCRNavigationGroup() {
        super();
        this.children = new ArrayList<MCRNavigationBaseItem>();
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
