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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;

/**
 * Manages groups and group membership using a database table.
 * Groups are configured in the file groups.xml. 
 * The property MIL.GroupMembers.Table specifies the table that stores
 * group membership information (users in group). 
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCRGroupManager {

    /** Map of defined groups, key is the unique group name */
    private static HashMap<String, MCRGroup> groupsByName = new HashMap<String, MCRGroup>();

    /** List of all defined groups */
    private static List<MCRGroup> groupsList = new ArrayList<MCRGroup>();

    static final MCRCategoryID GROUP_CLASSID = MCRCategoryID.rootID("mcr-groups");

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final MCRCategLinkService CATEG_LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    static {

        int onlyNonHierarchicalGroups = 1;
        MCRCategory groupCategory = DAO.getCategory(GROUP_CLASSID, onlyNonHierarchicalGroups);
        for (MCRCategory child : groupCategory.getChildren()) {
            String name = child.getId().getID();
            MCRGroup group = new MCRGroup(name, child.getLabels());
            groupsByName.put(name, group);
            groupsList.add(group);
        }
    }

    /**
     * Returns the group with the given group name, or null.
     * 
     * @param name the unique group name
     * @return the group with the given group name, or null.
     */
    public static MCRGroup getGroup(String name) {
        return groupsByName.get(name);
    }

    /**
     * Returns a group array for the given group names. 
     * @param names unique group names
     * @return array each element either MCRGroup instance, or null
     */
    public static MCRGroup[] getGroups(String... names) {
        MCRGroup[] groups = new MCRGroup[names.length];
        for (int i = 0; i < names.length; i++) {
            groups[i] = getGroup(names[i]);
        }
        return groups;
    }

    /**
     * Returns a list of all defined groups
     * 
     * @return a list of all defined groups
     */
    public static List<MCRGroup> listSystemGroups() {
        return groupsList;
    }

    /**
     * Removes user from all groups
     * 
     * @param user the user to remove from all groups
     */
    static void removeUserFromGroups(MCRUser user) {
        CATEG_LINK_SERVICE.deleteLink(getLinkID(user));
    }

    /**
     * Stores group membership information of the user
     * 
     * @param user the user 
     */
    static void storeGroupsOfUser(MCRUser user) {
        MCRCategLinkReference ref = getLinkID(user);
        LinkedList<MCRCategoryID> categories = new LinkedList<MCRCategoryID>();
        for (String groupID : user.getSystemGroupIDs()) {
            MCRCategoryID categID = new MCRCategoryID(GROUP_CLASSID.getRootID(), groupID);
            categories.add(categID);
        }
        for (String groupID : user.getExternalGroupIDs()) {
            MCRCategoryID categID = MCRCategoryID.fromString(groupID);
            categories.add(categID);
        }
        CATEG_LINK_SERVICE.setLinks(ref, categories);
    }

    static Collection<MCRCategoryID> getGroupIDs(MCRUser user) {
        return CATEG_LINK_SERVICE.getLinksFromReference(getLinkID(user));
    }

    private static MCRCategLinkReference getLinkID(MCRUser user) {
        return new MCRCategLinkReference(user.getUserName() + "@" + user.getRealmID(), "mcr-user");
    }
}
