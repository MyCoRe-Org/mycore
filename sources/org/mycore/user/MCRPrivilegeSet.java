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

import java.util.Vector;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.*;

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
  /** This vector holds all privileges of the org.mycore.user management system */
  private Vector privileges = null;

  /** the class responsible for persistent datastore (configurable ) */
  private MCRUserStore mcrUserStore;

  /** The one and only instance of this class */
  private static MCRPrivilegeSet theInstance = null;

  /** private constructor to create the singleton instance. */
  private MCRPrivilegeSet() throws Exception
  {
    Vector privs = new Vector();
    String userStoreName = MCRConfiguration.instance().getString("MCR.userstore_class_name");
    mcrUserStore = (MCRUserStore)Class.forName(userStoreName).newInstance();
    privs = mcrUserStore.retrievePrivilegeSet();

    if (!(privs == null)) // There already exists a privilege set
      this.privileges = privs;
  }

  /**
   * This method is the only way to get an instance of this class. It calls the
   * private constructor to create the singleton.
   *
   * @return
   *   returns the one and only instance of <CODE>MCRPrivilegeSet</CODE>
   */
  public final static synchronized MCRPrivilegeSet instance() throws Exception
  {
    if (theInstance == null)
      theInstance = new MCRPrivilegeSet();
    return theInstance;
  }

  /**
   * This method takes a Vector of privileges as parameter and creates resp. updates
   * them in the persistent datastore.
   *
   * @param privList   Vector containing privilege objects
   */
  public void loadPrivileges(Vector privList) throws Exception
  {
    if (!MCRUserMgr.instance().isLocked())
    {
      this.privileges = privList;

      if (mcrUserStore.existsPrivilegeSet())
        mcrUserStore.updatePrivilegeSet(this);
      else mcrUserStore.createPrivilegeSet(this);
    }
    else
      throw new MCRException("The user component is locked. At the moment write access is denied.");
  }

  /**
   * @returns
   *   This method returns a Vector of strings containing all names of the
   *   privileges of the system.
   */
  public Vector getPrivileges()
  { return privileges; }

  /**
   * @return
   *   This method returns the privilege set object as a DOM document.
   */
  public synchronized Document toJDOMDocument() throws Exception
  {
    Element root = new Element("mcr_userobject");
    root.setAttribute("type", "privilege");

    for (int i=0; i<privileges.size(); i++) {
      MCRPrivilege currentPriv = (MCRPrivilege)privileges.elementAt(i);
      root.addContent(currentPriv.toJDOMElement());
    }

    Document jdomDoc = new Document(root);
    return jdomDoc;
  }

  /**
   * This helper method replaces null with an empty string and trims whitespace
   * from non-null strings.
   */
  private static String trim(String s)
  { return (s != null) ? s.trim() : ""; }
}

