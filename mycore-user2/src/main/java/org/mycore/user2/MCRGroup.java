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

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Represents a group of users. Groups are configured in the file groups.xml.
 * A special group is the group of administrators, which is specified by the attribute adminGroups. 
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCRGroup {

    /** The unique group name */
    private String name;

    /** The labels of the group */
    private HashMap<String, MCRLabel> labels;

    private boolean isSystemGroup;

    /**
     * Creates a new group. 
     * 
     * @param id the unique group ID
     * @param name the unique group name
     */
    MCRGroup(String name, Set<MCRLabel> labels) {
        this.name = name;
        this.labels = new HashMap<String, MCRLabel>();
        for (MCRLabel label : labels) {
            this.labels.put(label.getLang(), label);
        }
        this.isSystemGroup=!name.contains(":")||name.startsWith(MCRGroupManager.GROUP_CLASSID.getRootID()+":");
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
     * Returns the label in the current language
     */
    public MCRLabel getLabel() {
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        return labels.get(lang);
    }

    /**
     * Returns a list of all users currently member of this group
     * 
     * @return a list of all users currently member of this group
     */
    public List<MCRUser> listUsers() {
        //return MCRGroupManager.getUsersInGroup(this);
        return null;
    }
    
    public boolean isSystemGroup(){
        return isSystemGroup;
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
