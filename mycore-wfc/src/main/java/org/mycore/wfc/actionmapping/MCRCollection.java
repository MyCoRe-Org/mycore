/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.wfc.actionmapping;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "collection")
public class MCRCollection {

    @XmlElement(name = "action")
    MCRAction[] actions;

    @XmlAttribute
    String name;

    public MCRAction[] getActions() {
        if (actions == null) {
            return new MCRAction[0];
        }
        return actions.clone();
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")//only POJO
    public void setActions(MCRAction... actions) {
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
