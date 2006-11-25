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

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.mycore.common.MCRException;

/**
 * Instances of this class represent MyCoRe groups.
 * <p>
 * The main duty of a group object is to define
 * exactly which members it will have.
 * 
 * @see org.mycore.user2.MCRUserMgr
 * @see org.mycore.user2.MCRUserObject
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Heiko Helmbrecht
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
    }

    /* copy contructor */
    public MCRGroup(MCRGroup other) {
        super();
        this.admUserIDs = new ArrayList(other.admUserIDs);
        this.admGroupIDs = new ArrayList(other.admGroupIDs);
        this.mbrUserIDs = new ArrayList(other.mbrUserIDs);
        this.ID = other.ID;
        this.creator = other.creator;
        this.creationDate = other.creationDate;
        this.modifiedDate = other.modifiedDate;
        this.description = other.description;
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
     */
    public MCRGroup(String ID, String creator, Timestamp creationDate, Timestamp modifiedDate, String description, ArrayList admUserIDs, ArrayList admGroupIDs, ArrayList mbrUserIDs) throws MCRException, Exception {
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
     * This method adds a user (ID) to the users list of the group
     * 
     * @param userID
     *            ID of the user added to the group
     */
    public void addMemberUserID(String userID) throws MCRException {
        addAndUpdate(userID, mbrUserIDs);
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
     * @return This method returns the user list (group members) as a ArrayList
     *         of strings.
     */
    public final ArrayList getMemberUserIDs() {
        return mbrUserIDs;
    }

    /**
     * @return This method returns the ID (user ID or group ID) of the user
     *         object.
     */
    public final String getID() {
        return ID;
    }
    
    public final void setID(String value){
        ID = value;
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
            test = test && (super.creator.length() > 0);
        }

        return test;
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
     * This method removes a user from the users list (members) of the group.
     * 
     * @param userID
     *            ID of the user removed from the group
     */
    public void removeMemberUserID(String userID) throws MCRException {
        removeAndUpdate(userID, mbrUserIDs);
    }

    /**
     * @return This method returns the user or group object as a JDOM document.
     */
    public org.jdom.Document toJDOMDocument() throws MCRException {
        org.jdom.Element root = new org.jdom.Element("mycoregroup");
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.setAttribute("noNamespaceSchemaLocation", "MCRGroup.xsd", XSI_NAMESPACE);
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

        // Aggregate group element
        group.addContent(Creator).addContent(CreationDate).addContent(ModifiedDate).addContent(Description).addContent(admins).addContent(members);

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

        for (int i = 0; i < mbrUserIDs.size(); i++) {
            logger.debug("mbrUserIDs         = " + (String) mbrUserIDs.get(i));
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
        if (AI.checkPermission("modify-group")) {
            return true;
        } else {
        	return false;
        }
    }
}
