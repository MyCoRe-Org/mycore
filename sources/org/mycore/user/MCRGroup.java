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

package mycore.user;

import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mycore.common.*;
import mycore.xml.MCRXMLHelper;

/**
 * Instances of this class represent MyCoRe groups.
 * <p>
 * In the MyCoRe user component the privileges of a user (e.g. the privilege to create
 * a new user or a new group, to delete users etc.) are determined by the membership
 * to a group. Hence the main duty of a group object is to define exactly which privileges
 * the members will have.
 *
 * @see mycore.user.MCRUserMgr
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRGroup extends MCRUserUnit
{
  /** A list of privileges members of this group have */
  private Vector privileges = null;

  /** A list of users which are members of the group */
  private Vector users = null;

  /** A list of other groups which are members of the group */
  private Vector groups = null;

  /** A list of users which have the privilege to administer this group */
  private Vector admins = null;

  /** A list of groups which members have the privilege to administer this group */
  private Vector adminGroups = null;

  /**
   * Default constructor. It is used to create a group object with empty fields. This is
   * useful for constructing an XML representation of a group without specialized data.
   * This empty group object will not be created in the persistent data store.
   */
  public MCRGroup() throws Exception
  {
    this("", "", null, null, "", null, null, null, null, "");
  }

  /**
   * Creates a group object from an XML string which must be passed as a parameter.
   *
   * @param groupXML
   *   XML string containing all neccessary information to create a group object.
   * @param todoInMgr
   *   String value which specifies whether the MCRUserMgr must be notified about
   *   the construction of this object. Values may be "" (no action), "create"
   *   (create new group) and "update" (update existing group).
   */
  public MCRGroup(String groupXML, String todoInMgr) throws Exception
  {
    // Parse the XML-string of the group and get a DOM representation. Then pass
    // this to the private create()-method.

    MCRArgumentChecker.ensureNotNull(groupXML, "groupXML");
    Document mcrDocument = MCRXMLHelper.parseXML(groupXML);
    NodeList domGroupList = mcrDocument.getElementsByTagName("group");
    Element domGroup = (Element)domGroupList.item(0);
    create(domGroup, todoInMgr);
  }

  /**
   * Creates a group object from a DOM element which must be passed as a parameter.
   *
   * @param domGroup
   *   DOM element containing all neccessary information to create a group object.
   * @param todoInMgr
   *   String value which specifies whether the MCRUserMgr must be notified about
   *   the construction of this object. Values may be "" (no action), "create"
   *   (create new group) and "update" (update existing group).
   */
  public MCRGroup(Element domGroup, String todoInMgr) throws Exception
  { create(domGroup, todoInMgr); }

  /**
   * This constructor takes all attributes of this class as single variables.
   *
   * @param ID             the group ID
   * @param creator        the user ID who creates this group
   * @param creationDate   timestamp of the creation of this group, if null the current date will be used
   * @param lastChanges    timestamp of the last modification of this group
   * @param description    description of the group
   * @param admins         a Vector of users which have administrative rights for the group
   * @param adminGroups    a Vector of groups which members have administrative rights for the group
   * @param users          a Vector of users this group has as members
   * @param privileges     a Vector of privileges this group has
   * @param todoInMgr      String value which specifies whether the MCRUserMgr must be notified
   *                       about the construction of this object. Values may be "" (no action),
   *                       "create" (create new group) and "update" (update existing group).
   */
  public MCRGroup(String ID, String creator, Timestamp creationDate, Timestamp lastChanges,
                  String description, Vector admins, Vector adminGroups, Vector users,
                  Vector privileges, String todoInMgr) throws MCRException, Exception
  {
    super.ID = trim(ID);
    super.creator = trim(creator);

    // check if the creation timestamp is provided. If not, use current date and time
    if (creationDate == null)
      super.creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());
    else
      super.creationDate = creationDate;

    // use the update method to populate the remaining attributes
    update(description, admins, adminGroups, users, privileges, todoInMgr);

    // The timestamp of the last changes has been set to the current date and time in
    // update(). If this is not wanted (i.e. if the parameter lastChanges is not null),
    // it will be reset here:
    if (lastChanges != null)
      super.lastChanges = lastChanges;

    // Eventually notify the User Manager and create a new group object in the datastore,
    // but only if this object is valid, i.e. all required fields are provided
    if (todoInMgr.trim().equals("create")) {
      if (isValid())
        MCRUserMgr.instance().createGroup(this);
      else
        throw new MCRException("Group object "+ID+" is not valid! Attributes are missing.");
    }
  }

  /**
   * This method creates a group object from a DOM element which must be passed as a parameter.
   *
   * @param domGroup
   *   DOM element containing all neccessary information to create a group object.
   * @param todoInMgr
   *   String value which specifies whether the MCRUserMgr must be notified about
   *   the construction of this object. Values may be "" (no action), "create"
   *   (create new group) and "update" (update existing group).
   */
  private final void create(Element domGroup, String todoInMgr) throws Exception
  {
    NodeList accountList = domGroup.getElementsByTagName("account");
    NodeList accountElements = accountList.item(0).getChildNodes();

    ID          = domGroup.getAttribute("groupID").trim();
    creator     = trim(MCRXMLHelper.getElementText("creator", accountElements));
    description = trim(MCRXMLHelper.getElementText("description", accountElements));

    // extract date information
    String date = trim(MCRXMLHelper.getElementText("creationdate", accountElements));
    if (date.equals(""))
      creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());
    else
      creationDate = Timestamp.valueOf(date);

    date = trim(MCRXMLHelper.getElementText("last_changes", accountElements));
    if (todoInMgr.trim().equals("update"))
      lastChanges = new Timestamp(new GregorianCalendar().getTime().getTime());
    else if (date.equals(""))
      lastChanges = creationDate;
    else
      lastChanges = Timestamp.valueOf(date);

    // Now we extract the admins-information from the DOM element and fill the Vectors
    // "admins" and "adminGroups".

    NodeList adminList = domGroup.getElementsByTagName("admins");
    NodeList adminElements = adminList.item(0).getChildNodes();
    admins = new Vector(MCRXMLHelper.getAllElementTexts("userID", adminElements));
    adminGroups = new Vector(MCRXMLHelper.getAllElementTexts("groupID", adminElements));

    // Now we extract the privileges-information from the DOM element and fill the
    // Vector "privileges". This way the group knows which privileges it has, resp.
    // members of this group have.

    NodeList privList = domGroup.getElementsByTagName("privileges");
    NodeList privElements = privList.item(0).getChildNodes();
    privileges = new Vector(MCRXMLHelper.getAllElementTexts("privilege", privElements));

    // Now we extract the members-information from the DOM Element and fill the
    // Vectors "users" and "groups". Members of a group can be users as well as
    // other groups.

    NodeList memberList = domGroup.getElementsByTagName("members");
    NodeList memberElements = memberList.item(0).getChildNodes();
    users  = new Vector(MCRXMLHelper.getAllElementTexts("userID", memberElements));
    groups = new Vector(MCRXMLHelper.getAllElementTexts("groupID", memberElements));

    // Eventually notify the User Manager and create a new group object or update an
    // existing group object in the datastore, but only if this object is valid, i.e.
    // all required fields are provided

    if (isValid()) {
      if (todoInMgr.trim().equals("create"))
        MCRUserMgr.instance().createGroup(this);
      if (todoInMgr.trim().equals("update"))
        MCRUserMgr.instance().updateGroup(this);
    }
    else
      throw new MCRException("Group object "+ID+" is not valid! Attributes are missing.");
  }

  /**
   * This method adds a group to the groups list of the group
   *
   * @param groupID
   *   ID of the group added to the group
   */
  public void addGroup(String groupID) throws Exception
  {
    if (!groups.contains(groupID))
    {
      groups.add(groupID);
      super.lastChanges = new Timestamp(new GregorianCalendar().getTime().getTime());
      MCRUserMgr.instance().updateGroup(this);
    }
  }

  /**
   * This method adds a user to the users list of the group
   *
   * @param userID
   *   ID of the user added to the group
   */
  public void addUser(String userID) throws Exception
  {
    if (!users.contains(userID))
    {
      users.add(userID);
      super.lastChanges = new Timestamp(new GregorianCalendar().getTime().getTime());
      MCRUserMgr.instance().updateGroup(this);
    }
  }

  /**
   * @return
   *   This method returns the group information as a formatted string.
   */
  public String getFormattedInfo() throws Exception
  {
    StringBuffer sb = new StringBuffer();

    sb.append("group ID      : ").append(ID).append("\n");
    sb.append("creator       : ").append(creator).append("\n");
    sb.append("creation date : ").append(creationDate.toString()).append("\n");
    sb.append("last changes  : ").append(lastChanges.toString()).append("\n");
    sb.append("description   : ").append(description).append("\n");

    sb.append("\n").append("admins [users]   : ");
    for (int i=0; i<admins.size(); i++) {
      sb.append(admins.elementAt(i)).append(",");
    }
    sb.append("\n").append("admins [groups]  : ");
    for (int i=0; i<adminGroups.size(); i++) {
      sb.append(adminGroups.elementAt(i)).append(",");
    }
    sb.append("\n").append("privileges       : ");
    for (int i=0; i<privileges.size(); i++) {
      sb.append(privileges.elementAt(i)).append(",");
    }
    sb.append("\n").append("members [users]  : ");
    for (int i=0; i<users.size(); i++) {
      sb.append(users.elementAt(i)).append(",");
    }
/*
    sb.append("\n").append("members [groups] : ");
    for (int i=0; i<groups.size(); i++) {
      sb.append(groups.elementAt(i)).append(",");
    }
*/
    sb.append("\n");
    return sb.toString();
  }

  /**
   * @return
   *   This method returns the list of admin groups as a Vector of strings.
   */
  public Vector getAdminGroups()
  { return adminGroups; }

  /**
   * @return
   *   This method returns the list of admin users as a Vector of strings.
   */
  public Vector getAdminUsers()
  { return admins; }

  /**
   * @return
   *   This method returns the group list (group members) as a Vector of strings.
   */
  public Vector getGroups()
  { return groups; }

  /**
   * @return
   *   This method returns the list of privileges as a Vector of strings.
   */
  public Vector getPrivileges()
  { return privileges; }

  /**
   * @return
   *   This method returns the user list (group members) as a Vector of strings.
   */
  public Vector getUsers()
  { return users; }

  /**
   * This method checks if all required fields have been provided. In a later
   * stage of the software development a User Policy object will be asked, which
   * fields exactly are the required fields. This will be configurable.
   *
   * @return
   *   returns true if all required fields have been provided
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
   * This method checks if members of this group have a given privilege
   *
   * @return
   *   returns true if the given privilege is in the list of privileges of this group
   */
  public boolean hasPrivilege(String privilege) throws Exception
  { return (privileges.contains(privilege)) ? true : false; }

  /**
   * This method removes a group from the groups list of the group.
   *
   * @param userID
   *   ID of the group removed from the group
   */
  public void removeGroup(String groupID) throws Exception
  {
    if (groups.contains(groupID))
    {
      groups.remove(groupID);
      super.lastChanges = new Timestamp(new GregorianCalendar().getTime().getTime());
      MCRUserMgr.instance().updateGroup(this);
    }
  }

  /**
   * This method removes a user from the users list of the group.
   *
   * @param userID
   *   ID of the user removed from the group
   */
  public void removeUser(String userID) throws Exception
  {
    if (users.contains(userID))
    {
      users.remove(userID);
      super.lastChanges = new Timestamp(new GregorianCalendar().getTime().getTime());
      MCRUserMgr.instance().updateGroup(this);
    }
  }

  /**
   * This method returns the group object as an xml representation.
   *
   * @param NL
   *   separation sequence. Typically this will be an empty string (if the XML
   *   representation is needed as one line) or a newline ("\n") sequence.
   * @return
   *   returns the group object as an xml representation
   */
  public String toXML(String NL) throws Exception
  {
    // At first we create XML representations of the admins, privileges and members lists
    StringBuffer adminsBuf = new StringBuffer();
    adminsBuf.append("<admins>").append(NL);
    for (int i=0; i<admins.size(); i++) {
      adminsBuf.append("<userID>").append(admins.elementAt(i)).append("</userID>").append(NL);
    }
    for (int i=0; i<adminGroups.size(); i++) {
      adminsBuf.append("<groupID>").append(adminGroups.elementAt(i)).append("</groupID>").append(NL);
    }
    adminsBuf.append("</admins>");

    // privileges
    StringBuffer privBuf = new StringBuffer();
    privBuf.append("<privileges>").append(NL);
    for (int i=0; i<privileges.size(); i++) {
      privBuf.append("<privilege>").append(privileges.elementAt(i)).append("</privilege>").append(NL);
    }
    privBuf.append("</privileges>");

    // members
    StringBuffer memberBuf = new StringBuffer();
    memberBuf.append("<members>").append(NL);
    for (int i=0; i<users.size(); i++) {
      memberBuf.append("<userID>").append(users.elementAt(i)).append("</userID>").append(NL);
    }
    /* at the moment not possible
    for (int i=0; i<groups.size(); i++) {
      memberBuf.append("<groupID>").append(groups.elementAt(i)).append("</groupID>").append(NL);
    }
    */
    memberBuf.append("</members>");

    // Now we put together all information of the group
    StringBuffer sb = new StringBuffer();

    sb.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>").append(NL)
      .append("<userinfo type=\"group\">").append(NL)
      .append("<group groupID=\"").append(ID).append("\">").append(NL)
      .append("<account>").append(NL)
      .append("<creator>").append(creator).append("</creator>").append(NL)
      .append("<creationdate>").append(creationDate.toString()).append("</creationdate>").append(NL)
      .append("<last_changes>").append(lastChanges.toString()).append("</last_changes>").append(NL)
      .append("<description>").append(description).append("</description>").append(NL)
      .append("</account>").append(NL)
      .append(adminsBuf).append(NL)
      .append(privBuf).append(NL)
      .append(memberBuf).append(NL)
      .append("</group>").append(NL)
      .append("</userinfo>").append(NL);
    return sb.toString();
  }

  /**
   * Updates some of the attributes of the group. Some attributes (ID, creator, creationDate,
   * lastChanges) cannot be updated. The date of the last changes will be updated inside
   * of this method.
   *
   * @param description    description of the group
   * @param admins         a Vector of users which have administrative rights for the group
   * @param adminGroups    a Vector of groups which members have administrative rights for the group
   * @param users          a Vector of users this group has as members
   * @param privileges     a Vector of privileges this group has
   * @param todoInMgr      String value which specifies whether the MCRUserMgr must be notified.
   */
  public void update(String description, Vector admins, Vector adminGroups,
                     Vector users, Vector privileges,
                     String todoInMgr) throws MCRException, Exception
  {
    super.description = trim(description);

    // update the date of the last changes
    super.lastChanges = new Timestamp(new GregorianCalendar().getTime().getTime());

    // the following lists might be null due to the default constructor
    this.admins = (admins != null) ? admins : new Vector();
    this.adminGroups = (adminGroups != null) ? adminGroups : new Vector();
    this.users = (users != null) ? users : new Vector();

    this.privileges = privileges;

    // Eventually notify the User Manager and update the user object in the datastore,
    // but only if this object is valid.
    if (todoInMgr.trim().equals("update")) {
      if (isValid())
        MCRUserMgr.instance().updateGroup(this);
      else
        throw new MCRException("Group object "+ID+" is not valid! Attributes are missing.");
    }
  }
}
