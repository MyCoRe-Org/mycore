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
import org.w3c.dom.NodeList;
import mycore.xml.MCRXMLHelper;

/**
 * This class defines the set of privileges of the MyCoRe user management. It
 * is implemented as a singleton since there must not be two instances of this
 * class.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRPrivilegeSet
{
  /** This vector holds the names of the privileges */
  private Vector privNames = null;

  /** This vector holds the descriptions of the privileges */
  private Vector privDescriptions = null;

  /** The one and only instance of this class */
  private static MCRPrivilegeSet theInstance = null;

  /** private constructor to create the singleton instance. */
  private MCRPrivilegeSet() throws Exception
  {
    privNames = new Vector();
    privDescriptions = new Vector();

    MCRUserStore mcrUserStore = MCRUserMgr.instance().getUserStore();
    String privsXML = mcrUserStore.retrievePrivilegeSet();

    if (privsXML != null) { // There already exists a privilege set
      Document mcrDocument = MCRXMLHelper.parseXML(privsXML);
      NodeList domPrivList = mcrDocument.getElementsByTagName("privilege");
      this.loadPrivileges(domPrivList, false); // need not to create in data store
    }
  }

  /**
   * This method is the only way to get an instance of this class. It calls the
   * private constructor to create the singleton.
   *
   * @return returns the one and only instance of <CODE>MCRPrivilegeSet</CODE>
   */
  public final static synchronized MCRPrivilegeSet instance() throws Exception
  {
    if (theInstance == null)
      theInstance = new MCRPrivilegeSet();
    return theInstance;
  }

  /**
   * This method takes a NodeList as parameter and fills the vector of known
   * privileges of the system.
   *
   * @param privList       DOM NodeList with information about the privileges
   * @param createInStore  boolean value to determine whether the privileges have
   *                       to be stored in the persistent data store
   */
  public void loadPrivileges(NodeList privList, boolean createInStore)
              throws Exception
  {
    NodeList privElements = null;
    privNames.clear();
    privDescriptions.clear();

    for (int i=0; i<privList.getLength(); i++) {
      privElements = privList.item(i).getChildNodes();
      privNames.add(trim(MCRXMLHelper.getElementText("name", privElements)));
      privDescriptions.add(trim(MCRXMLHelper.getElementText("description", privElements)));
    }

    if (createInStore) {
      MCRUserStore mcrUserStore = MCRUserMgr.instance().getUserStore();
      if (mcrUserStore.existsPrivilegeSet())
        mcrUserStore.updatePrivilegeSet(this);
      else
        mcrUserStore.createPrivilegeSet(this);
    }
  }

  /**
   * This method returns the list of privileges.
   * @returns privNames  Vector with the names of the privileges
   */
  public Vector getPrivileges()
  { return privNames; }

  /**
   * returns the privilege set object as a DOM document
   * @return returns the privilege set object as a DOM document
   */
  public Document toDOM()
  { return MCRXMLHelper.parseXML(this.toXML("")); }

  /**
   * returns the privilege set object as an xml representation
   *
   * @param NL separation sequence. Typically this will be an empty string (if the XML
   *        representation is needed as one line) or a newline ("\n") sequence.
   * @return returns the privilege object as an xml representation
   */
  public String toXML(String NL)
  {
    StringBuffer sb = new StringBuffer();
    sb.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>").append(NL)
      .append("<mycore_user_and_group_info type=\"privilege\">").append(NL);

    for (int i=0; i<privNames.size(); i++) {
      sb.append("<privilege>").append(NL)
        .append("<name>").append((String)privNames.elementAt(i)).append("</name>").append(NL)
        .append("<description>").append((String)privDescriptions.elementAt(i)).append("</description>").append(NL)
        .append("</privilege>").append(NL);
    }
    sb.append("</mycore_user_and_group_info>").append(NL);
    return sb.toString();
  }

  /**
   * This helper method replaces null with an empty string and trims whitespace
   * from non-null strings.
   */
  private static String trim(String s)
  { return (s != null) ? s.trim() : ""; }
}
