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

package org.mycore.user;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Instances of this class represent MyCoRe groups.
 * <p>
 * In the MyCoRe user component the privileges of a user (e.g. the privilege i
 * to create a new user or a new group, to delete users etc.) are determined by
 * the membership to a group. Hence the main duty of a group object is to define
 * exactly which privileges the members will have.
 * 
 * @see org.mycore.user.MCRUserMgr
 * @see org.mycore.user.MCRUserObject
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRGroup extends MCRUserObject implements MCRPrincipal {
    /** A list of users which have the privilege to administer this group */
    private ArrayList admUserIDs = null;

    /**
     * A list of groups which members have the privilege to administer this
     * group
     */
    private ArrayList admGroupIDs = null;

    /** A list of users (IDs) which are members of this group */
    private ArrayList mbrUserIDs = null;

    /** A list of other groups (IDs) which are members of this group */
    private ArrayList mbrGroupIDs = null;

    /** A list of privileges members of this group have */
    private ArrayList privileges = null;

    /**
     * Default constructor. It is used to create a group object with empty
     * fields. This is useful for constructing an XML representation of a group
     * without specialized data. This empty group object will not be created in
     * the persistent data store.
     */
    public MCRGroup() {
        super();
        admUserIDs = new ArrayList();
        admGroupIDs = new ArrayList();
        mbrUserIDs = new ArrayList();
        mbrGroupIDs = new ArrayList();
        privileges = new ArrayList();
    }

    /* copy contructor */
    public MCRGroup(MCRGroup other) {
        super();
        this.admUserIDs = new ArrayList(other.admUserIDs);
        this.admGroupIDs = new ArrayList(other.admGroupIDs);
        this.mbrUserIDs = new ArrayList(other.mbrUserIDs);
        this.mbrGroupIDs = new ArrayList(other.mbrGroupIDs);
        this.privileges = new ArrayList(other.privileges);
        this.ID = other.ID;
        this.creator = other.creator;
        this.creationDate = other.creationDate;
        this.modifiedDate = other.modifiedDate;
        this.description = other.description;
        this.groupIDs = new ArrayList(other.groupIDs);
    }

    /**
     * This minimal constructor only takes the group ID as a parameter. For all
     * other attributes the default constructor is invoked.
     */
    public MCRGroup(String id) {
        // This constructor is used by the access control system
        super.ID = id.trim();
    }

    /**
     * This constructor takes a subset of attributes of this class as single
     * variables and calls the main constructor (taking all attributes) with
     * default values for the remaining attribute (parameter 'create').
     * 
     * @param ID
     *            the group ID
     * @param creator
     *            the user ID who created this group
     * @param creationDate
     *            timestamp of the creation of this group, if null the current
     *            date will be used
     * @param modifiedDate
     *            timestamp of the last modification of this group
     * @param description
     *            description of the group
     * @param admUserIDs
     *            ArrayList of user IDs which have administrative rights for the
     *            group
     * @param admGroupIDs
     *            ArrayList of groups which members have administrative rights
     *            for the group
     * @param mbrUserIDs
     *            ArrayList of user IDs this group has as members
     * @param mbrGroupIDs
     *            ArrayList of group IDs this group has as members
     * @param groupIDs
     *            ArrayList of group IDs this group is a member of
     * @param privileges
     *            ArrayList of privileges members of this group have
     */
    public MCRGroup(String ID, String creator, Timestamp creationDate, Timestamp modifiedDate, String description, ArrayList admUserIDs, ArrayList admGroupIDs, ArrayList mbrUserIDs, ArrayList mbrGroupIDs, ArrayList groupIDs, ArrayList privileges) throws MCRException, Exception {
        super.ID = trim(ID, id_len);
        super.creator = trim(creator, id_len);

        // check if the creation timestamp is provided. If not, use current
        // timestamp
        if (creationDate == null) {
            super.creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());
        } else {
            super.creationDate = creationDate;
        }

        if (modifiedDate == null) {
            super.modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());
        } else {
            super.modifiedDate = modifiedDate;
        }

        this.description = trim(description, description_len);
        this.admUserIDs = new ArrayList();

        if (admUserIDs != null) {
            this.admUserIDs = admUserIDs;
        }

        this.admGroupIDs = new ArrayList();

        if (admGroupIDs != null) {
            this.admGroupIDs = admGroupIDs;
        }

        this.mbrUserIDs = new ArrayList();

        if (mbrUserIDs != null) {
            this.mbrUserIDs = mbrUserIDs;
        }

        this.mbrGroupIDs = new ArrayList();

        if (mbrGroupIDs != null) {
            this.mbrGroupIDs = mbrGroupIDs;
        }

        this.groupIDs = new ArrayList();

        if (groupIDs != null) {
            this.groupIDs = groupIDs;
        }

        this.privileges = new ArrayList();

        if (privileges != null) {
            this.privileges = privileges;
        }
    }

    /**
     * This constructor creates the data of this object from a given JDOM
     * Element.
     * 
     * @param the
     *            JDOM Element
     */
    public MCRGroup(org.jdom.Element elm) {
        this();

        if (!elm.getName().equals("group")) {
            return;
        }

        super.ID = trim(elm.getAttributeValue("ID"), id_len);
        this.creator = trim(elm.getChildTextTrim("group.creator"), id_len);

        String tmp = elm.getChildTextTrim("group.creation_date");

        if (tmp != null) {
            try {
                super.creationDate = Timestamp.valueOf(tmp);
            } catch (Exception e) {
            }
        }

        tmp = elm.getChildTextTrim("group.last_modified");

        if (tmp != null) {
            try {
                super.modifiedDate = Timestamp.valueOf(tmp);
            } catch (Exception e) {
            }
        }

        this.description = trim(elm.getChildTextTrim("group.description"), description_len);

        org.jdom.Element adminElement = elm.getChild("group.admins");

        if (adminElement != null) {
            List adminIDList = adminElement.getChildren();

            for (int j = 0; j < adminIDList.size(); j++) {
                org.jdom.Element newID = (org.jdom.Element) adminIDList.get(j);
                String id = trim(newID.getTextTrim(), id_len);

                if (newID.getName().equals("admins.userID")) {
                    if (!id.equals("")) {
                        addAdminUserID(id);
                    }

                    continue;
                }

                if (newID.getName().equals("admins.groupID")) {
                    if (!id.equals("")) {
                        addAdminGroupID(id);
                    }
                }
            }
        }

        org.jdom.Element memberElement = elm.getChild("group.members");

        if (memberElement != null) {
            List memberIDList = memberElement.getChildren();

            for (int j = 0; j < memberIDList.size(); j++) {
                org.jdom.Element newID = (org.jdom.Element) memberIDList.get(j);
                String id = trim(newID.getTextTrim(), id_len);

                if (newID.getName().equals("members.userID")) {
                    if (!id.equals("")) {
                        addMemberUserID(id);
                    }

                    continue;
                }

                if (newID.getName().equals("members.groupID")) {
                    if (!id.equals("")) {
                        addMemberGroupID(id);
                    }
                }
            }
        }

        org.jdom.Element userGroupElement = elm.getChild("group.groups");

        if (userGroupElement != null) {
            List groupIDList = userGroupElement.getChildren();

            for (int j = 0; j < groupIDList.size(); j++) {
                org.jdom.Element groupID = (org.jdom.Element) groupIDList.get(j);
                String id = trim(groupID.getTextTrim(), id_len);

                if (!id.equals("")) {
                    this.groupIDs.add(id);
                }
            }
        }

        org.jdom.Element privilegeElement = elm.getChild("group.privileges");

        if (privilegeElement != null) {
            List privilegeIDList = privilegeElement.getChildren();

            for (int j = 0; j < privilegeIDList.size(); j++) {
                org.jdom.Element privilegeID = (org.jdom.Element) privilegeIDList.get(j);
                String priv = trim(privilegeID.getTextTrim(), privilege_len);

                if (!priv.equals("")) {
                    this.privileges.add(priv);
                }
            }
        }
    }

    /**
     * This method adds a group to the list of groups with administrative
     * privileges for the group.
     * 
     * @param groupID
     *            ID of the group added to the group admin list
     */
    public void addAdminGroupID(String groupID) throws MCRException {
        addAndUpdate(groupID, admGroupIDs);
    }

    /**
     * This method adds a user (ID) to the administrators list of the group
     * 
     * @param userID
     *            ID of the administrative user added to the group
     */
    public void addAdminUserID(String userID) throws MCRException {
        addAndUpdate(userID, admUserIDs);
    }

    /**
     * This method adds a group to the list of member groups of the group. Do
     * not confuse with the list of groups the group itself is a member of.
     * 
     * @param groupID
     *            ID of the group added to the group member list
     */
    public void addMemberGroupID(String groupID) throws MCRException {
        addAndUpdate(groupID, mbrGroupIDs);
    }

    /**
     * This method adds a user (ID) to the users list of the group
     * 
     * @param userID
     *            ID of the user added to the group
     */
    public void addMemberUserID(String userID) throws MCRException {
        addAndUpdate(userID, mbrUserIDs);
    }

    /**
     * This method adds a privilege to the privileges list of the group
     * 
     * @param privName
     *            Name of the privilege added to the group
     */
    public void addPrivilege(String privName) throws MCRException {
        addAndUpdate(privName, privileges);
    }

    /**
     * @return This method returns the list of admin groups as a ArrayList of
     *         strings.
     */
    public final ArrayList getAdminGroupIDs() {
        return admGroupIDs;
    }

    /**
     * @return This method returns the list of admin users as a ArrayList of
     *         strings.
     */
    public final ArrayList getAdminUserIDs() {
        return admUserIDs;
    }

    /**
     * @return This method returns the list of group members (groups) as a
     *         ArrayList of strings. Do not confuse with the list of groups the
     *         group itself is a member of.
     */
    public final ArrayList getMemberGroupIDs() {
        return mbrGroupIDs;
    }

    /**
     * @return This method returns the user list (group members) as a ArrayList
     *         of strings.
     */
    public final ArrayList getMemberUserIDs() {
        return mbrUserIDs;
    }

    /**
     * @return This method returns the list of privileges as a ArrayList of
     *         strings.
     */
    public final ArrayList getPrivileges() {
        return privileges;
    }

    /**
     * This method return all privileges of this group and of groups in which
     * this group is member as an ArrayList.
     * 
     * @return a ArrayList of all privileges, that this group holds
     */
    public final ArrayList getAllPrivileges() {
        ArrayList ar = new ArrayList();

        for (int i = 0; i < privileges.size(); i++) {
            ar.add(privileges.get(i));
        }

        for (int i = 0; i < groupIDs.size(); i++) {
            MCRGroup nextGroup = MCRUserMgr.instance().retrieveGroup((String) groupIDs.get(i), false);
            ar.addAll(nextGroup.getAllPrivileges());
        }

        return ar;
    }

    /**
     * @return This method returns the ID (user ID or group ID) of the user
     *         object.
     */
    public final String getID() {
        return ID;
    }

    /**
     * This method checks if members of this group have a given privilege. Not
     * only the privileges of this group will be tested but also the privileges
     * of the groups where this group is a member of (recursivley).
     * 
     * @return returns true if the given privilege is in the list of privileges
     *         of this group or implicitly in one of the groups where this group
     *         is a member of.
     */
    public boolean hasPrivilege(String privilege) throws MCRException {
        if (privileges.contains(privilege)) {
            return true;
        }

        for (int i = 0; i < groupIDs.size(); i++) {
            MCRGroup nextGroup = MCRUserMgr.instance().retrieveGroup((String) groupIDs.get(i), false);

            if (nextGroup.hasPrivilege(privilege)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This method checks if a user is a member of this group.
     * 
     * @param user
     *            Is this user a member of the group?
     * @return Returns true if the given user is a member of this group.
     */
    public boolean hasUserMember(MCRUser user) {
        if ((admUserIDs.contains(user.getID())) || (mbrUserIDs.contains(user.getID()))) {
            return true;
        }

        return false;
    }

    /**
     * This method checks if a user is a member of this group.
     * 
     * @param user
     *            Is this user a member of the group?
     * @return Returns true if the given user is a member of this group.
     */
    public boolean hasUserMember(String user) {
        if ((admUserIDs.contains(user)) || (mbrUserIDs.contains(user))) {
            return true;
        }

        return false;
    }

    /**
     * This method checks if a group is a member of this group.
     * 
     * @param group
     *            Is this group a member of the group?
     * @return Returns true if the given group is a member of this group.
     */
    public boolean hasGroupMember(MCRGroup group) {
        if ((admGroupIDs.contains(group.getID())) || (mbrGroupIDs.contains(group.getID()))) {
            return true;
        }

        return false;
    }

    /**
     * This method checks if a group is a member of this group.
     * 
     * @param group
     *            Is this group a member of the group?
     * @return Returns true if the given group is a member of this group.
     */
    public boolean hasGroupMember(String group) {
        if ((admGroupIDs.contains(group)) || (mbrGroupIDs.contains(group))) {
            return true;
        }

        return false;
    }

    /**
     * This method checks whether a group implicitly is a member of a given
     * group. It is a recursive method.
     * 
     * @param group
     *            The group to be checked
     * @param matchID
     *            ID of the group to check if 'group' is a member of it
     * @return returns true if 'group' is an implicit member of the group with
     *         ID 'matchID'.
     */
    public static boolean isImplicitMemberOf(MCRGroup group, String matchID) throws MCRException {
        MCRGroup matchGroup = MCRUserMgr.instance().retrieveGroup(matchID, true);
        ArrayList matchMemberGroupIDs = matchGroup.getMemberGroupIDs();

        if (matchMemberGroupIDs.contains(group.getID())) {
            return true;
        }

        for (int i = 0; i < matchMemberGroupIDs.size(); i++) {
            if (MCRGroup.isImplicitMemberOf(group, (String) matchMemberGroupIDs.get(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * This method checks whether this group is a member of a given group. It
     * not only considers the groups list of the given group but recursively
     * checks the group lists of all groups the given group is a member of.
     * 
     * @param groupID
     *            ID of the group to check if this group is a member of
     * @return returns true if the group is an implicit member of the given
     *         group
     */
    public boolean isMemberOf(String groupID) throws MCRException {
        if (super.groupIDs.contains(groupID)) {
            return true;
        }

        return MCRGroup.isImplicitMemberOf(this, groupID);
    }

    /**
     * This method checks if all required fields have been provided. In a later
     * stage of the software development a User Policy object will be asked,
     * which fields exactly are the required fields. This will be configurable.
     * 
     * @return returns true if all required fields have been provided
     */
    public boolean isValid() throws MCRException {
        ArrayList requiredGroupAttributes = MCRUserPolicy.instance().getRequiredGroupAttributes();
        boolean test = true;

        if (requiredGroupAttributes.contains("groupID")) {
            test = test && (super.ID.length() > 0);
        }

        if (requiredGroupAttributes.contains("creator")) {
            test = test && (super.ID.length() > 0);
        }

        return test;
    }

    /**
     * This method cleans the list of member groups.
     */
    protected final void cleanMemberGroupID() throws MCRException {
        if (modificationIsAllowed()) {
            mbrGroupIDs.clear();
        }
    }

    /**
     * This method cleans the list of member users.
     */
    protected final void cleanMemberUserID() throws MCRException {
        if (modificationIsAllowed()) {
            mbrUserIDs.clear();
        }
    }

    /**
     * This method removes a group from the list of groups with administrative
     * privileges for this group.
     * 
     * @param groupID
     *            ID of the administrative group removed from the group
     */
    public void removeAdminGroupID(String groupID) throws MCRException {
        removeAndUpdate(groupID, admGroupIDs);
    }

    /**
     * This method removes a user from the list of administrators of the group.
     * 
     * @param userID
     *            ID of the administrative user removed from the group
     */
    public void removeAdminUserID(String userID) throws MCRException {
        removeAndUpdate(userID, admUserIDs);
    }

    /**
     * This method removes a group from the list of group members (groups). Do
     * not confuse with the list of groups the group itself is a member of.
     * 
     * @param groupID
     *            ID of the group removed from the group
     */
    public void removeMemberGroupID(String groupID) throws MCRException {
        removeAndUpdate(groupID, mbrGroupIDs);
    }

    /**
     * This method removes a user from the users list (members) of the group.
     * 
     * @param userID
     *            ID of the user removed from the group
     */
    public void removeMemberUserID(String userID) throws MCRException {
        removeAndUpdate(userID, mbrUserIDs);
    }

    /**
     * This method removes a privilege from the privileges list of the group.
     * 
     * @param privName
     *            Name of the privilege removed from the group
     */
    public void removePrivilege(String privName) throws MCRException {
        removeAndUpdate(privName, privileges);
    }

    /**
     * @return This method returns the user or group object as a JDOM document.
     */
    public org.jdom.Document toJDOMDocument() throws MCRException {
        org.jdom.Element root = new org.jdom.Element("mycoregroup");
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        root.setAttribute("noNamespaceSchemaLocation", "MCRGroup.xsd", org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        root.addContent(this.toJDOMElement());

        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        return jdomDoc;
    }

    /**
     * @return This method returns the user or group object as a JDOM element.
     *         This is needed if one wants to get a representation of several
     *         user or group objects in one xml document.
     */
    public org.jdom.Element toJDOMElement() {
        org.jdom.Element group = new org.jdom.Element("group").setAttribute("ID", ID);
        org.jdom.Element Creator = new org.jdom.Element("group.creator").setText(super.creator);
        org.jdom.Element CreationDate = new org.jdom.Element("group.creation_date").setText(super.creationDate.toString());
        org.jdom.Element ModifiedDate = new org.jdom.Element("group.last_modified").setText(super.modifiedDate.toString());
        org.jdom.Element Description = new org.jdom.Element("group.description").setText(super.description);
        org.jdom.Element admins = new org.jdom.Element("group.admins");
        org.jdom.Element members = new org.jdom.Element("group.members");
        org.jdom.Element groups = new org.jdom.Element("group.groups");
        org.jdom.Element Privileges = new org.jdom.Element("group.privileges");

        // Loop over all admin user IDs
        for (int i = 0; i < admUserIDs.size(); i++) {
            org.jdom.Element admUserID = new org.jdom.Element("admins.userID").setText((String) admUserIDs.get(i));
            admins.addContent(admUserID);
        }

        // Loop over all admin group IDs
        for (int i = 0; i < admGroupIDs.size(); i++) {
            org.jdom.Element admGroupID = new org.jdom.Element("admins.groupID").setText((String) admGroupIDs.get(i));
            admins.addContent(admGroupID);
        }

        // Loop over all user IDs (members of this group!)
        for (int i = 0; i < mbrUserIDs.size(); i++) {
            org.jdom.Element mbrUserID = new org.jdom.Element("members.userID").setText((String) mbrUserIDs.get(i));
            members.addContent(mbrUserID);
        }

        // Loop over all group IDs (members of this group!)
        for (int i = 0; i < mbrGroupIDs.size(); i++) {
            org.jdom.Element mbrGroupID = new org.jdom.Element("members.groupID").setText((String) mbrGroupIDs.get(i));
            members.addContent(mbrGroupID);
        }

        // Loop over all group IDs (where this group is a member of!)
        for (int i = 0; i < groupIDs.size(); i++) {
            org.jdom.Element groupID = new org.jdom.Element("groups.groupID").setText((String) groupIDs.get(i));
            groups.addContent(groupID);
        }

        // Loop over all privileges
        for (int i = 0; i < privileges.size(); i++) {
            org.jdom.Element priv = new org.jdom.Element("privileges.privilege").setText((String) privileges.get(i));
            Privileges.addContent(priv);
        }

        // Aggregate group element
        group.addContent(Creator).addContent(CreationDate).addContent(ModifiedDate).addContent(Description).addContent(admins).addContent(members).addContent(groups).addContent(Privileges);

        return group;
    }

    /**
     * This method writes debug data to the logger (for the debug mode).
     */
    public final void debug() {
        debugDefault();

        for (int i = 0; i < admGroupIDs.size(); i++) {
            logger.debug("admGroupIDs        = " + (String) admGroupIDs.get(i));
        }

        for (int i = 0; i < admUserIDs.size(); i++) {
            logger.debug("admUserIDs         = " + (String) admUserIDs.get(i));
        }

        for (int i = 0; i < mbrGroupIDs.size(); i++) {
            logger.debug("mbrGroupIDs        = " + (String) mbrGroupIDs.get(i));
        }

        for (int i = 0; i < mbrUserIDs.size(); i++) {
            logger.debug("mbrUserIDs         = " + (String) mbrUserIDs.get(i));
        }

        for (int i = 0; i < privileges.size(); i++) {
            logger.debug("privileges         = " + (String) privileges.get(i));
        }
    }

    /**
     * This private helper method adds values to a given vector. It is used by
     * addGroupID etc.
     * 
     * @param s
     *            String to be added to the vector vec
     * @param vec
     *            ArrayList to which the string s will be added to
     */
    private void addAndUpdate(String s, ArrayList vec) throws MCRException {
        if (modificationIsAllowed()) {
            if (!vec.contains(s)) {
                vec.add(s);
            }
        }
    }

    /**
     * This private helper method removes values from a given vector. It is used
     * by removeGroupID etc.
     * 
     * @param s
     *            String to be removed from the vector vec
     * @param vec
     *            ArrayList from which the string s will be removed from
     */
    private void removeAndUpdate(String s, ArrayList vec) throws MCRException {
        if (modificationIsAllowed()) {
            if (vec.contains(s)) {
                vec.remove(s);
            }
        }
    }

    /**
     * This private helper method checks if the modification of the group object
     * is allowed for the current user/session.
     */
    public final boolean modificationIsAllowed() throws MCRException {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getCurrentUserID();

        MCRUser currentUser = MCRUserMgr.instance().retrieveUser(currentUserID, false);

        if (currentUser.hasPrivilege("user administrator")) {
            return true;
        } else if (this.creator.equals(currentUserID)) {
            return true;
        } else if (admUserIDs.contains(currentUserID)) {
            return true;
        } else { // check if the current user is (direct, not implicit)
                    // member

            // of one of the admGroups
            for (int i = 0; i < admGroupIDs.size(); i++) {
                MCRGroup currentGroup = MCRUserMgr.instance().retrieveGroup((String) admGroupIDs.get(i), false);

                if (currentGroup.getMemberUserIDs().contains(currentUserID)) {
                    return true;
                }
            }
        }

        throw new MCRException("The current user " + currentUserID + " has no right to modify the group " + this.ID + ".");
    }
}
