/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.user;

import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.Vector;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.*;

/**
 * Instances of this class represent MyCoRe groups.
 * <p>
 * In the MyCoRe user component the privileges of a user (e.g. the privilege to create
 * a new user or a new group, to delete users etc.) are determined by the membership
 * to a group. Hence the main duty of a group object is to define exactly which privileges
 * the members will have.
 *
 * @see org.mycore.user.MCRUserMgr
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRGroup extends MCRUserObject
{
  /** A list of users which have the privilege to administer this group */
  private Vector admUserIDs = null;

  /** A list of groups which members have the privilege to administer this group */
  private Vector admGroupIDs = null;

  /** A list of users (IDs) which are members of this group */
  private Vector mbrUserIDs = null;

  /** A list of other groups (IDs) which are members of this group */
  private Vector mbrGroupIDs = null;

  /** A list of privileges members of this group have */
  private Vector privileges = null;

  /**
   * Default constructor. It is used to create a group object with empty fields. This is
   * useful for constructing an XML representation of a group without specialized data.
   * This empty group object will not be created in the persistent data store.
   */
  public MCRGroup() throws Exception
  { this("", "", null, null, "", null, null, null, null, null, null, ""); }

  /**
   * This constructor takes a subset of attributes of this class as single variables and
   * calls the main constructor (taking all attributes) with default values for the
   * remaining attribute (parameter 'create').
   *
   * @param ID             the group ID
   * @param creator        the user ID who created this user
   * @param creationDate   timestamp of the creation of this user, if null the current date will be used
   * @param modifiedDate   timestamp of the last modification of this user
   * @param description    description of the group
   * @param admUserIDs     a Vector of user IDs which have administrative rights for the group
   * @param admGroupIDs    a Vector of groups which members have administrative rights for the group
   * @param mbrUserIDs     a Vector of user IDs this group has as members
   * @param mbrGroupIDs    a Vector of group IDs this group has as members
   * @param groupIDs       a Vector of group IDs this group is a member of
   * @param privileges     a Vector of privileges members of this group have
   * @param todoInMgr      String value which specifies whether the MCRUserMgr must be notified
   *                       about the construction of this object. Values may be "" (no action)
   *                       and "create" (create new group).
   */
  public MCRGroup(String ID, String creator, Timestamp creationDate, Timestamp modifiedDate,
                  String description, Vector admUserIDs, Vector admGroupIDs, Vector mbrUserIDs,
                  Vector mbrGroupIDs, Vector groupIDs, Vector privileges,
                  String todoInMgr) throws MCRException, Exception
  {
    this(ID, creator, creationDate, modifiedDate, description, admUserIDs, admGroupIDs,
         mbrUserIDs, mbrGroupIDs, groupIDs, privileges, true, todoInMgr);
  }

  /**
   * This constructor takes all attributes of this class as single variables.
   *
   * @param ID             the group ID
   * @param creator        the user ID who created this user
   * @param creationDate   timestamp of the creation of this user, if null the current date will be used
   * @param modifiedDate   timestamp of the last modification of this user
   * @param description    description of the group
   * @param admUserIDs     a Vector of user IDs which have administrative rights for the group
   * @param admGroupIDs    a Vector of groups which members have administrative rights for the group
   * @param mbrUserIDs     a Vector of user IDs this group has as members
   * @param mbrGroupIDs    a Vector of group IDs this group has as members
   * @param groupIDs       a Vector of group IDs this group is a member of
   * @param privileges     a Vector of privileges members of this group have
   * @param create         a boolean value determining whether to create (true) a group from
   *                       scratch or import (false) a group from file
   * @param todoInMgr      String value which specifies whether the MCRUserMgr must be notified
   *                       about the construction of this object. Values may be "" (no action)
   *                       and "create" (create new group).
   */
  public MCRGroup(String ID, String creator, Timestamp creationDate, Timestamp modifiedDate,
                  String description, Vector admUserIDs, Vector admGroupIDs, Vector mbrUserIDs,
                  Vector mbrGroupIDs, Vector groupIDs, Vector privileges,
                  boolean create, String todoInMgr) throws MCRException, Exception
  {
    super.ID = trim(ID);
    super.creator = trim(creator);

    // check if the creation timestamp is provided. If not, use current date and time
    if (creationDate == null)
      super.creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());
    else super.creationDate = creationDate;

    // use the update method to populate the remaining attributes
    update(description, admUserIDs, admGroupIDs, mbrUserIDs, mbrGroupIDs, groupIDs, privileges, "");

    // The timestamp of the last changes has been set to the current date and time in update().
    // If this is not wanted (i.e. if the parameter lastChanges is not null), it will be reset here:

    if (modifiedDate != null)
      super.modifiedDate = modifiedDate;

    // Eventually notify the User Manager and create a new group object in the datastore,
    // but only if this object is valid, i.e. all required fields are provided
    if (todoInMgr.trim().equals("create")) {
      if (isValid())
        MCRUserMgr.instance().createGroup(this, create);
      else
        throw new MCRException("Group object "+ID+" is not valid! Attributes are missing.");
    }
  }

  /**
   * This method adds a group to the groups list of the group. This is the list of
   * group IDs where this group is a member of, not the list of groups this group
   * has as members!
   *
   * @param groupID   ID of the group added to the group
   */
  public void addGroupID(String groupID) throws Exception
  { addAndUpdate(groupID, groupIDs); }

  /**
   * This method adds a group to the list of groups with administrative privileges of the group.
   * @param groupID   ID of the group added to the group admin list
   */
  public void addAdminGroupID(String groupID) throws Exception
  { addAndUpdate(groupID, admGroupIDs); }

   /**
   * This method adds a user (ID) to the administrators list of the group
   * @param userID   ID of the administrative user added to the group
   */
  public void addAdminUserID(String userID) throws Exception
  { addAndUpdate(userID, admUserIDs); }

  /**
   * This method adds a group to the list of member groups of the group. Do not confuse
   * with the list of groups the group itself is a member of.
   *
   * @param groupID   ID of the group added to the group member list
   */
  public void addMemberGroupID(String groupID) throws Exception
  { addAndUpdate(groupID, mbrGroupIDs); }

  /**
   * This method adds a user (ID) to the users list of the group
   * @param userID   ID of the user added to the group
   */
  public void addMemberUserID(String userID) throws Exception
  { addAndUpdate(userID, mbrUserIDs); }

  /**
   * This method adds a privilege to the privileges list of the group
   * @param privName   Name of the privilege added to the group
   */
  public void addPrivilege(String privName) throws Exception
  { addAndUpdate(privName, privileges); }

  /**
   * @return
   *   This method returns the list of admin groups as a Vector of strings.
   */
  public Vector getAdminGroupIDs()
  { return admGroupIDs; }

  /**
   * @return
   *   This method returns the list of admin users as a Vector of strings.
   */
  public Vector getAdminUserIDs()
  { return admUserIDs; }

  /**
   * @return
   *   This method returns the list of group members (groups) as a Vector of strings.
   *   Do not confuse with the list of groups the group itself is a member of.
   */
  public Vector getMemberGroupIDs()
  { return mbrGroupIDs; }

  /**
   * @return
   *   This method returns the user list (group members) as a Vector of strings.
   */
  public Vector getMemberUserIDs()
  { return mbrUserIDs; }

  /**
   * @return
   *   This method returns the list of privileges as a Vector of strings.
   */
  public Vector getPrivileges()
  { return privileges; }

  /**
   * This method checks if members of this group have a given privilege. Not only the
   * privileges of this group will be tested but also the privileges of the groups where
   * this group is a member of (recursivley).
   *
   * @return   returns true if the given privilege is in the list of privileges of this group
   *           or implicitly in one of the groups where this group is a member of.
   */
  public boolean hasPrivilege(String privilege) throws Exception
  {
    if (privileges.contains(privilege))
      return true;
    else {
      for (int i=0; i<groupIDs.size(); i++) {
        MCRGroup nextGroup = MCRUserMgr.instance().retrieveGroup((String)groupIDs.elementAt(i));
        if (nextGroup.hasPrivilege(privilege))
          return true;
      }
    }
    return false;
  }

  /**
   * This method checks whether a group implicitly is a member of a given group.
   * It is a recursive method.
   *
   * @param group     The group to be checked
   * @param matchID   ID of the group to check if 'group' is a member of it
   * @return          returns true if 'group' is an implicit member of the group with ID 'matchID'.
   */
  public static boolean isImplicitMemberOf(MCRGroup group, String matchID) throws Exception
  {
    Vector groupIDs = group.getGroupIDs();
    if (groupIDs.contains(matchID))
      return true;
    else {
      for (int i=0; i<groupIDs.size(); i++) {
        MCRGroup nextGroup = MCRUserMgr.instance().retrieveGroup((String)groupIDs.elementAt(i), true);
        if (MCRGroup.isImplicitMemberOf(nextGroup, matchID))
          return true;
      }
    }
    return false;
  }

  /**
   * This method checks whether this group is a member of a given group. It not only
   * considers the groups list of this group but recursively checks the group lists
   * of all groups it is a member of.
   *
   * @param groupID   ID of the group to check if this group is a member of
   * @return          returns true if the group is an implicit member of the given group
   */
  public boolean isMemberOf(String groupID) throws Exception
  {
    if (super.groupIDs.contains(groupID))
      return true;
    else return MCRGroup.isImplicitMemberOf(this, groupID);
  }

  /**
   * This method checks if all required fields have been provided. In a later
   * stage of the software development a User Policy object will be asked, which
   * fields exactly are the required fields. This will be configurable.
   *
   * @return   returns true if all required fields have been provided
   */
  public boolean isValid() throws Exception
  {
    Vector requiredGroupAttributes = MCRUserPolicy.instance().getRequiredGroupAttributes();
    boolean test = true;

    if (requiredGroupAttributes.contains("groupID"))
      test = test && super.ID.length() > 0;
    if (requiredGroupAttributes.contains("creator"))
      test = test && super.ID.length() > 0;

    return test;
  }

  /**
   * This method removes a group from the list of groups with administrative privileges
   * for this group.
   *
   * @param groupID   ID of the administrative group removed from the group
   */
  public void removeAdminGroupID(String groupID) throws Exception
  { removeAndUpdate(groupID, admGroupIDs); }

  /**
   * This method removes a user from the list of administrators of the group.
   * @param userID   ID of the administrative user removed from the group
   */
  public void removeAdminUserID(String userID) throws Exception
  { removeAndUpdate(userID, admUserIDs); }

  /**
   * This method removes a group from the groups list of the group. These are the
   * groups where the group itself is a member of.
   *
   * @param groupID   ID of the group removed from the group
   */
  public void removeGroupID(String groupID) throws Exception
  { removeAndUpdate(groupID, groupIDs); }

  /**
   * This method removes a group from the list of group members (groups).
   * Do not confuse with the list of groups the group itself is a member of.
   *
   * @param groupID   ID of the group removed from the group
   */
  public void removeMemberGroupID(String groupID) throws Exception
  { removeAndUpdate(groupID, mbrGroupIDs); }

  /**
   * This method removes a user from the users list (members) of the group.
   *
   * @param userID   ID of the user removed from the group
   */
  public void removeMemberUserID(String userID) throws Exception
  { removeAndUpdate(userID, mbrUserIDs); }

  /**
   * This method removes a privilege from the privileges list of the group.
   *
   * @param privName   Name of the privilege removed from the group
   */
  public void removePrivilege(String privName) throws Exception
  { removeAndUpdate(privName, privileges); }

  /**
   * Updates some of the attributes of the group. Some attributes (ID, creator, creationDate,
   * lastChanges) cannot be updated. The date of the last changes will be updated inside
   * of this method.
   *
   * @param description    description of the group
   * @param admUserIDs     a Vector of user IDs which have administrative rights for the group
   * @param admGroupIDs    a Vector of groups which members have administrative rights for the group
   * @param mbrUserIDs     a Vector of user IDs this group has as members
   * @param mbrGroupIDs    a Vector of group IDs this group has as members
   * @param groupIDs       a Vector of group IDs this group is a member of
   * @param privileges     a Vector of privileges members of this group have
   * @param todoInMgr      String value which specifies whether the MCRUserMgr must be notified.
   */
  public void update(String description, Vector admUserIDs, Vector admGroupIDs, Vector mbrUserIDs,
                     Vector mbrGroupIDs, Vector groupIDs, Vector privileges,
                     String todoInMgr) throws MCRException, Exception
  {
    super.description = trim(description);

    // update the date of the last changes
    super.modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());

    // the following lists might be null due to the default constructor
    super.groupIDs   = (groupIDs != null) ? groupIDs : new Vector();
    this.admUserIDs  = (admUserIDs != null) ? admUserIDs : new Vector();
    this.admGroupIDs = (admGroupIDs != null) ? admGroupIDs : new Vector();
    this.mbrUserIDs  = (mbrUserIDs != null) ? mbrUserIDs : new Vector();
    this.mbrGroupIDs = (mbrGroupIDs != null) ? mbrGroupIDs : new Vector();
    this.privileges  = (privileges != null) ? privileges : new Vector();

    // Eventually notify the User Manager and update the user object in the datastore,
    // but only if this object is valid.

    if (todoInMgr.trim().equals("update")) {
      if (isValid())
        MCRUserMgr.instance().updateGroup(this);
      else
        throw new MCRException("Group object "+ID+" is not valid! Attributes are missing.");
    }
  }

  /**
   * @return
   *   This method returns the user or group object as a JDOM document.
   */
  public Document toJDOMDocument() throws Exception
  {
    Element root = new Element("mcr_userobject");
    root.setAttribute("type", "group");
    root.addContent(this.toJDOMElement());
    Document jdomDoc = new Document(root);
    return jdomDoc;
  }

  /**
   * @return
   *   This method returns the user or group object as a JDOM element. This is needed
   *   if one wants to get a representation of several user or group objects in one xml
   *   document.
   */
  public Element toJDOMElement() throws Exception
  {
    Element group        = new Element("group").setAttribute("ID", ID);
    Element Creator      = new Element("group.creator").setText(super.creator);
    Element CreationDate = new Element("group.creation_date").setText(super.creationDate.toString());
    Element ModifiedDate = new Element("group.last_modified").setText(super.modifiedDate.toString());
    Element Description  = new Element("group.description").setText(super.description);
    Element admins       = new Element("group.admins");
    Element members      = new Element("group.members");
    Element groups       = new Element("group.groups");
    Element Privileges   = new Element("group.privileges");

    // Loop over all admin user IDs
    for (int i=0; i<admUserIDs.size(); i++) {
      Element admUserID = new Element("admins.userID").setText((String)admUserIDs.elementAt(i));
      admins.addContent(admUserID);
    }

    // Loop over all admin group IDs
    for (int i=0; i<admGroupIDs.size(); i++) {
      Element admGroupID = new Element("admins.groupID").setText((String)admGroupIDs.elementAt(i));
      admins.addContent(admGroupID);
    }

    // Loop over all user IDs (members of this group!)
    for (int i=0; i<mbrUserIDs.size(); i++) {
      Element mbrUserID = new Element("members.userID").setText((String)mbrUserIDs.elementAt(i));
      members.addContent(mbrUserID);
    }

    // Loop over all group IDs (members of this group!)
    for (int i=0; i<mbrGroupIDs.size(); i++) {
      Element mbrGroupID = new Element("members.groupID").setText((String)mbrGroupIDs.elementAt(i));
      members.addContent(mbrGroupID);
    }

    // Loop over all group IDs (where this group is a member of!)
    for (int i=0; i<groupIDs.size(); i++) {
      Element groupID = new Element("groups.groupID").setText((String)groupIDs.elementAt(i));
      groups.addContent(groupID);
    }

    // Loop over all privileges
    for (int i=0; i<privileges.size(); i++) {
      Element priv = new Element("privileges.privilege").setText((String)privileges.elementAt(i));
      Privileges.addContent(priv);
    }

    // Aggregate group element
    group.addContent(Creator)
         .addContent(CreationDate)
         .addContent(ModifiedDate)
         .addContent(Description)
         .addContent(admins)
         .addContent(members)
         .addContent(groups)
         .addContent(Privileges);

    return group;
  }

  /**
   * This private helper method adds values to a given vector. It is used by addGroupID etc.
   *
   * @param s       String to be added to the vector vec
   * @param vec     Vector to which the string s will be added to
   */
  private void addAndUpdate(String s, Vector vec) throws Exception
  {
    if (!vec.contains(s)) {
      vec.add(s);
      super.modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());
      MCRUserMgr.instance().updateGroup(this);
    }
  }

  /**
   * This private helper method removes values from a given vector. It is used by removeGroupID etc.
   *
   * @param s       String to be removed from the vector vec
   * @param vec     Vector from which the string s will be removed from
   */
  private void removeAndUpdate(String s, Vector vec) throws Exception
  {
    if (vec.contains(s)) {
      vec.remove(s);
      super.modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());
      MCRUserMgr.instance().updateGroup(this);
    }
  }
