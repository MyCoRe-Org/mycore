/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * This class is the user (and group) manager of the MyCoRe system. It is
 * implemented using the singleton design pattern in order to ensure that there
 * is only one instance of this class, i.e. one user manager, running. The user
 * manager has several responsibilities. First it serves as a facade for client
 * classes such as MyCoRe-Servlets to retrieve objects from the persistent
 * datastore. Then the manager is used by the user and group objects themselves
 * to manage their existence in the underlying datastore.
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUserMgr {
    /** The LOGGER and the configuration */
    private static Logger LOGGER = Logger.getLogger(MCRUserMgr.class.getName());

    private static MCRConfiguration config = null;

    private static MCRAccessInterface AI;

    /**
     * flag that determines whether write access to the data is denied (true) or
     * allowed
     */
    private boolean locked = false;

    /** flag that determines whether we use password encryption */
    private boolean useEncryption = false;

    /** the user cache */
    private MCRCache userCache;

    /** the group cache */
    private MCRCache groupCache;

    /** the class responsible for persistent datastore (configurable ) */
    private MCRUserStore mcrUserStore;

    /** The one and only instance of this class */
    private static MCRUserMgr theInstance = null;

    /**
     * private constructor to create the singleton instance.
     */
    private MCRUserMgr() throws MCRException {
        config = MCRConfiguration.instance();

        String userStoreName = config.getString("MCR.userstore_class_name");
        useEncryption = config.getBoolean("MCR.users_use_password_encryption", false);

        try {
            mcrUserStore = (MCRUserStore) Class.forName(userStoreName).newInstance();
        } catch (Exception e) {
            throw new MCRException("MCRUserStore error", e);
        }

        userCache = new MCRCache(20); // resonable values? This might also be
        groupCache = new MCRCache(10); // read from mycore.properties....

        AI = MCRAccessManager.getAccessImpl();
    }

    /**
     * This method is the only way to get an instance of this class. It calls
     * the private constructor to create the singleton.
     * 
     * @return returns the one and only instance of <CODE>MCRUserMgr</CODE>
     */
    public final static synchronized MCRUserMgr instance() throws MCRException {
        if (theInstance == null) {
            theInstance = new MCRUserMgr();
        }

        return theInstance;
    }

    /**
     * This method checks the consistency of the user and group data. It should
     * be executed after importing data from xml files, e.g.
     */
    public final void checkConsistency() throws MCRException {
        locked = true; // we now run in the read only mode

        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession session = MCRSessionMgr.getCurrentSession();

        if (session == null) {
            return;
        }
        // For this action you must have list rights.
        if (!AI.checkPermission("administrate-user") && !session.getCurrentUserID().equals("root")) {
            locked = false;
            throw new MCRException("The session does not have the permission \"administrate-user\"!");
        }

        // For all users in the system get their groups and check if the groups
        // really exist at all.
        // We do not need to check if the user is a member of the groups listed
        // in his or her groups
        // list since the user object is constructed from the data - so he or
        // she *must* be a member
        // by definition. However, since the primary group of the user is added
        // automatically to the
        // group list in MCRUser, we have to check if this group has the user as
        // one of its members.
        LOGGER.info("Consistency check is started.");

        ArrayList allUserIDs = mcrUserStore.getAllUserIDs();

        for (int i = 0; i < allUserIDs.size(); i++) {
            MCRUser currentUser = retrieveUser((String) allUserIDs.get(i), true);
            ArrayList currentGroupIDs = currentUser.getGroupIDs();

            for (int j = 0; j < currentGroupIDs.size(); j++) {
                if (!mcrUserStore.existsGroup((String) currentGroupIDs.get(j))) {
                    LOGGER.error("user : '" + currentUser.getID() + "' error: unknown group '" + (String) currentGroupIDs.get(j) + "'!");
                }
            }

            MCRGroup primaryGroup = retrieveGroup(currentUser.getPrimaryGroupID(), true);
            ArrayList mbrUserIDs = primaryGroup.getMemberUserIDs();

            if (!mbrUserIDs.contains(currentUser.getID())) {
                LOGGER.error("user : '" + currentUser.getID() + "' error: is not member of" + " primary group '" + currentUser.getPrimaryGroupID() + "'!");
            }
        }

        // For all groups get the admins and members (user and group lists,
        // respectively) and check
        // if they have unknown users as admins or members, unknown groups as
        // admins or members etc.
        ArrayList allGroupIDs = mcrUserStore.getAllGroupIDs();

        for (int i = 0; i < allGroupIDs.size(); i++) {
            MCRGroup currentGroup = retrieveGroup((String) allGroupIDs.get(i), true);

            // check the admin users
            ArrayList admUserIDs = currentGroup.getAdminUserIDs();

            for (int j = 0; j < admUserIDs.size(); j++) {
                if (!mcrUserStore.existsUser((String) admUserIDs.get(j))) {
                    LOGGER.error("group: '" + currentGroup.getID() + "' error: unknown admin" + " user '" + (String) admUserIDs.get(j) + "'!");
                }
            }

            // check the admin groups
            ArrayList admGroupIDs = currentGroup.getAdminGroupIDs();

            for (int j = 0; j < admGroupIDs.size(); j++) {
                if (!mcrUserStore.existsGroup((String) admGroupIDs.get(j))) {
                    LOGGER.error("group: '" + currentGroup.getID() + "' error: unknown admin" + " group '" + (String) admGroupIDs.get(j) + "'!");
                }
            }

            // check the users (members)
            ArrayList mbrUserIDs = currentGroup.getMemberUserIDs();

            for (int j = 0; j < mbrUserIDs.size(); j++) {
                if (!mcrUserStore.existsUser((String) mbrUserIDs.get(j))) {
                    LOGGER.error("group: '" + currentGroup.getID() + "' error: unknown user '" + (String) mbrUserIDs.get(j) + "'!");
                }
            }
        }

        LOGGER.info("done.");
        locked = false; // write access is allowed again
    }

    /**
     * This method creates a group in the datastore (and the group cache as
     * well). The logical correctness of the data is checked.
     * 
     * @param group
     *            The group object to be created
     */
    public final synchronized void createGroup(MCRGroup group) throws MCRException {
        if (locked) {
            throw new MCRException("The user component is locked. At the moment write access is denied.");
        }

        // Check the permissions
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRUser admin = retrieveUser(session.getCurrentUserID(), false);

        if ((!AI.checkPermission("create-group"))) {
            throw new MCRException("The current user does not have the permission to create a group!");
        }

        // Check if there really is a non-null group object provided
        if (group == null) {
            throw new MCRException("The given group object is null.");
        }

        // Check if the given group object is valid.
        if (!group.isValid()) {
            throw new MCRException("The given group object is not valid.");
        }

        // Check if the group already exists.
        if (mcrUserStore.existsGroup(group.getID())) {
            throw new MCRException("The group '" + group.getID() + "' already exists!");
        }

        // We first check whether this group has admins (users or groups) and if
        // so,
        // whether they exist at all.
        group.addAdminUserID(admin.getID());

        ArrayList admUserIDs = group.getAdminUserIDs();

        for (int j = 0; j < admUserIDs.size(); j++) {
            if (!mcrUserStore.existsUser((String) admUserIDs.get(j))) {
                throw new MCRException("MCRUserMgr.createGroup(): unknown admin userID: " + (String) admUserIDs.get(j));
            }
        }

        group.addAdminGroupID(admin.getPrimaryGroupID());

        ArrayList admGroupIDs = group.getAdminGroupIDs();

        for (int j = 0; j < admGroupIDs.size(); j++) {
            if (((String) admGroupIDs.get(j)).equals(group.getID())) {
                continue;
            }

            if (!mcrUserStore.existsGroup((String) admGroupIDs.get(j))) {
                throw new MCRException("MCRUserMgr.createGroup(): unknown admin groupID: " + (String) admGroupIDs.get(j));
            }
        }

        try {
            // Set some data by the manager
            group.setCreationDate();
            group.setModifiedDate();
            group.setCreator(admin.getID());

            // Just create the group. The group must be created before updating
            // the groups this
            // group is a member of because the existence of the group will be
            // checked while
            // updating the groups.
            mcrUserStore.createGroup(group);
        } catch (Exception ex) {
            // Since something went wrong we delete the previously created
            // group. We do this
            // using this.deleteGroup() in order to ensure that already updated
            // groups will
            // be resetted to the original state as well.
            try {
                deleteGroup(group.getID());
            } catch (MCRException e) {
            }

            LOGGER.error("Can't create MCRGroup", ex);
            throw new MCRException("Can't create MCRGroup.", ex);
        }
    }

    /**
     * This method creates a user in the datastore (and the user cache as well).
     * The logical correctness of the data is checked.
     * 
     * @param user
     *            The user object which will be created
     */
    public final synchronized void createUser(MCRUser user) throws MCRException {
        if (locked) {
            throw new MCRException("The user component is locked. At the moment write access is denied.");
        }

        // Check the permissions
        MCRSession session = MCRSessionMgr.getCurrentSession();

        if (!AI.checkPermission("create-user")) {
            throw new MCRException("The current user does not have the permission to create a user!");
        }

        // Check if there really is a non-null object provided
        if (user == null) {
            throw new MCRException("The given user object is null.");
        }

        // Check if the given user object is valid.
        if (!user.isValid()) {
            throw new MCRException("The given user object is not valid.");
        }

        // Check if the user already exists.
        if (mcrUserStore.existsUser(user.getNumID(), user.getID())) {
            throw new MCRException("The user '" + user.getID() + "' or numerical ID '" + user.getNumID() + "' already exists!");
        }

        // Check if the primary group exists and if so, whether the current user
        // may modify the group
        if (!mcrUserStore.existsGroup(user.getPrimaryGroupID())) {
            throw new MCRException("The primary group of the user '" + user.getID() + "' does not exist.");
        }

        MCRGroup primGroup = retrieveGroup(user.getPrimaryGroupID());
        primGroup.modificationIsAllowed();

        // Check if the groups the user will be a member of really exist
        ArrayList groupIDs = user.getGroupIDs();

        for (int j = 0; j < groupIDs.size(); j++) {
            if (!mcrUserStore.existsGroup((String) groupIDs.get(j))) {
                throw new MCRException("The user '" + user.getID() + "' is linked to the unknown group '" + groupIDs.get(j) + "'.");
            }

            // With the following we test if the current user may modify the
            // groups this user
            // will be a member of. It is important to check this prior to
            // creating the user
            // such that we do not have to make a rollback.
            MCRGroup linkedGroup = this.retrieveGroup((String) groupIDs.get(j), true);
            linkedGroup.modificationIsAllowed();
        }

        try {
            // Set some data by the manager
            user.setCreationDate();
            user.setModifiedDate();
            user.setCreator(session.getCurrentUserID());

            // At first create the user. The user must be created before
            // updating the groups
            // because the existence of the user will be checked while updating
            // the groups.
            mcrUserStore.createUser(user);

            // now we update the primary group
            primGroup.addMemberUserID(user.getID());
            groupCache.remove(primGroup.getID());
            mcrUserStore.updateGroup(primGroup);

            // now update the other groups
            for (int i = 0; i < groupIDs.size(); i++) {
                MCRGroup otherGroup = retrieveGroup((String) groupIDs.get(i), true);
                otherGroup.addMemberUserID(user.getID());
                groupCache.remove(otherGroup.getID());
                mcrUserStore.updateGroup(otherGroup);
            }
        } catch (MCRException ex) {
            // Since something went wrong we delete the previously created user.
            // We do this
            // using this.deleteUser() in order to ensure that already updated
            // groups will
            // be resetted to the original state as well.
            try {
                deleteUser(user.getID());
            } catch (Exception e) {
            }

            throw new MCRException("Can't create user.", ex);
        }
    }

    /**
     * This method deletes a group from the datastore (and the group cache as
     * well).
     * 
     * @param groupID
     *            The group ID which will be deleted
     */
    public final synchronized void deleteGroup(String groupID) throws MCRException {
        if (locked) {
            throw new MCRException("The user component is locked. At the moment write" + " access is denied.");
        }

        // Check the permissions
        if (!AI.checkPermission("delete-group")) {
            throw new MCRException("The current user does not have the permission to delete a group!");
        }

        // Check if the group exists at all
        if (!mcrUserStore.existsGroup(groupID)) {
            throw new MCRException("The group '" + groupID + "' is unknown!");
        }

        // Now we check if there are users in the system which have this group
        // as their primary
        // group. If so, this group cannot be deleted. First the users must be
        // updated.
        ArrayList primUserIDs = mcrUserStore.getUserIDsWithPrimaryGroup(groupID);

        if (primUserIDs.size() > 0) {
            throw new MCRException("Group '" + groupID + "' can't be deleted since there" + " are users with '" + groupID + "' as their primary group. First update or" + " delete the users!");
        }

        try {
            // It is sufficient to remove the members from the
            // caches. The next time they will be used they will be rebuild from
            // the datastore and
            // hence no longer have this group in their group lists.
            MCRGroup delGroup = retrieveGroup(groupID);

            for (int i = 0; i < delGroup.getMemberUserIDs().size(); i++) {
                userCache.remove(delGroup.getMemberUserIDs().get(i));

                MCRUser uuser = retrieveUser((String) delGroup.getMemberUserIDs().get(i), false);
                uuser.removeGroupID(groupID);
                mcrUserStore.updateUser(uuser);
            }

            // Remove all admin items from other groups
            ArrayList ogroups = mcrUserStore.getGroupIDsWithAdminUser(groupID);

            for (int i = 0; i < ogroups.size(); i++) {
                groupCache.remove(ogroups.get(i));

                MCRGroup agroup = retrieveGroup((String) ogroups.get(i));
                agroup.removeAdminGroupID(groupID);
                mcrUserStore.updateGroup(agroup);
            }
        } catch (Exception ex) {
            throw new MCRException("Can't delete user " + groupID, ex);
        } finally {
            groupCache.remove(groupID);
            mcrUserStore.deleteGroup(groupID);
        }
        LOGGER.info("User with ID " + groupID + " deleted.");
    }

    /**
     * This method deletes a user from the datastore (and the user cache as
     * well).
     * 
     * @param userID
     *            The user ID which will be deleted
     */
    public final synchronized void deleteUser(String userID) throws MCRException {
        if (locked) {
            throw new MCRException("The user component is locked. At the moment write" + " access is denied.");
        }

        // Check the permission
        if (!AI.checkPermission("delete-user")) {
            throw new MCRException("The current user does not have the permission to delete a user!");
        }

        // Check if the user exists at all
        if (!mcrUserStore.existsUser(userID)) {
            throw new MCRException("User '" + userID + "' is unknown!");
        }

        MCRUser user = retrieveUser(userID, false);

        if (!user.isUpdateAllowed()) {
            throw new MCRException("Delete for user '" + userID + "' is not allowed!");
        }

        // We have to notify the groups where this user is an administrative
        // user
        ArrayList adminGroups = mcrUserStore.getGroupIDsWithAdminUser(userID);
        for (int i = 0; i < adminGroups.size(); i++) {
            try {
                MCRGroup adminGroup = retrieveGroup((String) adminGroups.get(i));
                adminGroup.removeAdminUserID(userID);
                groupCache.remove(adminGroup.getID());
                mcrUserStore.updateGroup(adminGroup);
            } catch (Exception ex) {
                LOGGER.warn("Can't remove " + userID + "from group " + adminGroups.get(i));
            }
        }

        // We have to notify the groups this user is a member of
        for (int i = 0; i < user.getGroupIDs().size(); i++) {
            try {
                MCRGroup currentGroup = retrieveGroup((String) user.getGroupIDs().get(i));
                currentGroup.removeMemberUserID(userID);
                groupCache.remove(currentGroup.getID());
                mcrUserStore.updateGroup(currentGroup);
            } catch (Exception ex) {
                LOGGER.warn("Can't remove " + userID + "from group " + adminGroups.get(i));
            }
        }
        userCache.remove(userID);
        mcrUserStore.deleteUser(userID);
        LOGGER.info("User with ID " + userID + " deleted.");
    }

    /**
     * This method disables the user.
     * 
     * @param userID
     *            The user object which will be disabled
     */
    public final void disableUser(String userID) throws MCRException {
        enable(userID, false);
        LOGGER.info("User with ID " + userID + " disabled!");
    }

    /**
     * This method enables the user.
     * 
     * @param userID
     *            The user object which will be enabled
     */
    public final void enableUser(String userID) throws MCRException {
        enable(userID, true);
        LOGGER.info("User with ID " + userID + " enabled!");
    }

    /**
     * This method gets all group IDs from the persistent datastore and returns
     * them as a ArrayList of strings.
     * 
     * @return ArrayList of strings containing the group IDs of the system.
     */
    public final synchronized ArrayList getAllGroupIDs() throws MCRException {
        return mcrUserStore.getAllGroupIDs();
    }

    /**
     * This method returns a JDOM presentation of all groups of the system
     * 
     * @return JDOM document presentation of all groups of the system
     */
    public final synchronized org.jdom.Document getAllGroups() throws MCRException {
        // Build the DOM
        MCRGroup currentGroup = null;
        org.jdom.Element root = new org.jdom.Element("mycoregroup");
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        root.setAttribute("noNamespaceSchemaLocation", "MCRGroup.xsd", org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));

        ArrayList allGroupIDs = mcrUserStore.getAllGroupIDs();

        for (int i = 0; i < allGroupIDs.size(); i++) {
            currentGroup = mcrUserStore.retrieveGroup((String) allGroupIDs.get(i));
            root.addContent(currentGroup.toJDOMElement());
        }

        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        return jdomDoc;
    }

    /**
     * This method gets all user IDs from the persistent datastore and returns
     * them as an ArrayList of strings.
     * 
     * @return ArrayList of strings containing the user IDs of the system.
     */
    public final synchronized ArrayList getAllUserIDs() throws MCRException {
        return mcrUserStore.getAllUserIDs();
    }

    /**
     * This method returns a JDOM presentation of all users of the system
     * 
     * @return JDOM document presentation of all users of the system
     */
    public final synchronized org.jdom.Document getAllUsers() throws MCRException {
        // Build the DOM
        MCRUser currentUser;
        org.jdom.Element root = new org.jdom.Element("mycoreuser");
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        root.setAttribute("noNamespaceSchemaLocation", "MCRUser.xsd", org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));

        ArrayList allUserIDs = mcrUserStore.getAllUserIDs();

        for (int i = 0; i < allUserIDs.size(); i++) {
            currentUser = mcrUserStore.retrieveUser((String) allUserIDs.get(i));
            root.addContent(currentUser.toJDOMElement());
        }

        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        return jdomDoc;
    }

    /**
     * The access control subsystem needs to know the current working user. This
     * information is held in the session, so we just ask the session about it.
     * 
     * @return the current user
     */
    public final MCRUser getCurrentUser() {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        return retrieveUser(mcrSession.getCurrentUserID(), false);
    }

    /**
     * This method determines in which groups of a given set of groups a given
     * user is a member of.
     * 
     * @return set of groups the given user is a member of
     */
    public final Set getGroupsContainingUser(MCRUser user, Set groups) {
        Set set = new HashSet();
        Iterator iterator = groups.iterator();

        while (iterator.hasNext()) {
            Object element = iterator.next();

            if (element instanceof MCRGroup) {
                MCRGroup group = (MCRGroup) element;

                if (group.hasUserMember(user)) {
                    set.add(group);
                }
            }
        }

        return set;
    }

    /**
     * This method determines in which groups is the user member.
     * 
     * @return an ArrayList of groups where the given user is a member of
     */
    public final ArrayList getGroupsContainingUser(String user) {
        MCRUser u = retrieveUser(user);
        ArrayList a = getAllGroupIDs();
        ArrayList o = new ArrayList();
        for (int i = 0; i < a.size(); i++) {
            String aa = (String) a.get(i);
            if (retrieveGroup(aa).hasUserMember(u)) {
                if (!o.contains(aa)) {
                    o.add(aa);
                }
            }
        }
        return o;
    }

    /**
     * This method returns the maximum value of the numerical user IDs
     * 
     * @return maximum value of the numerical user IDs
     */
    public final int getMaxUserNumID() throws MCRException {
        // whitout checking a permission, because we only want to see it
        return mcrUserStore.getMaxUserNumID();
    }

    /**
     * This method returns the ID of the primary group for a given userID.
     * 
     * @param userID
     *            the userID for which the primary group ID is requested
     * @return groupID the ID of the primary group of the user
     */
    public final String getPrimaryGroupIDOfUser(String userID) {
        try {
            MCRUser u = retrieveUser(userID);

            return u.getPrimaryGroupID();
        } catch (MCRException e) {
        }

        return null;
    }

    /**
     * This method imports a user or a group to the mycore system. Importing a
     * user or a group is essentially the same as creating a user or a group.
     * However, the data will be imported without checking the logical
     * correctness. This method will be used for administrative purposes, i.e.
     * restoring a set of users after a crash of the database. Another
     * difference is that the values for the creator, creation date and modified
     * date are taken from the given user or group object. This is important if
     * the user or group is read from an xml file and was formerly created in a
     * different system.
     * 
     * @param userObject
     *            The user or group object which will be imported
     */
    public final synchronized void importUserObject(MCRUserObject obj) throws MCRException {
        try {
            // Check that the user system is not locked
            if (locked) {
                throw new MCRException("The user component is locked. At the moment write access is denied.");
            }

            // is it not NULL
            if (obj == null) {
                throw new MCRException("The provided user system object is null.");
            }

            // Check validation
            if (!obj.isValid()) {
                throw new MCRException("The data of the provided user or group object is not valid.");
            }

            // backup up creator and dates
            String creator = obj.getCreator();
            java.sql.Timestamp created = obj.getCreationDate();
            java.sql.Timestamp modified = obj.getModifiedDate();

            // now create the user or group
            if (obj instanceof MCRUser) {
                // Check exist user
                if (mcrUserStore.existsUser(((MCRUser) obj).getID())) {
                    throw new MCRException("The user '" + obj.getID() + "' already exists!");
                }
                // Check if the primary group exists
                String primarygroup = ((MCRUser) obj).getPrimaryGroupID();
                if (!mcrUserStore.existsGroup(primarygroup)) {
                    throw new MCRException("The primary group of the user '" + obj.getID() + "' does not exist.");
                }
                // Check if the groups the user will be a member of really exist
                ArrayList groupIDs = ((MCRUser) obj).getGroupIDs();
                for (int i = 0; i < groupIDs.size(); i++) {
                    if (!mcrUserStore.existsGroup((String) groupIDs.get(i))) {
                        throw new MCRException("The user '" + obj.getID() + "' is linked to the unknown group '" + groupIDs.get(i) + "'.");
                    }
                }
                // At first create the user. The user must be created before
                // updating the groups because the existence of the user will
                // be checked while updating the groups.
                mcrUserStore.createUser((MCRUser) obj);
                // now we update the primary group
                MCRGroup primGroup = mcrUserStore.retrieveGroup(primarygroup);
                primGroup.addMemberUserID(((MCRUser) obj).getID());
                groupCache.remove(primGroup.getID());
                mcrUserStore.updateGroup(primGroup);
                // now update the other groups
                for (int i = 0; i < groupIDs.size(); i++) {
                    MCRGroup otherGroup = retrieveGroup((String) groupIDs.get(i), true);
                    otherGroup.addMemberUserID(((MCRUser) obj).getID());
                    groupCache.remove(otherGroup.getID());
                    mcrUserStore.updateGroup(otherGroup);
                }

            } else {
                // Check exist group
                if (mcrUserStore.existsGroup(((MCRGroup) obj).getID())) {
                    throw new MCRException("The group '" + obj.getID() + "' already exists!");
                }
                // create data
                mcrUserStore.createGroup((MCRGroup) obj);
            }

            // finally set the old values and update the user or group
            obj.setCreator(creator);
            obj.setCreationDate(created);
            obj.setModifiedDate(modified);

            if (obj instanceof MCRUser) {
                mcrUserStore.updateUser((MCRUser) obj);
            } else {
                mcrUserStore.updateGroup((MCRGroup) obj);
            }
            LOGGER.info("User or group with ID " + obj.getID() + " imported.");
        } catch (MCRException ex) {
            setLock(false);
            throw new MCRException("Can't import user or group.", ex);
        }
    }

    /**
     * This method is used by the initialization process of the user/group
     * system to create a starting configuration without checking the
     * consistency of the data. It is also used when importing groups into the
     * system.
     * 
     * @param group
     *            The group object which should be created
     * @param creator
     *            the creator
     */
    public final synchronized void initializeGroup(MCRGroup group, String creator) throws MCRException {
        if (locked) { // This is very unlikely to happen since we are just

            // initialising the system.
            throw new MCRException("The user component is locked. At the moment write access is denied.");
        }

        if (group == null) {
            throw new MCRException("The provided group object is null.");
        }

        if ((creator == null) || ((creator = creator.trim()).length() == 0)) {
            throw new MCRException("The value for creator is null or empty.");
        }

        if (!group.isValid()) {
            throw new MCRException("The data of the provided group object is not valid.");
        }

        if (mcrUserStore.existsGroup(group.getID())) {
            throw new MCRException("The group '" + group.getID() + "' already exists!");
        }

        try {
            mcrUserStore.createGroup(group);
        } catch (Exception ex) {
            throw new MCRException("Can't initalize group system.", ex);
        }
    }

    /**
     * This method is used by the initialization process of the user/group
     * system to create a starting configuration without checking the
     * consistency of the data. It is also used when importing users into the
     * system.
     * 
     * @param user
     *            The user object which should be created
     * @param creator
     *            The creator
     */
    public final synchronized void initializeUser(MCRUser user, String creator) throws MCRException {
        if (locked) { // This is very unlikely to happen since we are just

            // initialising the system.
            throw new MCRException("The user component is locked. At the moment write access is denied.");
        }

        if (user == null) {
            throw new MCRException("The provided user object is null.");
        }

        if ((creator == null) || ((creator = creator.trim()).length() == 0)) {
            throw new MCRException("The value for creator is null or empty.");
        }

        if (!user.isValid()) {
            throw new MCRException("The data of the provided user object is not valid.");
        }

        if (mcrUserStore.existsUser(user.getNumID(), user.getID())) {
            throw new MCRException("The user '" + user.getID() + "' or numerical ID '" + user.getNumID() + "' already exists!");
        }

        try {
            mcrUserStore.createUser(user);
        } catch (MCRException ex) {
            throw new MCRException("Can't initialize user system.", ex);
        }
    }

    /**
     * This method checks if the user is authenticated, i.e. if he or she is in
     * the current session.
     * 
     * @return returns true if the user is authenticated
     */
    public final static boolean isAuthenticated(MCRUser user) {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        String sessionUser = mcrSession.getCurrentUserID();

        return (sessionUser.equals(user.getID())) ? true : false;
    }

    /**
     * return This method returns true is if the user manager is in the locked
     * state
     */
    public final boolean isLocked() {
        return locked;
    }

    /**
     * login to the system. This method just checks the password for a given
     * user.
     * 
     * @param userID
     *            user ID for the login
     * @param passwd
     *            password for the user
     * @return true if the password matches the password stored, false otherwise
     */
    public synchronized boolean login(String userID, String passwd) throws MCRException {
        MCRUser loginUser = retrieveUser(userID, false);

        if (loginUser.isEnabled()) {
            if (useEncryption) {
                String salt = loginUser.getPassword().substring(0, 3);
                String newCrypt = MCRCrypt.crypt(salt, passwd);

                return (loginUser.getPassword().equals(newCrypt));
            }

            return (loginUser.getPassword().equals(passwd));
        }

        throw new MCRException("Login denied. User is disabled.");
    }

    /**
     * This method retrieves the group object with the given group ID.
     * 
     * @param groupID
     *            string representing the requested group object
     * @return MCRGroup group object (if available)
     * @exception MCRException
     *                if group object is not known
     */
    public MCRGroup retrieveGroup(String groupID) throws MCRException {
        return this.retrieveGroup(groupID, false);
    }

    /**
     * This method first looks for a given groupID in the group cache and
     * returns this group object. In case that the group object is not in the
     * cache, the group will be retrieved from the database. Then the group
     * object is put into the cache.
     * 
     * @param groupID
     *            string representing the requested group object
     * @param bFromDataStore
     *            boolean value, if true the group must be retrieved directly
     *            from the data store
     * @return MCRGroup group object (if available)
     * @exception MCRException
     *                if group object is not known
     */
    protected synchronized MCRGroup retrieveGroup(String groupID, boolean bFromDataStore) throws MCRException {
        // In order to compare a modified group object with the persistent one
        // we must
        // be able to force this method to get the group from the store
        MCRGroup reqGroup;
        reqGroup = (bFromDataStore) ? null : (MCRGroup) groupCache.get(groupID);

        if (reqGroup == null) { // We do not have this group in the cache.
            try {
                reqGroup = mcrUserStore.retrieveGroup(groupID);
            } catch (MCRException e) {
                throw new MCRException("MCRUserMgr.retrieveGroup(): Unknown group '" + groupID + "'!");
            }

            if (reqGroup == null) {
                throw new MCRException("MCRUserMgr.retrieveGroup(): Unknown group '" + groupID + "'!");
            }

            groupCache.put(groupID, reqGroup);
        }

        return reqGroup;
    }

    /**
     * In the access control subsystem only IDs are stored, not references to
     * user or group objects. Therefore the user system must provide a method to
     * retrieve a set of groups according to a set of given group IDs.
     * 
     * @param groupIDs
     *            A set of group IDs for which the group objects are to be
     *            retrieved
     * @return set of groups according to the given set of group IDs
     * @throws MCRException
     */
    public final Set retrieveGroups(Set groupIDs) throws MCRException {
        Set groups = new HashSet();
        Iterator iterator = groupIDs.iterator();

        while (iterator.hasNext()) {
            Object object = iterator.next();

            if (object instanceof String) {
                String groupid = (String) object;
                MCRGroup group = retrieveGroup(groupid);

                if (group != null) {
                    groups.add(group);
                }
            }
        }

        return groups;
    }

    /**
     * This method first retrieves the user object with the given userID.
     * 
     * @param userID
     *            string representing the requested user object
     * @return MCRUser user object (if available), otherwise null
     * @exception MCRException
     *                if user object is not known
     */
    public MCRUser retrieveUser(String userID) throws MCRException {
        return this.retrieveUser(userID, false);
    }

    /**
     * This method first looks for a given userID in the user cache and returns
     * this user object. In case that the user object is not in the cache, the
     * user will be retrieved from the database. Then the user object is put
     * into the cache.
     * 
     * @param userID
     *            string representing the requested user object
     * @param bFromDataStore
     *            boolean value, if true the user must be retrieved directly
     *            from the data store
     * @return MCRUser user object (if available), otherwise null
     * @exception MCRException
     *                if user object is not known
     */
    protected synchronized MCRUser retrieveUser(String userID, boolean bFromDataStore) throws MCRException {
        // In order to compare a modified user object with the persistent one we
        // must
        // be able to force this method to get the user from the store
        MCRUser reqUser = null;

        if (bFromDataStore) {
            reqUser = (MCRUser) userCache.get(userID);
        }

        if (reqUser == null) { // We do not have this user in the cache

            try {
                reqUser = mcrUserStore.retrieveUser(userID);
            } catch (MCRException e) {
                LOGGER.warn(e.getMessage());
                throw new MCRException("user can't be found in the database");
            }

            if (reqUser != null) {
                userCache.put(userID, reqUser);
            }
        }

        return reqUser;
    }

    /**
     * In the access control subsystem only IDs are stored, not references to
     * user or group objects. Therefore the user system must provide a method to
     * retrieve a set of users according to a set of given user IDs.
     * 
     * @param userIDs
     *            A set of user IDs for which the user objects are to be
     *            retrieved
     * @return set of users according to the given set of user IDs
     * @throws MCRException
     */
    public final Set retrieveUsers(Set userIDs) {
        Set users = new HashSet();
        Iterator iterator = userIDs.iterator();

        while (iterator.hasNext()) {
            Object object = iterator.next();

            if (object instanceof String) {
                String userid = (String) object;
                MCRUser user = retrieveUser(userid, false);

                if (user != null) {
                    users.add(user);
                }
            }
        }

        return users;
    }

    /**
     * This method sets the lock-status of the user manager.
     * 
     * @param locked
     *            flag that determines whether write access to the data is
     *            denied (true) or allowed
     */
    public final synchronized void setLock(boolean locked) {
        // Check the permissions
        if (!AI.checkPermission("administrate-user")) {
            throw new MCRException("The current user does not have the permission to  'administrate-user'!");
        }

        this.locked = locked;
        if (locked)
            LOGGER.info("Write access to the user component persistent database now is denied.");
        else
            LOGGER.info("Write access to the user component persistent database now is allowed.");
    }

    /**
     * This method sets a new password for the given user.
     * 
     * @param userID
     *            the userID
     * @param password
     *            the user password
     */
    public final void setPassword(String userID, String password) throws MCRException {
        // check for empty strings
        if ((userID == null) || ((userID = userID.trim()).length() == 0)) {
            throw new MCRException("The userID is null or empty!");
        }

        if ((password == null) || ((password = password.trim()).length() == 0)) {
            throw new MCRException("The password is null or empty!");
        }

        // Check permissions
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRUser user = retrieveUser(userID, false);

        if (!user.isUpdateAllowed()) {
            throw new MCRException("The update for the user " + userID + " is not allowed!");
        }

        boolean test = false;

        if (session.getCurrentUserID().equals(userID)) {
            test = true;
        } else {
            if (AI.checkPermission("administrate-user")) {
                test = true;
            }
        }

        if (!test) {
            throw new MCRException("You have no rights to change the users password!");
        }

        if (useEncryption) {
            user.setPassword(MCRCrypt.crypt(password));
        } else {
            user.setPassword(password);
        }

        userCache.remove(userID);
        mcrUserStore.updateUser(user);
        LOGGER.info("The new password was set.");
    }

    /**
     * This method updates a group in the datastore (and the cache as well).
     * 
     * @param session
     *            the MCRSession object
     * @param updGroup
     *            The group object which will be updated
     */
    public final synchronized void updateGroup(MCRGroup updGroup) throws MCRException {
        if (locked) {
            throw new MCRException("The user component is locked. At the moment write access is denied.");
        }

        // Check the permission
        if (!AI.checkPermission("modify-group")) {
            throw new MCRException("The current user does not have the permission to modify this group!");
        }

        // check that the updGroup is valid
        if (updGroup == null) {
            throw new MCRException("The provided group object is null!");
        }

        if (!updGroup.isValid()) {
            throw new MCRException("The provided group object is not valid.");
        }

        // check that the group which shall be updated really exists
        String groupID = updGroup.getID();

        if (!mcrUserStore.existsGroup(groupID)) {
            throw new MCRException("You tried to update the unknown group '" + groupID + "'.");
        }

        try {
            // get the group directly from the datastore
            MCRGroup oldGroup = retrieveGroup(groupID, true);

            // At first we check that admins or members or permissions which may
            // be added to this
            // group really exist at all. Because this is not a performance
            // issue, we simply
            // check that all members etc. exist. This should be improved later.
            // We look for newly
            // added admin users in this group:
            for (int i = 0; i < updGroup.getAdminUserIDs().size(); i++) {
                String userID = (String) updGroup.getAdminUserIDs().get(i);

                if (!mcrUserStore.existsUser(userID)) {
                    throw new MCRException("You tried to add the unknown admin user '" + userID + "' to the group '" + groupID + "'.");
                }
            }

            // We look for newly added admin groups in this group:
            for (int i = 0; i < updGroup.getAdminGroupIDs().size(); i++) {
                String gid = (String) updGroup.getAdminGroupIDs().get(i);

                if (!mcrUserStore.existsGroup(gid)) {
                    throw new MCRException("You tried to add the unknown admin group '" + gid + "' to the group '" + groupID + "'.");
                }
            }

            // We look for newly added member users in this group:
            for (int i = 0; i < updGroup.getMemberUserIDs().size(); i++) {
                String userID = (String) updGroup.getMemberUserIDs().get(i);

                if (!mcrUserStore.existsUser(userID)) {
                    throw new MCRException("You tried to add the unknown member user '" + userID + "' to the group '" + groupID + "'.");
                }
            }

            // Some values are taken from the old version, they cannot be
            // updated. Then we
            // really update the object in the datastore.
            updGroup.setCreationDate(oldGroup.getCreationDate());
            updGroup.setCreator(oldGroup.getCreator());
            mcrUserStore.updateGroup(updGroup);
            groupCache.remove(groupID);
            groupCache.put(groupID, updGroup);

            // We finally look again for recently added and deleted members
            // (users). If
            // so, we do not have to notify them, since the users and groups get
            // their membership
            // information when they are constructed from the datastore.
            // However, we have to remove
            // them from the user or group cache so that they will be retrieved
            // from the datastore.
            for (int i = 0; i < oldGroup.getMemberUserIDs().size(); i++) {
                if (!updGroup.getMemberUserIDs().contains(oldGroup.getMemberUserIDs().get(i))) {
                    userCache.remove(oldGroup.getMemberUserIDs().get(i));
                }
            }

            for (int i = 0; i < updGroup.getMemberUserIDs().size(); i++) {
                if (!oldGroup.getMemberUserIDs().contains(updGroup.getMemberUserIDs().get(i))) {
                    userCache.remove(updGroup.getMemberUserIDs().get(i));
                }
            }
        } catch (MCRException ex) {
            throw new MCRException("Error while updating group " + updGroup.getID(), ex);
        }
    }

    /**
     * This method updates a user in the datastore (and the cache as well).
     * 
     * @param user
     *            The user object which will be updated
     */
    public final synchronized void updateUser(MCRUser updUser) throws MCRException {
        if (locked) {
            throw new MCRException("The user component is locked. At the moment write access is denied.");
        }

        // Check the permissions
        if (!AI.checkPermission("modify-user")) {
            throw new MCRException("The current user does not have the permission to modify this user!");
        }

        // Check that the provided user object is valid
        if (updUser == null) {
            throw new MCRException("The provided user object is null!");
        }

        if (!updUser.isValid()) {
            throw new MCRException("The provided user object is not valid.");
        }

        // Check that the user exists
        if (!mcrUserStore.existsUser(updUser.getID())) {
            throw new MCRException("You tried to update the unknown user '" + updUser.getID() + "'.");
        }

        try {
            // get the user directly from the datastore
            MCRUser oldUser = mcrUserStore.retrieveUser(updUser.getID());

            // Check whether the primary group has changed and if so, if the
            // current user
            // may modify the old and new primary group. In order to avoid a
            // rollback we only
            // test here, changes will be made later.
            if (!updUser.getPrimaryGroupID().equals(oldUser.getPrimaryGroupID())) {
                MCRGroup testGroup = retrieveGroup(updUser.getPrimaryGroupID(), false);
                testGroup.modificationIsAllowed();
                testGroup = retrieveGroup(oldUser.getPrimaryGroupID(), false);
                testGroup.modificationIsAllowed();
            }

            // We have to check whether the membership to some of the groups of
            // this user changed.
            // For example, the user might be removed from one of the groups he
            // or she was
            // a member of. This group must be notified! To get information
            // about which groups
            // have been added or removed, we compare the current (updated) user
            // object with
            // the one from the datastore before the update process takes place.
            update(updUser, oldUser);

            // Now we come back to the primary group.
            if (!updUser.getPrimaryGroupID().equals(oldUser.getPrimaryGroupID())) {
                MCRGroup primGroup = retrieveGroup(oldUser.getPrimaryGroupID());
                primGroup.removeMemberUserID(oldUser.getID());
                this.updateGroup(primGroup);
                primGroup = retrieveGroup(updUser.getPrimaryGroupID());
                primGroup.addMemberUserID(updUser.getID());
                this.updateGroup(primGroup);
            }

            // Some values are taken from the old version, they cannot be
            // updated. Then we
            // really update the object in the datastore.
            updUser.setCreationDate(oldUser.getCreationDate());
            updUser.setCreator(oldUser.getCreator());
            mcrUserStore.updateUser(updUser);
            userCache.remove(updUser.getID());
            userCache.put(updUser.getID(), updUser);
        } catch (MCRException ex) {
            try {
                deleteUser(updUser.getID());
            } catch (MCRException e) {
            }

            throw new MCRException("Error while updating user " + updUser.getID() + ", the user has been deleted.");
        }
    }

    /**
     * This method updates groups where a given user is a new member of or is no
     * longer a member of, respectively. The groups are determined by comparing
     * the user to be updated with the version out of the datastore, i.e. before
     * the update request.
     * 
     * @param updObject
     *            The user object which is to be updated
     * @param oldObject
     *            The same user object as it was before the update request.
     */
    private final void update(MCRUser updUser, MCRUser oldUser) throws MCRException {
        // It is important to first check whether *all* groups where the current
        // object is a new member of
        // exist at all. Furthermore it is very important that the current user
        // (session) has the right
        // to update the groups. Hence we must check this, too. If something
        // goes wrong here an exception
        // will be thrown and we do not have to care about a rollback of data
        // already modified. Hence do
        // not combine the following two loops!
        String ID = updUser.getID();

        for (int i = 0; i < updUser.getGroupIDs().size(); i++) {
            String gid = (String) updUser.getGroupIDs().get(i);

            if (!mcrUserStore.existsGroup(gid)) {
                StringBuffer msg = new StringBuffer("You tried to update ");
                msg.append((updUser instanceof MCRUser) ? "user '" : "group '").append(ID).append("' with the unknown group '").append(gid).append("'.");
                throw new MCRException(msg.toString());
            }

            MCRGroup linkedGroup = retrieveGroup(gid);
            linkedGroup.modificationIsAllowed(); // If the modification is
            // not

            // allowed an exception will be
            // thrown.
        }

        // update groups where this User is a new member of
        for (int i = 0; i < updUser.getGroupIDs().size(); i++) {
            MCRGroup updGroup = retrieveGroup((String) updUser.getGroupIDs().get(i));

            if (!oldUser.getGroupIDs().contains(updGroup.getID())) {
                updGroup.addMemberUserID(ID);
                this.updateGroup(updGroup);
            }
        }

        // update groups where this object is no longer a member of
        for (int i = 0; i < oldUser.getGroupIDs().size(); i++) {
            MCRGroup updGroup = retrieveGroup((String) oldUser.getGroupIDs().get(i));

            if (!updUser.getGroupIDs().contains(updGroup.getID())) {
                // The object is no longer member of this group
                updGroup.removeMemberUserID(ID);
                this.updateGroup(updGroup);
            }
        }
    }

    /**
     * This method enables the user.
     * 
     * @param userID
     *            The user object which will be enabled
     * @param flag
     *            the enable/disable flag
     */
    private final void enable(String userID, boolean flag) throws MCRException {
        if (locked) {
            throw new MCRException("The user component is locked. At the moment write access is denied.");
        }

        // Check the permissions
        if (!AI.checkPermission("administrate-user")) {
            throw new MCRException("The session does not have the permission  'administrate-user'!");
        }

        if (userID == null) {
            throw new MCRException("The userID String is null!");
        }

        // check that the user exists
        if (!mcrUserStore.existsUser(userID)) {
            throw new MCRException("You tried to update the unknown user '" + userID + "'.");
        }

        try {
            MCRUser olduser = mcrUserStore.retrieveUser(userID);
            olduser.setEnabled(flag);
            olduser.setModifiedDate();
            userCache.remove(olduser.getID());
            mcrUserStore.updateUser(olduser);
        } catch (MCRException ex) {
            throw new MCRException("Error while updating user " + userID);
        }
    }

    /**
     * The method check that a group entry exists.
     * 
     * @param group
     *            the group name as String
     * @return true if the group exists, else return false
     */
    public final boolean existGroup(String group) {
        if (group == null)
            return false;
        return mcrUserStore.existsGroup(group);
    }

    /**
     * The method check that an user entry exists.
     * 
     * @param group
     *            the user name as String
     * @return true if the user exists, else return false
     */
    public final boolean existUser(String user) {
        if (user == null)
            return false;
        return mcrUserStore.existsUser(user);
    }
}
