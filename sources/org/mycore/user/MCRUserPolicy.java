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

/**
 * This class defines the policies of the MyCoRe user and group objects such as
 * required fields or password policy. It is implemented as a singleton since
 * there must not be two instances of this class.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserPolicy
{
  /** Vector with Strings indicating required user fields */
  private Vector reqUserAttributes = null;

  /** Vector with Strings indicating required group fields */
  private Vector reqGroupAttributes = null;

  /** The one and only instance of this class */
  private static MCRUserPolicy theInstance = null;

  /** private constructor to create the singleton instance. */
  private MCRUserPolicy() throws Exception
  {
    // For the moment this is hard coded but this will change soon....
    reqUserAttributes = new Vector();
    reqUserAttributes.add("userID");
    reqUserAttributes.add("password");
    reqUserAttributes.add("id_enabled");
    reqUserAttributes.add("update_allowed");
    reqUserAttributes.add("creator");
    //reqUserAttributes.add("description");
    reqUserAttributes.add("salutation");
    reqUserAttributes.add("firstname");
    reqUserAttributes.add("lastname");
    reqUserAttributes.add("telephone");
    reqUserAttributes.add("email");
    reqUserAttributes.add("primary_group");

    reqGroupAttributes = new Vector();
    reqGroupAttributes.add("groupID");
    reqGroupAttributes.add("creator");
  }

  /**
   * This method is the only way to get an instance of this class. It calls the
   * private constructor to create the singleton.
   *
   * @return
   *   returns the one and only instance of <CODE>MCRUserPolicy</CODE>
   */
  public final static synchronized MCRUserPolicy instance() throws Exception
  {
    if (theInstance == null)
      theInstance = new MCRUserPolicy();
    return theInstance;
  }

  /**
   * This method returns true if the given field is a required user attribute.
   * @param required
   *   string value representing a user attribute to check whether it is required
   */
  public boolean isRequiredForUser(String required)
  { return (reqUserAttributes.contains(required)) ? true : false; }

  /**
   * This method returns true if the given field is a required group attribute.
   * @param required
   *   string value representing a group attribute to check whether it is required
   */
  public boolean isRequiredForGroup(String required)
  { return (reqGroupAttributes.contains(required)) ? true : false; }

  /**
   * @return
   *   This method returns a vector of strings with the names of required
   *   user attributes.
   */
  public Vector getRequiredUserAttributes()
  { return reqUserAttributes; }

  /**
   * @return
   *   This method returns a vector of strings with the names of required
   *   group attributes.
   */
  public Vector getRequiredGroupAttributes()
  { return reqGroupAttributes; }
}
