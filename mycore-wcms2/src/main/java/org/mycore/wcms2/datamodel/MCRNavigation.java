/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
public class MCRNavigation implements MCRNavigationItemContainer {

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
        this.children = new ArrayList<>();
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
