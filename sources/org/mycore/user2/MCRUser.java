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

import java.security.Principal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Instances of this class represent MyCoRe users.
 * 
 * @see org.mycore.user2.MCRUserMgr
 * @see org.mycore.user2.MCRUserObject
 * @see org.mycore.user2.MCRUserContact
 * @see org.mycore.user2.MCRUserMgr
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
 */
public class MCRUser extends MCRUserObject implements MCRPrincipal, Principal {
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
    protected ArrayList groupIDs = null;    

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
        groupIDs = new ArrayList();
        userContact = new MCRUserContact();
    }



    /**
     * This minimal constructor only takes the user ID as a parameter. For all
     * other attributes the default constructor is invoked. This constructor is
     * used by the access control system.
     * 
     * @param ID
     *            the named user ID
     */
    public MCRUser(String id) {
        super.ID = id.trim();
    }

    /**
     * This constructor takes all attributes of this class as single variables.
     * 
     * @param numID
     *            (int) the numerical user ID
     * @param ID
     *            the named user ID
     * @creator the creator name
     * @creationDate the timestamp of creation
     * @modifiedDate the timestamp of modification
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
     * @param salutation
     *            contact information
     * @param firstname
     *            contact information
     * @param lastname
     *            contact information
     * @param street
     *            contact information
     * @param city
     *            contact information
     * @param postalcode
     *            contact information
     * @param country
     *            contact information
     * @param state
     *            contact information
     * @param institution
     *            contact information
     * @param faculty
     *            contact information
     * @param department
     *            contact information
     * @param institute
     *            contact information
     * @param telephone
     *            telephone number
     * @param fax
     *            fax number
     * @param email
     *            email address
     * @param cellphone
     *            number of cellular phone, if available
     */
    public MCRUser(int numID, String ID, String creator, Timestamp creationDate, Timestamp modifiedDate, boolean idEnabled, boolean updateAllowed, String description, String passwd, String primaryGroupID, ArrayList groupIDs, String salutation, String firstname, String lastname, String street, String city, String postalcode, String country, String state, String institution, String faculty,
            String department, String institute, String telephone, String fax, String email, String cellphone) throws MCRException, Exception {
        // The following data will never be changed by update
        super.ID = trim(ID, id_len);
        this.numID = numID;
        super.creator = trim(creator, id_len);

        // check if the creation and modified timestamp is provided. If not, use
        // current timestamp
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

        this.idEnabled = idEnabled;
        this.updateAllowed = updateAllowed;
        super.description = trim(description, description_len);
        this.passwd = trim(passwd, password_len);
        this.primaryGroupID = trim(primaryGroupID, id_len);
        this.groupIDs = groupIDs;

        this.userContact = new MCRUserContact(salutation, firstname, lastname, street, city, postalcode, country, state, institution, faculty, department, institute, telephone, fax, email, cellphone);
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
    public MCRUser(org.jdom.Element elm, boolean useEncryption) {
        this(elm);

        if (useEncryption) {
            String cryptPwd = MCRCrypt.crypt(this.passwd);
            this.passwd = cryptPwd;
        }
    }

    /**
     * This constructor creates the data of this object from a given JDOM
     * element.
     * 
     * @param elm
     *            JDOM Element defining a user
     */
    public MCRUser(org.jdom.Element elm) {
        this();

        if (!elm.getName().equals("user")) {
            return;
        } // Detlev asks: Jens, what is this good for??

        super.ID = trim(elm.getAttributeValue("ID"), id_len);

        String numIDtmp = trim(elm.getAttributeValue("numID"));

        try {
            this.numID = Integer.parseInt(numIDtmp);
        } catch (Exception e) {
            this.numID = -1;
        }

        this.idEnabled = (elm.getAttributeValue("id_enabled").equals("true")) ? true : false;
        this.updateAllowed = (elm.getAttributeValue("update_allowed").equals("true")) ? true : false;
        this.creator = trim(elm.getChildTextTrim("user.creator"), id_len);
        this.passwd = trim(elm.getChildTextTrim("user.password"), password_len);

        String tmp = elm.getChildTextTrim("user.creation_date");

        if (tmp != null) {
            try {
                super.creationDate = Timestamp.valueOf(tmp);
            } catch (Exception e) {
            }
        }

        tmp = elm.getChildTextTrim("user.last_modified");

        if (tmp != null) {
            try {
                super.modifiedDate = Timestamp.valueOf(tmp);
            } catch (Exception e) {
            }
        }

        this.description = trim(elm.getChildTextTrim("user.description"), description_len);
        this.primaryGroupID = trim(elm.getChildTextTrim("user.primary_group"), id_len);

        org.jdom.Element contactElement = elm.getChild("user.contact");

        if (contactElement != null) {
            userContact = new MCRUserContact(contactElement);
        }

        org.jdom.Element userGroupElement = elm.getChild("user.groups");

        if (userGroupElement != null) {
            List groupIDList = userGroupElement.getChildren();

            for (int j = 0; j < groupIDList.size(); j++) {
                org.jdom.Element groupID = (org.jdom.Element) groupIDList.get(j);

                if (!(groupID.getTextTrim()).equals("")) {
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
     */
    public final ArrayList getAllGroupIDs() {
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
     * This method checks if the user is authenticated. It does so by querying
     * the user manager. This information is needed to assign categories in the
     * access control subsystem.
     * 
     * @return returns true if the user is authenticated
     */
    public final boolean isAuthenticated() {
        return MCRUserMgr.isAuthenticated(this);
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
        if (groupIDs.contains(group.getID())) {
            return true;
        }

        return false;
    }

    /**
     * @return This method returns true if the user may update his or her data.
     */
    public boolean isUpdateAllowed() {
        return updateAllowed;
    }

    /**
     * This method checks if all required fields have been provided. In a later
     * stage of the software development a User Policy object will be asked,
     * which fields exactly are the required fields. This will be configurable.
     * 
     * @return returns true if all required fields have been provided
     */
    public boolean isValid() {
        ArrayList requiredUserAttributes = MCRUserPolicy.instance().getRequiredUserAttributes();
        boolean test = true;

        if (requiredUserAttributes.contains("userID")) {
            test = test && (super.ID.length() > 0);
        }

        if (requiredUserAttributes.contains("numID")) {
            test = test && (this.numID >= 0);
        }

        if (requiredUserAttributes.contains("creator")) {
            test = test && (super.ID.length() > 0);
        }

        if (requiredUserAttributes.contains("password")) {
            test = test && (this.passwd.length() > 0);
        }

        if (requiredUserAttributes.contains("primary_group")) {
            test = test && (this.primaryGroupID.length() > 0);
        }

        return test;
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
    public org.jdom.Document toJDOMDocument() throws MCRException {
        org.jdom.Element root = new org.jdom.Element("mycoreuser");
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.setAttribute("noNamespaceSchemaLocation", "MCRUser.xsd", XSI_NAMESPACE);
        root.addContent(this.toJDOMElement());

        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        return jdomDoc;
    }

    /**
     * This method returns the user object as a JDOM element. This is needed if
     * one wants to get a representation of several user objects in one xml
     * document.
     * 
     * @return this user data as JDOM element
     */
    public org.jdom.Element toJDOMElement() throws MCRException {
        org.jdom.Element user = new org.jdom.Element("user").setAttribute("numID", Integer.toString(numID)).setAttribute("ID", ID).setAttribute("id_enabled", (idEnabled) ? "true" : "false").setAttribute("update_allowed", (updateAllowed) ? "true" : "false");
        org.jdom.Element Creator = new org.jdom.Element("user.creator").setText(super.creator);
        org.jdom.Element CreationDate = new org.jdom.Element("user.creation_date").setText(super.creationDate.toString());
        org.jdom.Element ModifiedDate = new org.jdom.Element("user.last_modified").setText(super.modifiedDate.toString());
        org.jdom.Element Passwd = new org.jdom.Element("user.password").setText(passwd);
        org.jdom.Element Description = new org.jdom.Element("user.description").setText(super.description);
        org.jdom.Element Primarygroup = new org.jdom.Element("user.primary_group").setText(primaryGroupID);

        // Loop over all group IDs
        org.jdom.Element Groups = new org.jdom.Element("user.groups");

        for (int i = 0; i < groupIDs.size(); i++) {
            org.jdom.Element groupID = new org.jdom.Element("groups.groupID").setText((String) groupIDs.get(i));
            Groups.addContent(groupID);
        }

        // Aggregate user element
        user.addContent(Creator).addContent(CreationDate).addContent(ModifiedDate).addContent(Passwd).addContent(Description).addContent(Primarygroup).addContent(userContact.toJDOMElement()).addContent(Groups);

        return user;
    }

    /**
     * This method writes debug data to the logger (for the debug mode).
     */
    public final void debug() {
        debugDefault();
        logger.debug("primaryGroupID     = " + primaryGroupID);
        logger.debug("groupIDs #         = " + groupIDs.size());
        for (int i = 0; i < groupIDs.size(); i++) {
            logger.debug("groupIDs           = " + ((String) groupIDs.get(i)));
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
        String currentUserID = mcrSession.getCurrentUserID();

        MCRGroup primaryGroup = MCRUserMgr.instance().retrieveGroup(primaryGroupID, false);

        if (AI.checkPermission("modify-user")) {
            return true;
        } else if (this.ID.equals(currentUserID) || this.creator.equals(currentUserID)) {
            return true;
        } else if (primaryGroup.getAdminUserIDs().contains(currentUserID)) {
            return true;
        } else { // check if the current user is (direct, not implicit)
                    // member

            // of one of the admGroups
            ArrayList admGroupIDs = primaryGroup.getAdminGroupIDs();

            for (int i = 0; i < admGroupIDs.size(); i++) {
                MCRGroup currentGroup = MCRUserMgr.instance().retrieveGroup((String) admGroupIDs.get(i), false);

                if (currentGroup.getMemberUserIDs().contains(mcrSession.getCurrentUserID())) {
                    return true;
                }
            }
        }

        throw new MCRException("The current user " + currentUserID + " has no right to modify the user " + this.ID + ".");
    }
    
    /**
     * @return This method returns the list of groups as a ArrayList of strings.
     *         These are the groups where the object itself is a member of.
     */
    public final ArrayList getGroupIDs() {
        return groupIDs;
    }    
    
    public final int getGroupCount(){
           try{
               return groupIDs.size();
           }catch(Exception e){
               return 0;
           }
    }

    /**
     * @see #getID()
     */
    public String getName() {
        return getID();
    }
    
    /**
     * @see #getID()
     */
    public String toString() {
        return getID();
    }
    
    public boolean equals(Object obj){
        if (!(obj instanceof MCRUser)){
            return false;
        }
        MCRUser u=(MCRUser)obj;
        if (this==u){
            return true;
        }
        if (this.hashCode()!=this.hashCode()){
            //acording to the hashCode() contract
            return false;
        }
        return fastEquals(u);
    }
    
    private boolean fastEquals(MCRUser u){
        return (
                ((this.getID()==u.getID()) || (this.getID().equals(u.getID()))) &&
                ((this.getUserContact()==u.getUserContact()) || (this.getUserContact().equals(u.getUserContact())))
               );
    }
    
    public int hashCode() {
        int result=17;
        result=37*result+getID().hashCode();
        result=37*result+getUserContact().hashCode();
        return result;
    }
}
