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

import java.util.Date;
import java.util.Vector;
import java.text.SimpleDateFormat;
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
 * a new user or a new group, to delete users etc.) are determined by the belonging
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
   * Creates a group object from an XML string which must be passed as a parameter.
   *
   * @param groupXML     XML string containing all neccessary information to create a group object.
   * @param bCreateInMgr boolean value which specifies whether the MCRUserMgr must be notified
   *                     about the creation of this object
   */
  public MCRGroup(String groupXML) throws Exception
  { this(groupXML, true); }

  public MCRGroup(String groupXML, boolean bCreateInMgr) throws Exception
  {
    MCRArgumentChecker.ensureNotNull(groupXML, "groupXML");
    super.bCreateInMgr = bCreateInMgr;

    // Parse the XML-string of the group and get a DOM representation

    Document mcrDocument = MCRXMLHelper.parseXML(groupXML);
    NodeList domGroupList = mcrDocument.getElementsByTagName("group");
    Element domGroup = (Element)domGroupList.item(0);

    create(domGroup, bCreateInMgr);
  }

  /**
   * Creates a group object from a DOM element which must be passed as a parameter.
   *
   * @param domGroup     DOM element containing all neccessary information to create a group object.
   * @param bCreateInMgr boolean value which specifies whether the MCRUserMgr must be notified
   *                     about the creation of this object
   */
  public MCRGroup(Element domGroup) throws Exception
  { create(domGroup, true); }

  private final void create(Element domGroup, boolean bCreateInMgr) throws Exception
  {
    NodeList accountList = domGroup.getElementsByTagName("account");
    NodeList accountElements = accountList.item(0).getChildNodes();

    ID          = trim(MCRXMLHelper.getElementText("groupID", accountElements));
    creator     = trim(MCRXMLHelper.getElementText("creator", accountElements));
    description = trim(MCRXMLHelper.getElementText("description", accountElements));

    // extract date information
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
    String date = trim(MCRXMLHelper.getElementText("creationdate", accountElements));
    if (date.equals(""))
      creationDate = new Date();
    else
      creationDate = sdf.parse(date);

    date = trim(MCRXMLHelper.getElementText("last_changes", accountElements));
    if (date.equals(""))
      lastChanges = creationDate;
    else
      lastChanges = sdf.parse(date);

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

    // Notify the User Manager
    if (bCreateInMgr)
      MCRUserMgr.instance().createGroup(this);
  }

  /**
   * This method adds a group to the groups list of the group
   * @param groupID  ID of the group added to the group
   */
  public void addGroup(String groupID) throws Exception
  {
    groups.add(groupID);
    super.lastChanges = new Date();
    MCRUserMgr.instance().updateGroup(this);
  }

  /**
   * This method adds a user to the users list of the group
   * @param userID  ID of the user added to the group
   */
  public void addUser(String userID) throws Exception
  {
    users.add(userID);
    super.lastChanges = new Date();
    MCRUserMgr.instance().updateGroup(this);
  }

  /**
   * This method returns the group information as a formatted string.
   *
   * @return group information, all in one string
   */
  public String getFormattedInfo() throws Exception
  {
    StringBuffer sb = new StringBuffer();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    sb.append("group ID      : ").append(ID).append("\n");
    sb.append("creator       : ").append(creator).append("\n");
    sb.append("creation date : ").append(sdf.format(creationDate)).append("\n");
    sb.append("last changes  : ").append(sdf.format(lastChanges)).append("\n");
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
    sb.append("\n").append("members [groups] : ");
    for (int i=0; i<groups.size(); i++) {
      sb.append(groups.elementAt(i)).append(",");
    }
    sb.append("\n");
    return sb.toString();
  }

  /**
   * returns the group list (group members) as a Vector of strings
   * @return returns the group list (group members) as a Vector of strings
   */
  public Vector getGroups()
  { return groups; }

  /**
   * returns the list of privileges as a Vector of strings
   * @return returns the list of privileges as a Vector of strings
   */
  public Vector getPrivileges()
  { return privileges; }

  /**
   * returns the user list (group members) as a Vector of strings
   * @return returns the user list (group members) as a Vector of strings
   */
  public Vector getUsers()
  { return users; }

  /**
   * checks if members of this group have a given privilege
   * @return returns true if the given privilege is in the list of privileges
   *         of this group
   */
  public boolean hasPrivilege(String privilege) throws Exception
  {
    if (privileges.contains(privilege))
      return true;
    else
      return false;
  }

  /**
   * This method removes a group from the groups list of the group
   * @param userID  ID of the group removed from the group
   */
  public void removeGroup(String groupID) throws Exception
  {
    groups.remove(groupID);
    super.lastChanges = new Date();
    MCRUserMgr.instance().updateGroup(this);
  }

  /**
   * This method removes a user from the users list of the group
   * @param userID  ID of the user removed from the group
   */
  public void removeUser(String userID) throws Exception
  {
    users.remove(userID);
    super.lastChanges = new Date();
    MCRUserMgr.instance().updateGroup(this);
  }

  /**
   * returns the group object as an xml representation
   *
   * @param NL separation sequence. Typically this will be an empty string (if the XML
   *        representation is needed as one line) or a newline ("\n") sequence.
   * @return returns the group object as an xml representation
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
    for (int i=0; i<groups.size(); i++) {
      memberBuf.append("<groupID>").append(groups.elementAt(i)).append("</groupID>").append(NL);
    }
    memberBuf.append("</members>");

    // Now we put together all information of the group
    StringBuffer sb = new StringBuffer();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    sb.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>").append(NL)
      .append("<mycore_user_and_group_info type=\"group\">").append(NL)
      .append("<group>").append(NL)
      .append("<account>").append(NL)
      .append("<groupID>").append(ID).append("</groupID>" ).append(NL)
      .append("<creator>").append(creator).append("</creator>").append(NL)
      .append("<creationdate>").append(sdf.format(creationDate)).append("</creationdate>").append(NL)
      .append("<last_changes>").append(sdf.format(lastChanges)).append("</last_changes>").append(NL)
      .append("<description>").append(description).append("</description>").append(NL)
      .append("</account>").append(NL)
      .append(adminsBuf).append(NL)
      .append(privBuf).append(NL)
      .append(memberBuf).append(NL)
      .append("</group>").append(NL)
      .append("</mycore_user_and_group_info>").append(NL);
    return sb.toString();
  }
}
