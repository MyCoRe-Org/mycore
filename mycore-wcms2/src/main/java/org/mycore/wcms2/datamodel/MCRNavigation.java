package org.mycore.wcms2.datamodel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the root entry of a navigation.xml.
 *
 * @author Matthias Eichner
 */
@XmlRootElement(name = "navigation")
@XmlAccessorType(XmlAccessType.NONE)
public class MCRNavigation implements MCRNavigationBaseItem, MCRNavigationItemContainer {

    // general
    @XmlAttribute
    private String hrefStartingPage;

    @XmlAttribute
    private String dir;

    @XmlAttribute
    private String mainTitle;

    @XmlAttribute
    protected String template;

    // multitenancy
    @XmlAttribute
    private String historyTitle;

    // children
    @XmlElementRefs({ @XmlElementRef(type = MCRNavigationMenuItem.class),
        @XmlElementRef(type = MCRNavigationInsertItem.class) })
    private List<MCRNavigationBaseItem> children;

    public MCRNavigation() {
        this.children = new ArrayList<MCRNavigationBaseItem>();
    }

    public String getHrefStartingPage() {
        return hrefStartingPage;
    }

    public String getDir() {
        return dir;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public String getHistoryTitle() {
        return historyTitle;
    }

    public String getTemplate() {
        return template;
    }

    public void setHrefStartingPage(String hrefStartingPage) {
        this.hrefStartingPage = hrefStartingPage;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public void setHistoryTitle(String historyTitle) {
        this.historyTitle = historyTitle;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void addMenu(MCRNavigationMenuItem menu) {
        this.children.add(menu);
    }

    public void addInsertItem(MCRNavigationInsertItem insertItem) {
        this.children.add(insertItem);
    }

    public List<MCRNavigationBaseItem> getChildren() {
        return this.children;
    }

}
