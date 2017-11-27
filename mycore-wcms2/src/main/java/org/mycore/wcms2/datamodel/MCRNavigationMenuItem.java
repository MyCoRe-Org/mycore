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

@XmlRootElement(name = "menu")
@XmlAccessorType(XmlAccessType.NONE)
public class MCRNavigationMenuItem extends MCRNavigationI18nItem
    implements MCRNavigationItemContainer {

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
        this.children = new ArrayList<>();
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
