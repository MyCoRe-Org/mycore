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
 * Represents a role of users.
 * Roles are {@link MCRCategory} instances and every category from {@link MCRUser2Constants#ROLE_CLASSID} {@link MCRRole#isSystemRole()}.
 * 
 * @author Thomas Scheffler (yagee)
 */
@XmlRootElement(name = "role")
public class MCRRole {

    /** The unique role name */
    private String name;

    /** The labels of the role */
    private HashMap<String, MCRLabel> labels;

    private boolean isSystemRole;

    private MCRRole() {
        this.labels = new HashMap<String, MCRLabel>();
    }

    /**
     * Creates a new role instance. 
     * 
     * @param name the unique role ID
     * @param labels a set of MCRLabel in different languages
     */
    public MCRRole(String name, Set<MCRLabel> labels) {
        this();
        for (MCRLabel label : labels) {
            this.labels.put(label.getLang(), label);
        }
        setName(name);
    }

    /**
     * Returns the roles's name
     * 
     * @return the roles's name
     */
    @XmlAttribute
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
     * Returns all labels available for this role. 
     */
    public Collection<MCRLabel> getLabels() {
        return labels.values();
    }

    /**
     * Returns true if this role is a system role.
     * A system role is every category in {@link MCRUser2Constants#ROLE_CLASSID}. 
     * @return false if category has not the same root ID as the system role classification.
     */
    public boolean isSystemRole() {
        return isSystemRole;
    }

    @XmlElement(name = "label")
    private MCRLabel[] getLabelsArray() {
        return labels.values().toArray(new MCRLabel[labels.size()]);
    }

    @SuppressWarnings("unused")
    private void setLabelsArray(MCRLabel[] labels) {
        for (MCRLabel label : labels) {
            this.labels.put(label.getLang(), label);
        }
    }

    private void setName(String name) {
        this.name = name;
        this.isSystemRole = !name.contains(":") || name.startsWith(MCRUser2Constants.ROLE_CLASSID.getRootID() + ":");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRRole)
            return ((MCRRole) obj).getName().equals(this.getName());
        else
            return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }
}
