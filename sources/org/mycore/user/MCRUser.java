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

import java.io.*;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Instances of this class represent MyCoRe users. MCRUser is part of the MyCoRe user
 * component and must not be used directly by other components. All user objects are
 * managed by the user manager (the instance of the singleton MCRUserMgr), which
 * is the only class of the MyCoRe user component that other components should use.
 *
 * @see mycore.user.MCRUserMgr
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUser
{
  /** The user ID of the MyCoRe user */
  private String userID = "";

  /** The password of the MyCoRe user */
  private String passwd = "";

  /** The date of creation of the user object in the MyCoRe system */
  private Date creationDate = null;

  /** The list of groups this MyCoRe user is a member of */
  private Vector groups = null;

  /** Object representing user address information */
  private MCRUserAddress userAddress;

  /**
   * Creates a user object from a DOM element which must be passed as a parameter.
   *
   * @param domUser DOM element containing all neccessary information to create a user object.
   */
  public MCRUser(Element domUser) throws Exception
  {
    // In a later stage of the software development we expect that users may have
    // more than one account. Therefore we use the following NodeList even though
    // we only allow one <account> element in the XML file at this time. The same
    // holds true for the user address part <address>.

    NodeList accountList = domUser.getElementsByTagName("account");
    NodeList accountElements = accountList.item(0).getChildNodes();

    userID = MCRXmlHelper.getElementText("userID", accountElements);
    passwd = MCRXmlHelper.getElementText("password", accountElements);

    String date = MCRXmlHelper.getElementText("creationdate", accountElements);
    if (date.equals(""))
      creationDate = new Date();
    else{
      DateFormat df = DateFormat.getDateInstance();
      creationDate  = df.parse(date);
    }

    NodeList addressList = domUser.getElementsByTagName("address");
    NodeList addressElements = addressList.item(0).getChildNodes();
    userAddress = new MCRUserAddress(addressElements);

    // Now we extract the group-information from the DOM Element and fill the
    // Vector "groups". So the user knows which groups he or she is a member of.

    NodeList groupList = domUser.getElementsByTagName("groups");
    NodeList groupElements = groupList.item(0).getChildNodes();
    groups = new Vector(MCRXmlHelper.getAllElementTexts("group", groupElements));
  }

  /**
   * returns the address information of the user. Only useful for a nice listing
   * of the address information.
   *
   * @return returns the address information of the user in a table formatted
   *         output, all in one string
   */
  public String getAddress()
  { return userAddress.toString(); }

  /**
   * returns the address information of the user. All attributes of the address
   * information are separated by a separator string, which must be provided as
   * a parameter. This is useful if you want e.g. a comma-separated list of the
   * attributes.
   *
   * @param separator separator sequence for the address attributes, e.g. a comma
   * @return address information of the user, all attributes separated by the
   *         separator sequence
   */
  public String getAddress(String separator)
  { return userAddress.toString(separator); }

  /**
   * returns the creation date of the user as a string
   * @return returns the creation date of the user as a string
   */
  public String getCreationDateAsString()
  { return DateFormat.getDateInstance().format(creationDate); }

  /**
   * returns the group list of the user as a Vector of strings
   * @return returns the group list of the user as a Vector of strings
   */
  public Vector getGroups()
  { return groups; }

  /**
   * returns the password of the user
   * @return returns the password of the user
   */
  public String getPassword()
  { return passwd; }

  /**
   * returns the user object as an xml representation
   *
   * @param NL separation sequence. Typically this will be an empty string (if the XML
   *        representation is needed as one line) or a newline ("\n") sequence.
   * @return returns the group object as an xml representation
   */
  public String getUserAsXML(String NL)
  {
    // At first we create an XML representation of the group list. This will be
    // used further down.
    StringBuffer groupBuf = new StringBuffer();
    groupBuf.append("<groups>").append(NL);
    for (int i=0; i<groups.size(); i++) {
      groupBuf.append("<group>").append(groups.elementAt(i)).append("</group>").append(NL);
    }
    groupBuf.append("</groups>");

    // Now we put together all information of the user
    StringBuffer ub = new StringBuffer();
    ub.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>").append(NL)
      .append("<mycore_user_and_group_info type=\"user\">").append(NL)
      .append("<user>").append(NL)
      .append("<account>").append(NL)
      .append("<userID>").append(userID).append("</userID>" ).append(NL)
      .append("<password>").append(passwd).append("</password>").append(NL)
      .append("<creationdate>").append(getCreationDateAsString()).append("</creationdate>").append(NL)
      .append("</account>").append(NL)
      .append(userAddress.getAddressAsXmlElement(NL)).append(NL)
      .append(groupBuf).append(NL)
      .append("</user>").append(NL)
      .append("</mycore_user_and_group_info>").append(NL);
    return ub.toString();
  }

  /**
   * returns the userID of the user
   * @return returns the userID of the user
   */
  public String getUserID()
  { return userID; }

  /**
   * sets the password of the user
   * @param newPassword the new password of the user
   */
  public void setPassword(String newPassword)
  {
    // At the moment, the permission to do this is not checked...
    passwd = newPassword;
  }
}
