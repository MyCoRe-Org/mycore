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

package org.mycore.user2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

/**
 * Manages roles and role assignments using a database table.
 * 
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRRoleManager {

    private static Logger LOGGER = LogManager.getLogger(MCRRoleManager.class);

    /** Map of defined roles, key is the unique role name */
    private static HashMap<String, MCRRole> rolesByName = new HashMap<>();

    /** List of all defined roles */
    private static List<MCRRole> rolesList = new ArrayList<>();

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static long lastLoaded;

    private static final MCRCategLinkService CATEG_LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    static {
        loadSystemRoles();
    }

    private static void loadSystemRoles() {
        if (lastLoaded == DAO.getLastModified() || !DAO.exist(MCRUser2Constants.ROLE_CLASSID)) {
            return;
        }
        lastLoaded = DAO.getLastModified();
        int onlyNonHierarchicalRoles = 1;
        MCRCategory roleCategory = DAO.getCategory(MCRUser2Constants.ROLE_CLASSID, onlyNonHierarchicalRoles);
        rolesByName.clear();
        rolesList.clear();
        if (roleCategory != null) {
            for (MCRCategory child : roleCategory.getChildren()) {
                String name = child.getId().getID();
                MCRRole role = new MCRRole(name, child.getLabels());
                rolesByName.put(name, role);
                rolesList.add(role);
            }
        }
    }

    /**
     * Returns the role with the given role name, or null.
     * 
     * @param name the unique role name
     * @return the role with the given role name, or null.
     */
    public static MCRRole getRole(String name) {
        loadSystemRoles();
        MCRRole mcrRole = rolesByName.get(name);
        if (mcrRole == null) {
            return getExternalRole(name);
        }
        return mcrRole;
    }

    /**
     * Factory for external roles
     * @param name a valid {@link MCRCategoryID}
     * @return MCRRole instance or null if category does not exist
     */
    public static MCRRole getExternalRole(String name) {
        MCRCategoryID categoryID = MCRCategoryID.fromString(name);
        if (categoryID.isRootID()) {
            LOGGER.debug("External role may not be a rootCategory: {}", categoryID);
            return null;
        }
        MCRCategory category = DAO.getCategory(categoryID, 0);
        if (category == null) {
            LOGGER.debug("Category does not exist: {}", categoryID);
            return null;
        }
        return new MCRRole(name, category.getLabels());
    }

    /**
     * Returns a role array for the given role names. 
     * @param names unique role names
     * @return array each element either MCRRole instance, or null
     */
    public static MCRRole[] getRoles(String... names) {
        loadSystemRoles();
        MCRRole[] roles = new MCRRole[names.length];
        for (int i = 0; i < names.length; i++) {
            roles[i] = rolesByName.get(names[i]);
            if (roles[i] == null) {
                roles[i] = getExternalRole(names[i]);
            }
        }
        return roles;
    }

    /**
     * Returns a role collection for the given role names.
     * If a role is not known the returning collection contains fewer items.
     * @param names unique role names
     * @return collection each element is MCRRole instance.
     */
    public static Collection<MCRRole> getRoles(Collection<String> names) {
        loadSystemRoles();
        String[] namesArr = names.toArray(new String[names.size()]);
        MCRRole[] roles = getRoles(namesArr);
        return Arrays.asList(roles);
    }

    /**
     * Returns a list of all defined roles
     * 
     * @return a list of all defined roles
     */
    public static List<MCRRole> listSystemRoles() {
        return rolesList;
    }

    /**
     * Removes user from all roles
     * 
     * @param user the user to remove from all roles
     */
    static void unassignRoles(MCRUser user) {
        CATEG_LINK_SERVICE.deleteLink(getLinkID(user));
    }

    /**
     * Stores role membership information of the user
     * 
     * @param user the user 
     */
    static void storeRoleAssignments(MCRUser user) {
        MCRCategLinkReference ref = getLinkID(user);
        LinkedList<MCRCategoryID> categories = new LinkedList<>();
        for (String roleID : user.getSystemRoleIDs()) {
            MCRCategoryID categID = new MCRCategoryID(MCRUser2Constants.ROLE_CLASSID.getRootID(), roleID);
            categories.add(categID);
        }
        for (String roleID : user.getExternalRoleIDs()) {
            MCRCategoryID categID = MCRCategoryID.fromString(roleID);
            categories.add(categID);
        }
        LOGGER.info("Assigning {} to these roles: {}", user.getUserID(), categories);
        CATEG_LINK_SERVICE.setLinks(ref, categories);
    }

    static Collection<MCRCategoryID> getRoleIDs(MCRUser user) {
        return CATEG_LINK_SERVICE.getLinksFromReference(getLinkID(user));
    }

    static boolean isAssignedToRole(MCRUser user, String roleID) {
        MCRCategoryID categoryID = MCRCategoryID.fromString(roleID);
        MCRCategLinkReference linkReference = getLinkID(user);
        return CATEG_LINK_SERVICE.isInCategory(linkReference, categoryID);
    }

    private static MCRCategLinkReference getLinkID(MCRUser user) {
        return new MCRCategLinkReference(user.getUserName() + "@" + user.getRealmID(),
            MCRUser2Constants.CATEG_LINK_TYPE);
    }

    /**
     * Adds <code>role</code> to the classification system.
     * If the representing {@link MCRCategory} already exists this method does nothing.
     * It will create any category if necessary. 
     */
    public static void addRole(MCRRole role) {
        MCRCategoryID categoryID = null;
        if (role.isSystemRole()) {
            categoryID = new MCRCategoryID(MCRUser2Constants.ROLE_CLASSID.getRootID(), role.getName());
        } else {
            categoryID = MCRCategoryID.fromString(role.getName());
        }
        if (DAO.exist(categoryID)) {
            return;
        }
        MCRCategoryID rootID = MCRCategoryID.rootID(categoryID.getRootID());
        if (!DAO.exist(rootID)) {
            MCRCategoryImpl category = new MCRCategoryImpl();
            category.setId(rootID);
            HashSet<MCRLabel> labels = new HashSet<>();
            labels.add(new MCRLabel("de", "Systemrollen", null));
            labels.add(new MCRLabel("en", "system roles", null));
            category.setLabels(labels);
            DAO.addCategory(null, category);
        }
        MCRCategoryImpl category = new MCRCategoryImpl();
        category.setId(categoryID);
        category.getLabels().addAll(role.getLabels());
        DAO.addCategory(rootID, category);
    }

    /**
     * Deletes a role from the system.
     * If the role is currently not stored in the classification system this method does nothing.
     * This method will fail if any objects (e.g. users) are linked to it.
     */
    public static void deleteRole(String roleID) {
        MCRRole role = MCRRoleManager.getRole(roleID);
        if (role == null) {
            //unknown role
            return;
        }
        MCRCategoryID categoryID = null;
        if (role.isSystemRole()) {
            categoryID = new MCRCategoryID(MCRUser2Constants.ROLE_CLASSID.getRootID(), role.getName());
        } else {
            categoryID = MCRCategoryID.fromString(role.getName());
        }
        DAO.deleteCategory(categoryID);
    }

    /**
     * Returns a collection of userIDs linked to the given <code>role</code>.
     * @param role role to list user IDs.
     */
    public static Collection<String> listUserIDs(MCRRole role) {
        MCRCategoryID categoryID = getCategoryID(role);
        return CATEG_LINK_SERVICE.getLinksFromCategoryForType(categoryID, MCRUser2Constants.CATEG_LINK_TYPE);
    }

    private static MCRCategoryID getCategoryID(MCRRole role) {
        if (role.isSystemRole()) {
            return new MCRCategoryID(MCRUser2Constants.ROLE_CLASSID.getRootID(), role.getName());
        }
        return MCRCategoryID.fromString(role.getName());
    }
}
