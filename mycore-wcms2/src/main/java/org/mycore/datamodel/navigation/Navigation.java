package org.mycore.datamodel.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the root entry of a navigation.xml.
 *
 * @author Matthias Eichner
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Navigation implements NavigationItem, ItemContainer {

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
    @XmlAttribute
    private String parentTenant;
    @XmlAttribute
    private String parentPage;
    @XmlElement
    private List<String> include;
    // children
    @XmlElementRefs ({
        @XmlElementRef(type = MenuItem.class),
        @XmlElementRef(type = InsertItem.class)
    })
    private List<NavigationItem> children;

    public Navigation() {
        this.include = new ArrayList<String>();
        this.children = new ArrayList<NavigationItem>();
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
    public String getParentTenant() {
        return parentTenant;
    }
    public String getParentPage() {
        return parentPage;
    }
    public List<String> getInclude() {
        return include;
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
    public void setParentTenant(String parentTenant) {
        this.parentTenant = parentTenant;
    }
    public void setParentPage(String parentPage) {
        this.parentPage = parentPage;
    }
    public void setTemplate(String template) {
        this.template = template;
    }

    public void addMenu(MenuItem menu) {
        this.children.add(menu);
    }
    public void addInsertItem(InsertItem insertItem) {
        this.children.add(insertItem);
    }
    public List<NavigationItem> getChildren() {
        return this.children;
    }

}
