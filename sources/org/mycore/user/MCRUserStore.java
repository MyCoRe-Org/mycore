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

import java.io.IOException;
import java.util.Vector;
import org.mycore.common.MCRException;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe user, group and
 * privilege information must implement this interface. Which database actually
 * will be used can then be configured by reading the value
 * <code>MCR.userstore_class_name</code> from mycore.properties.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public interface MCRUserStore
{
  /**
   * This method creates a MyCoRe user object in the persistent datastore.
   * @param newUser  the new user object to be stored
   */
  public void createUser(MCRUser newUser) throws Exception;

  /**
   * This method creates a MyCoRe group object in the persistent datastore.
   * @param newGroup  the new group object to be stored
   */
  public void createGroup(MCRGroup newGroup) throws Exception;

  /**
   * This method creates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet  the privilege set object
   */
  public void createPrivilegeSet(MCRPrivilegeSet privilegeSet) throws Exception;

  /**
   * This method deletes a MyCoRe user object in the persistent datastore.
   * @param delUserID  a String representing the MyCoRe user object which is to be deleted
   */
  public void deleteUser(String delUserID) throws Exception;

  /**
   * This method deletes a MyCoRe group object in the persistent datastore.
   * @param delGroupID  a String representing the MyCoRe group object which is to be deleted
   */
  public void deleteGroup(String delGroupID) throws Exception;

  /**
   * This method tests if a MyCoRe user object is available in the persistent datastore.
   * @param userID  a String representing the MyCoRe user object which is to be looked for
   */
  public boolean existsUser(String userID) throws Exception;

  /**
   * This method tests if a MyCoRe user object is available in the persistent datastore. The
   * numerical userID is taken into account, too.
   *
   * @param numID         (int) numerical userID of the MyCoRe user object
   * @param userID  a String representing the MyCoRe user object which is to be looked for
   */
  public boolean existsUser(int numID, String userID) throws Exception;

  /**
   * This method tests if a MyCoRe group object is available in the persistent datastore.
   * @param groupID  a String representing the MyCoRe group object which is to be looked for
   */
  public boolean existsGroup(String groupID) throws Exception;

  /**
   * This method tests if a MyCoRe privilege object is available in the persistent datastore.
   * @param privName  a String representing the MyCoRe privilege object which is to be looked for
   */
  public boolean existsPrivilege(String privName) throws Exception;

  /**
   * This method tests if a MyCoRe privilege set object is available in the persistent datastore.
   */
  public boolean existsPrivilegeSet() throws Exception;

  /**
   * This method gets all user IDs and returns them as a Vector of strings.
   * @return  Vector of strings including the user IDs of the system
   */
  public Vector getAllUserIDs() throws Exception;

  /**
   * This method gets all group IDs and returns them as a Vector of strings.
   * @return  Vector of strings including the group IDs of the system
   */
  public Vector getAllGroupIDs() throws Exception;

  /**
   * This method gets all group IDs where a given user ID can manage the group (i.e. is
   * in the administrator user IDs list) as a vector of strings.
   *
   * @param userID   a String representing the administrative user
   * @return         Vector of strings including the group IDs of the system which
   *                 have userID in their administrators list
   */
  public Vector getGroupIDsWithAdminUser(String userID) throws Exception;

  /**
   * This method gets all user IDs with a given primary group and returns them as a
   * Vector of strings.
   *
   * @param groupID  a String representing a primary Group
   * @return         Vector of strings including the user IDs of the system which
   *                 have groupID as primary group
   */
  public Vector getUserIDsWithPrimaryGroup(String groupID) throws Exception;

  /**
   * This method retrieves a MyCoRe user object from the persistent datastore.
   * @param userID     a String representing the MyCoRe user object which is to be retrieved
   * @return           the requested user object
   */
  public MCRUser retrieveUser(String userID) throws Exception;

  /**
   * This method retrieves a MyCoRe privilege set from the persistent datastore.
   * @return  the Vector of known privileges of the system
   */
  public Vector retrievePrivilegeSet() throws Exception;

  /**
   * This method retrieves a MyCoRe group object from the persistent datastore.
   * @param groupID    a String representing the MyCoRe group object which is to be retrieved
   * @return           the requested group object
   */
  public MCRGroup retrieveGroup(String groupID) throws Exception;

  /**
   * This method updates a MyCoRe group object in the persistent datastore.
   * @param group      the group to be updated
   */
  public void updateGroup(MCRGroup group) throws Exception;

  /**
   * This method updates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet the privilege set object to be updated
   */
  public void updatePrivilegeSet(MCRPrivilegeSet privilegeSet) throws Exception;

  /**
   * This method updates a MyCoRe user object in the persistent datastore.
   * @param user       the user to be updated
   */
  public void updateUser(MCRUser user) throws Exception;
