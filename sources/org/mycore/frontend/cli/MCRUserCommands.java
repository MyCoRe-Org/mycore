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

package mycore.commandline;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import mycore.common.*;
import mycore.user.*;
import org.w3c.dom.DOMException;

/**
 * This class provides a set of commands for the mycore user management which
 * can be used by the command line interface.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRUserCommands
{
  /**
   * This method invokes MCRUserMgr.deleteGroup() and permanently removes
   * a group from the system.
   *
   * @param groupID the ID of the group which will be deleted
   */
  public static void deleteGroup(String groupID) throws Exception
  {
    MCRUserMgr.instance().deleteGroup(groupID);
    System.out.println("Group ID " + groupID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.deleteUser() and permanently removes
   * a user from the system.
   *
   * @param userID the ID of the user which will be deleted
   */
  public static void deleteUser(String userID) throws Exception
  {
    MCRUserMgr.instance().deleteUser(userID);
    System.out.println("User ID " + userID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a vector
   * of all users stored in the persistent datastore.
   */
  public static void listAllUsers() throws Exception
  {
    Vector users = new Vector(MCRUserMgr.instance().getAllUserIDs());
    System.out.println();
    for (int i=0; i<users.size(); i++)
      System.out.println(users.elementAt(i));
  }

  /**
   * This method invokes MCRUserMgr.getAllGroupIDs() and retrieves a vector
   * of all groups stored in the persistent datastore.
   */
  public static void listAllGroups() throws Exception
  {
    Vector groups = new Vector(MCRUserMgr.instance().getAllGroupIDs());
    System.out.println();
    for (int i=0; i<groups.size(); i++)
      System.out.println(groups.elementAt(i));
  }

  /**
   * This method invokes MCRPrivilegeSet.getPrivileges() and retrieves a vector
   * of all privileges stored in the persistent datastore.
   */
  public static void listAllPrivileges() throws Exception
  {
    MCRPrivilege thePrivilege;
    Vector privileges = new Vector(MCRPrivilegeSet.instance().getPrivileges());
    System.out.println();

    for (int i=0; i<privileges.size(); i++) {
      thePrivilege = (MCRPrivilege)privileges.elementAt(i);
      System.out.println(thePrivilege.getName());
      System.out.println("    " + thePrivilege.getDescription() +"\n");
    }
  }

  /**
   * This method invokes MCRUserMgr.loadFromFile() with a parameter that tells
   * the user manager that these objects have to be created (not updated).
   *
   * @param filename name of a file or directory containing user or group
   *                 information in XML files
   */
  public static void loadFromFile(String filename) throws Exception
  {
    System.out.println("Loading data from file/directory: "+filename);
    MCRUserMgr.instance().loadFromFile(filename, "create");
  }

  /**
   * This method invokes MCRUserMgr.saveAllGroupsToXMLFile() and saves all groups
   * in XML representation to the given file.
   *
   * @param fileName Name of the file the groups will be saved to
   */
  public static void saveAllGroupsToFile(String fileName)
                     throws IOException, Exception
  { MCRUserMgr.instance().saveAllGroupsToXMLFile(fileName); }

  /**
   * This method invokes MCRUserMgr.saveAllUsersToXMLFile() and saves all users
   * in XML representation to the given file.
   *
   * @param fileName Name of the file the users will be saved to
   */
  public static void saveAllUsersToFile(String fileName)
                     throws IOException, Exception
  { MCRUserMgr.instance().saveAllUsersToXMLFile(fileName); }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to change the password.
   *
   * @param userID the ID of the user for which the password will be set
   */
  public static void setPassword(String userID, String password) throws Exception
  {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
    user.setPassword(password);
  }

  /** This method invokes MCRUserMgr.getGroupCacheInfo() */
  public static void showGroupCacheInfo() throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getGroupCacheInfo()); }

  /**
   * This method invokes MCRUserMgr.retrieveGroup() and then works with the
   * retrieved group object to get the formatted group information data.
   *
   * @param groupID the ID of the group for which the group information is needed
   */
  public static void showGroupInfo(String groupID) throws Exception
  {
    MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
    System.out.println("\n"+group.getFormattedInfo());
  }

  /** This method invokes MCRUserMgr.getUserCacheInfo() */
  public static void showUserCacheInfo() throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getUserCacheInfo()); }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to get the formatted user information data.
   *
   * @param userID the ID of the user for which the user information is needed
   */
  public static void showUserInfo(String userID) throws Exception
  {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
    System.out.println("\n"+user.getFormattedInfo());
  }

  /**
   * This method invokes MCRUserMgr.loadFromFile() with a parameter that tells
   * the user manager that these objects have to be updated (not created).
   *
   * @param filename name of a file or directory containing user or group
   *                 information in XML files
   */
  public static void updateFromFile(String filename) throws Exception
  {
    System.out.println("Updating data from file/directory: "+filename);
    MCRUserMgr.instance().loadFromFile(filename, "update");
  }
}
