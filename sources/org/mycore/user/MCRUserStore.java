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

import java.io.IOException;
import java.util.Vector;
import mycore.common.MCRException;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe user and group
 * information must implement this interface. Which database actually will be
 * used can then be configured by reading the value
 * <code>MCR.userstore_class_name</code> from mycore.properties.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public interface MCRUserStore
{
  /**
   * This method creates a MyCoRe user object in the persistent datastore.
   * @param newUserID    a String representing the user ID of the new user
   * @param newUser      the new user object to be stored
   */
  public void createUser(String newUserID, MCRUser newUser) throws MCRException, IOException, Exception;

  /**
   * This method creates a MyCoRe group object in the persistent datastore.
   * @param newGroupID   a String representing the group ID of the new group
   * @param newGroup     the new group object to be stored
   */
  public void createGroup(String newGroupID, MCRGroup newGroup) throws MCRException, IOException, Exception;

  /**
   * This method creates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet the privilege set object
   */
  public void createPrivilegeSet(MCRPrivilegeSet privilegeSet) throws MCRException, IOException;

  /**
   * This method deletes a MyCoRe user object in the persistent datastore.
   * @param delUserID    a String representing the MyCoRe user object which is to be deleted
   */
  public void deleteUser(String delUserID) throws MCRException, IOException;

  /**
   * This method deletes a MyCoRe group object in the persistent datastore.
   * @param delGroupID   a String representing the MyCoRe group object which is to be deleted
   */
  public void deleteGroup(String delGroupID) throws MCRException, IOException;

  /**
   * This method tests if a MyCoRe user object is available in the persistent datastore.
   * @param userID       a String representing the MyCoRe user object which is to be looked for
   */
  public boolean existsUser(String userID) throws MCRException, IOException;

  /**
   * This method tests if a MyCoRe group object is available in the persistent datastore.
   * @param groupID      a String representing the MyCoRe group object which is to be looked for
   */
  public boolean existsGroup(String groupID) throws MCRException, IOException;

  /**
   * This method tests if a MyCoRe privilege set object is available in the persistent datastore.
   */
  public boolean existsPrivilegeSet() throws MCRException, IOException;

  /**
   * This method gets all user IDs and returns them as a Vector of strings.
   * @return   Vector of strings including the user IDs of the system
   */
  public Vector getAllUserIDs() throws MCRException, IOException;

  /**
   * This method gets all group IDs and returns them as a Vector of strings.
   * @return   Vector of strings including the group IDs of the system
   */
  public Vector getAllGroupIDs() throws MCRException, IOException;

  /**
   * This method retrieves a MyCoRe user object from the persistent datastore.
   * @param userID     a String representing the MyCoRe user object which is to be retrieved
   * @return           the requested user object
   */
  public MCRUser retrieveUser(String userID)
                 throws MCRException, IOException, Exception;

  /**
   * This method retrieves a MyCoRe privilege set object from the persistent datastore.
   * @return  the requested privilege set object as XML string representation
   */
  public String retrievePrivilegeSet() throws MCRException, IOException;

  /**
   * This method retrieves a MyCoRe group object from the persistent datastore.
   * @param groupID    a String representing the MyCoRe group object which is to be retrieved
   * @return           the requested group object
   */
  public MCRGroup retrieveGroup(String groupID)
                  throws MCRException, IOException, Exception;

  /**
   * This method updates a MyCoRe group object in the persistent datastore.
   * @param groupID    a String representing the MyCoRe group object which is to be updated
   * @param group      the group to be updated
   */
  public void updateGroup(String groupID, MCRGroup group) throws MCRException, IOException, Exception;

  /**
   * This method updates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet the privilege set object to be updated
   */
  public void updatePrivilegeSet(MCRPrivilegeSet privilegeSet) throws MCRException, IOException;

  /**
   * This method updates a MyCoRe user object in the persistent datastore.
   * @param userID     a String representing the MyCoRe user object which is to be updated
   * @param user       the user to be updated
   */
  public void updateUser(String userID, MCRUser user) throws MCRException, IOException, Exception;
}


