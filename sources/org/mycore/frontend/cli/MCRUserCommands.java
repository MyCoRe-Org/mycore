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
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRUserCommands
{
  /**
   * This method invokes MCRUserMgr.deleteGroup()
   * @param groupID the ID of the group which will be deleted
   */
  public static void deleteGroup(String groupID) throws Exception
  {
    MCRUserMgr.instance().deleteGroup(groupID);
    System.out.println("Group ID " + groupID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.deleteUser()
   * @param userID the ID of the user which will be deleted
   */
  public static void deleteUser(String userID) throws Exception
  {
    MCRUserMgr.instance().deleteUser(userID);
    System.out.println("User ID " + userID + " deleted!");
  }

  /** This method invokes MCRUserMgr.getAllUserIDs() */
  public static void listAllUsers() throws Exception
  {
    Vector users = new Vector(MCRUserMgr.instance().getAllUserIDs());
    System.out.println();
    for (int i=0; i<users.size(); i++)
      System.out.println(users.elementAt(i));
  }

  /** This method invokes MCRUserMgr.getAllGroupIDs() */
  public static void listAllGroups() throws Exception
  {
    Vector groups = new Vector(MCRUserMgr.instance().getAllGroupIDs());
    System.out.println();
    for (int i=0; i<groups.size(); i++)
      System.out.println(groups.elementAt(i));
  }

  /**
   * This method invokes MCRUserMgr.loadUsersFromFile()
   * @param filename name of a file or directory containing user or group
   *                 information in XML files
   */
  public static void loadFromFile(String filename) throws Exception
  {
    System.out.println("Loading user data from file/directory: "+filename);
    MCRUserMgr.instance().loadUsersOrGroupsFromFile(filename);
  }

  /**
   * This method invokes MCRUserMgr.retrieveGroup() and then works with the
   * retrieved group object to get an XML-Representation.
   *
   * @param groupID the ID of the group for which the XML-representation is needed
   */
  public static void printGroupAsXML(String groupID) throws Exception
  {
    MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
    System.out.println("\n"+group.getGroupAsXML("\n"));
  }

  /**
   * This method invokes MCRUserMgr.retrieveGroup() and then works with the
   * retrieved group object to get the formatted group information data.
   *
   * @param groupID the ID of the group for which the group information is needed
   */
  public static void printGroupInfo(String groupID) throws Exception
  {
    MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
    System.out.println("\n"+group.getFormattedInfo());
  }

  /** This method invokes MCRUserMgr.getGroupCacheInfo() */
  public static void printGroupCacheInfo() throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getGroupCacheInfo()); }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to get an XML-Representation.
   *
   * @param userID the ID of the user for which the XML-representation is needed
   */
  public static void printUserAsXML(String userID) throws Exception
  {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
    System.out.println("\n"+user.getUserAsXML("\n"));
  }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to get the formatted user information data.
   *
   * @param userID the ID of the user for which the user information is needed
   */
  public static void printUserInfo(String userID) throws Exception
  {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
    System.out.println("\n"+user.getFormattedInfo());
  }

  /** This method invokes MCRUserMgr.getUserCacheInfo() */
  public static void printUserCacheInfo() throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getUserCacheInfo()); }

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
}

