/*
 * 
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

package org.mycore.migration21_22.user;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCrypt;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Instances of this class represent MyCoRe users.
 * 
 * @see org.mycore.migration21_22.user.MCRUserObject
 * @see org.mycore.migration21_22.user.MCRUserContact
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
 */
public class MCRUser extends MCRUserObject {
    /** The numerical ID of the MyCoRe user unit (either user ID or group ID) */
    protected int numID = -1;

    /** Specify whether the user ID is enabled or disabled */
    protected boolean idEnabled = false;

    /** Specify whether the user is allowed to update the user object */
    protected boolean updateAllowed = false;

    /** The password of the MyCoRe user */
    protected String passwd = "";

    /** The primary group ID of the user */
    protected String primaryGroupID = "";

    /** Object representing user address information */
    protected MCRUserContact userContact;

    /** A list of groups (IDs) where this user is a member of */
    protected List<String> groupIDs = null;

    /**
     * Default constructor. It is used to create a user object with empty
     * fields. This is useful for constructing an XML representation of a user
     * without specialized data which is used e.g. by MCRUserServlet just to get
     * an XML-representation. The XML representation is used by the
     * XSLT-Stylesheet to create HTML output for the presentation. This empty
     * user object will not be created in the persistent data store.
     */
    public MCRUser() {
        super();
        numID = -1;
        idEnabled = false;
        updateAllowed = false;
        passwd = "";
        primaryGroupID = "";
        groupIDs = new ArrayList<String>();
        userContact = new MCRUserContact();
    }

    /**
     * This minimal constructor only takes the user ID as a parameter. For all
     * other attributes the default constructor is invoked. This constructor is
     * used by the access control system.
     * 
     * @param id
     *            the named user ID
     */
    public MCRUser(String id) {
        this();
        super.ID = trim(id, id_len);
    }

    /**
     * This constructor takes all attributes of this class as single variables.
     * 
     * @param numID
     *            (int) the numerical user ID
     * @param ID
     *            the named user ID
     * @param idEnabled
     *            (boolean) specifies whether the account is disabled or enabled
     * @param updateAllowed
     *            (boolean) specifies whether the user may update his or her
     *            data
     * @param description
     *            description of the user
     * @param passwd
     *            password of the user (encrypted or not encrypted, depending on
     *            property)
     * @param primaryGroupID
     *            the ID of the primary group of the user
     * @param groupIDs
     *            a ArrayList of groups (IDs) the user belongs to
     * @param userContact
     *            contact information
     */
    public MCRUser(int numID, String ID, String creator, Timestamp creationDate, Timestamp modifiedDate, boolean idEnabled, boolean updateAllowed,
        String description, String passwd, String primaryGroupID, List<String> groupIDs, MCRUserContact userContact) {
        // The following data will never be changed by update
        this(ID);
        this.numID = numID;
        super.creator = trim(creator, id_len);

        // check if the creation and modified timestamp is provided. If not, use
        // current timestamp
        super.creationDate = creationDate == null ? new Timestamp(new Date().getTime()) : creationDate;
        super.modifiedDate = modifiedDate == null ? new Timestamp(new Date().getTime()) : modifiedDate;

        this.idEnabled = idEnabled;
        this.updateAllowed = updateAllowed;
        super.description = trim(description, description_len);
        this.passwd = trim(passwd, password_len);
        this.primaryGroupID = trim(primaryGroupID, id_len);
        this.groupIDs = groupIDs;

        this.userContact = userContact == null ? new MCRUserContact() : userContact;
    }

    /**
     * It the passes the given JDOM element to a different constructor and after
     * that encrypts the password if the flag useEncryption is true. This
     * constructor must only be used if a cleartext password is provided in the
     * JDOM element user data.
     * 
     * @param elm
     *            JDOM Element defining a user
     * @param useEncryption
     *            flag to determine if the password has to be encrypted
     */
    public MCRUser(Element elm, boolean useEncryption) {
        this(elm);

        if (useEncryption) {
            passwd = MCRCrypt.crypt(passwd);
        }
    }

    /**
     * This constructor creates the data of this object from a given JDOM
     * element.
     * 
     * @param elm
     *            JDOM Element defining a user
     */
    public MCRUser(Element elm) {
        this();

        if (!elm.getName().equals("user")) {
            return;
        } // Detlev asks: Jens, what is this good for??

        super.ID = trim(elm.getAttributeValue("ID"), id_len);

        String numIDtmp = trim(elm.getAttributeValue("numID"));

        try {
            numID = Integer.parseInt(numIDtmp);
        } catch (Exception e) {
            numID = -1;
        }

        idEnabled = elm.getAttributeValue("id_enabled").equals("true");
        updateAllowed = elm.getAttributeValue("update_allowed").equals("true");
        creator = trim(elm.getChildTextTrim("user.creator"), id_len);
        passwd = trim(elm.getChildTextTrim("user.password"), password_len);

        String tmp = elm.getChildTextTrim("user.creation_date");

        if (tmp != null) {
            try {
                super.creationDate = Timestamp.valueOf(tmp);
            } catch (Exception ignored) {
            }
        }

        tmp = elm.getChildTextTrim("user.last_modified");

        if (tmp != null) {
            try {
                super.modifiedDate = Timestamp.valueOf(tmp);
            } catch (Exception ignored) {
            }
        }

        description = trim(elm.getChildTextTrim("user.description"), description_len);
        primaryGroupID = trim(elm.getChildTextTrim("user.primary_group"), id_len);

        Element contactElement = elm.getChild("user.contact");

        if (contactElement != null) {
            userContact = new MCRUserContact(contactElement);
        }

        Element userGroupElement = elm.getChild("user.groups");

        if (userGroupElement != null) {
            List<Element> groupIDList = userGroupElement.getChildren();

            for (Element groupID : groupIDList) {
                if (!groupID.getTextTrim().equals("")) {
                    groupIDs.add(groupID.getTextTrim());
                }
            }
        }
    }

    /**
     * @return This method returns the contact object of the user
     */
    public MCRUserContact getUserContact() {
        return (MCRUserContact) userContact.clone();
    }

    /**
     * @return This method returns the ID (user ID or group ID) of the user
     *         object.
     */
    @Override
    public final String getID() {
        return ID;
    }

    /**
     * This method removes a group from the groups list of the user object. This
     * list is the list of group IDs where this user or group itself is a member
     * of, not the list of groups this user or group has as members.
     * 
     * @param groupID
     *            ID of the group removed from the user object
     */
    public void removeGroupID(String groupID) throws MCRException {
        // Since this operation is a modification of the group with ID groupID
        // and not of
        // the current group we do not need to check if the modification is
        // allowed.
        if (groupIDs.contains(groupID)) {
            groupIDs.remove(groupID);
        }
    }

    /**
     * This method adds a group to the groups list of the user object. This is
     * the list of group IDs where this user or group itself is a member of, not
     * the list of groups this user or group has as members.
     * 
     * @param groupID
     *            ID of the group added to the user object
     */
    public void addGroupID(String groupID) throws MCRException {
        // Since this operation is a modification of the group with ID groupID
        // and not of
        // the current group we do not need to check if the modification is
        // allowed.
        if (!groupIDs.contains(groupID)) {
            groupIDs.add(groupID);
        }
    }

    /**
     * This method determines the list of all groups the user is a member of,
     * including the implicit ones. That means: if user u is a member of group
     * G1 and G1 is member of group G2 and G2 itself is member of G3, then user
     * u is considered to be an implicit member of groups G2 and G3.
     * 
     * @return list of all groups the user is a member of
     * @deprecated use getGroupIDs instead
     */
    @Deprecated
    public final List<String> getAllGroupIDs() {
        return groupIDs;
    }

    /**
     * @return This method returns the numerical ID of the user object.
     */
    public int getNumID() {
        return numID;
    }

    /**
     * @return This method returns the password of the user.
     */
    public String getPassword() {
        return passwd;
    }

    /**
     * @return This method returns the ID of the primary group of the user.
     */
    public final String getPrimaryGroupID() {
        return primaryGroupID;
    }

    /**
     * @return This method returns true if the user is enabled and may login.
     */
    public boolean isEnabled() {
        return idEnabled;
    }

    /**
     * This method checks if the user is member of a given group.
     * 
     * @param group
     *            Is the user a member of this group?
     * @return Returns true if the user is a member of the given group.
     */
    public boolean isMemberOf(MCRGroup group) {
        return isMemberOf(group.getID());
    }

    /**
     * This method checks if the user is member of a given group.
     * 
     * @param groupID
     *            Is the user a member of this group?
     * @return Returns true if the user is a member of the given group.
     */
    public boolean isMemberOf(String groupID) {
        return getPrimaryGroupID().equals(groupID) || groupIDs.contains(groupID);
    }

    /**
     * @return This method returns true if the user may update his or her data.
     */
    public boolean isUpdateAllowed() {
        return updateAllowed;
    }

    /**
     * This method sets the password of the user.
     * 
     * @param newPassword
     *            The new password of the user
     */
    public boolean setPassword(String newPassword) {
        // We do not allow empty passwords. Later we might check if the password
        // is
        // conform with a password policy.
        if (newPassword == null) {
            return false;
        }

        if (modificationIsAllowed()) {
            if (newPassword.length() != 0) {
                passwd = trim(newPassword, password_len);
                super.modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());

                return true;
            }
        }

        return false;
    }

    /**
     * This method sets the "enabled" attribute to a boolean value.
     * 
     * @param flag
     *            the boolean data
     */
    public final void setEnabled(boolean flag) {
        if (modificationIsAllowed()) {
            idEnabled = flag;
        }
    }

    /**
     * This method updates this instance with the data of the given MCRUser.
     * 
     * @param newuser
     *            the data for the update.
     */
    public final void update(MCRUser newuser) {
        // updateAllowed is an attribute of the user object which determines,
        // whether
        // the user himself may modify his or her data at all.
        if (!updateAllowed) {
            return;
        }

        if (modificationIsAllowed()) { // check if the current user/session may

            // modify the object
            idEnabled = newuser.isEnabled();
            passwd = newuser.getPassword();
            primaryGroupID = newuser.getPrimaryGroupID();
            description = newuser.getDescription();
            groupIDs = newuser.getGroupIDs();
            userContact = newuser.getUserContact();
        }
    }

    /**
     * @return This method returns the user or group object as a JDOM document.
     */
    @Override
    public Document toJDOMDocument() throws MCRException {
        Element root = new Element("mycoreuser");
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.setAttribute("noNamespaceSchemaLocation", "MCRUser.xsd", XSI_NAMESPACE);
        root.addContent(toJDOMElement());

        Document jdomDoc = new Document(root);

        return jdomDoc;
    }

    /**
     * This method returns the user object as a JDOM element. This is needed if
     * one wants to get a representation of several user objects in one xml
     * document.
     * 
     * @return this user data as JDOM element
     */
    @Override
    public Element toJDOMElement() throws MCRException {
        Element user = new Element("user")
            .setAttribute("numID", Integer.toString(numID))
            .setAttribute("ID", ID)
            .setAttribute("id_enabled", idEnabled ? "true" : "false")
            .setAttribute("update_allowed", updateAllowed ? "true" : "false");
        Element Creator = new Element("user.creator").setText(super.creator);
        Element CreationDate = new Element("user.creation_date").setText(super.creationDate.toString());
        Element ModifiedDate = new Element("user.last_modified").setText(super.modifiedDate.toString());
        Element Passwd = new Element("user.password").setText(passwd);
        Element Description = new Element("user.description").setText(super.description);
        Element Primarygroup = new Element("user.primary_group").setText(primaryGroupID);

        // Aggregate user element
        user.addContent(Creator)
            .addContent(CreationDate)
            .addContent(ModifiedDate)
            .addContent(Passwd)
            .addContent(Description)
            .addContent(Primarygroup)
            .addContent(userContact.toJDOMElement());

        // Loop over all group IDs
        if (groupIDs.size() != 0) {
            Element Groups = new Element("user.groups");
            for (String groupID1 : groupIDs) {
                Element groupID = new Element("groups.groupID").setText(groupID1);
                Groups.addContent(groupID);
            }
            user.addContent(Groups);
        }
        return user;
    }

    /**
     * This method writes debug data to the logger (for the debug mode).
     */
    public final void debug() {
        debugDefault();
        logger.debug("primaryGroupID     = " + primaryGroupID);
        logger.debug("groupIDs #         = " + groupIDs.size());
        for (String groupID : groupIDs) {
            logger.debug("groupIDs           = " + groupID);
        }
        userContact.debug();
    }

    /**
     * This private helper method checks if the modification of the user object
     * is allowed for the current user/session.
     */
    public final boolean modificationIsAllowed() throws MCRException {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getUserInformation().getUserID();

        MCRGroup primaryGroup = store.retrieveGroup(primaryGroupID);

        if (MCRAccessManager.checkPermission("modify-user")) {
            return true;
        } else if (ID.equals(currentUserID) || creator.equals(currentUserID)) {
            return true;
        } else if (primaryGroup.getAdminUserIDs().contains(currentUserID)) {
            return true;
        } else { // check if the current user is (direct, not implicit)
            // member

            // of one of the admGroups
            ArrayList<String> admGroupIDs = primaryGroup.getAdminGroupIDs();

            for (String admGroupID : admGroupIDs) {
                MCRGroup currentGroup = store.retrieveGroup(admGroupID);

                if (currentGroup.getMemberUserIDs().contains(currentUserID)) {
                    return true;
                }
            }
        }

        throw new MCRException("The current user " + currentUserID + " has no right to modify the user " + ID + ".");
    }

    /**
     * @return This method returns the list of groups as a ArrayList of strings.
     *         These are the groups where the object itself is a member of.
     */
    public final List<String> getGroupIDs() {
        return groupIDs;
    }

    public final int getGroupCount() {
        try {
            return groupIDs.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * @see #getID()
     */
    @Override
    public String toString() {
        return getID();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MCRUser)) {
            return false;
        }
        MCRUser u = (MCRUser) obj;
        if (this == u) {
            return true;
        }
        // acording to the hashCode() contract
        return hashCode() == u.hashCode() && fastEquals(u);
    }

    private boolean fastEquals(MCRUser u) {
        return getID().equals(u.getID()) && getUserContact().equals(u.getUserContact());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + getID().hashCode();
        result = 37 * result + getUserContact().hashCode();
        return result;
    }
}
