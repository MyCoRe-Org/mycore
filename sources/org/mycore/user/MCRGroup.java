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
 * a new user or a new group, to delete users etc.) are determined by the belonging
 * to a group. Hence the main duty of a group object is to define exactly which privileges
 * the members will have.
 *
 * @see mycore.user.MCRUserMgr
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRGroup
{
  /** The group ID of the MyCoRe group */
  private String groupID = "";

  /** A list of privileges members of this group have */
  private Vector privileges = null;

  /** A list of users which are members of the group */
  private Vector users = null;

  /** A list of other groups which are members of the group */
  private Vector groups = null;

  /** Specify whether the UserManager must be notified about the creation of this object */
  private boolean bCreateInMgr = true;

  /**
   * Creates a group object from a XML string which must be passed as a parameter.
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
    this.bCreateInMgr = bCreateInMgr;

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
    groupID = MCRXMLHelper.getElementText("groupID", domGroup);

    // Now we extract the privileges-information from the DOM Element and fill the
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
   * returns the group object as an xml representation
   *
   * @param NL separation sequence. Typically this will be an empty string (if the XML
   *        representation is needed as one line) or a newline ("\n") sequence.
   * @return returns the group object as an xml representation
   */
  public String getGroupAsXML(String NL)
  {
    // At first we create XML representations of the privileges and members lists
    StringBuffer privBuf = new StringBuffer();
    privBuf.append("<privileges>").append(NL);
    for (int i=0; i<privileges.size(); i++) {
      privBuf.append("<privilege>").append(privileges.elementAt(i)).append("</privilege>").append(NL);
    }
    privBuf.append("</privileges>");

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
    sb.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>").append(NL)
      .append("<mycore_user_and_group_info type=\"group\">").append(NL)
      .append("<group>").append(NL)
      .append("<groupID>").append(groupID).append("</groupID>" ).append(NL)
      .append(privBuf).append(NL)
      .append(memberBuf).append(NL)
      .append("</group>").append(NL)
      .append("</mycore_user_and_group_info>").append(NL);
    return sb.toString();
  }

  /**
   * returns the groupID of the group
   * @return returns the groupID of the group
   */
  public String getGroupID()
  { return groupID; }

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
   * This method returns the group information as a formatted string.
   *
   * @return group information, all in one string
   */
  public String getFormattedInfo() throws Exception
  {
    StringBuffer sb = new StringBuffer();
    sb.append("group ID         : ").append(groupID);
    sb.append("\n").append("privileges       : ");
    for (int i=0; i<privileges.size(); i++) {
      sb.append(privileges.elementAt(i)).append(",");
    }
    sb.append("\n").append("members [groups] : ");
    for (int i=0; i<groups.size(); i++) {
      sb.append(groups.elementAt(i)).append(",");
    }
    sb.append("\n").append("members [users]  : ");
    for (int i=0; i<users.size(); i++) {
      sb.append(users.elementAt(i)).append(",");
    }
    sb.append("\n");
    return sb.toString();
  }
}
