/**
 * $Revision$ 
 * $Date$
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Represents a group of users.
 * Groups are {@link MCRCategory} instances and every category from {@link MCRUser2Constants#GROUP_CLASSID} {@link MCRGroup#isSystemGroup()}.
 * 
 * @author Thomas Scheffler (yagee)
 */
@XmlRootElement(name = "group")
public class MCRGroup {

    /** The unique group name */
    @XmlAttribute
    private String name;

    /** The labels of the group */
    private HashMap<String, MCRLabel> labels;

    private boolean isSystemGroup;

    private MCRGroup() {
        this.labels = new HashMap<String, MCRLabel>();
    }

    /**
     * Creates a new group instance. 
     * 
     * @param id the unique group ID
     * @param name the unique group name
     */
    MCRGroup(String name, Set<MCRLabel> labels) {
        this();
        for (MCRLabel label : labels) {
            this.labels.put(label.getLang(), label);
        }
        setName(name);
    }

    /**
     * Returns the group's name
     * 
     * @return the group's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the label in the current language.
     */
    public MCRLabel getLabel() {
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        return labels.get(lang);
    }

    /**
     * Returns all labels available for this group. 
     */
    public Collection<MCRLabel> getLabels() {
        return labels.values();
    }

    /**
     * Returns true if this group is a system group.
     * A system group is every category in {@link MCRUser2Constants#GROUP_CLASSID}. 
     * @return false if category has not the same root ID as the system group classification.
     */
    public boolean isSystemGroup() {
        return isSystemGroup;
    }

    @XmlElement(name = "label")
    private MCRLabel[] getLabelsArray() {
        return labels.values().toArray(new MCRLabel[labels.size()]);
    }

    private void setLabelsArray(MCRLabel[] labels) {
        for (MCRLabel label : labels) {
            this.labels.put(label.getLang(), label);
        }
    }

    private void setName(String name) {
        this.name = name;
        this.isSystemGroup = !name.contains(":") || name.startsWith(MCRUser2Constants.GROUP_CLASSID.getRootID() + ":");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRGroup)
            return ((MCRGroup) obj).getName().equals(this.getName());
        else
            return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
