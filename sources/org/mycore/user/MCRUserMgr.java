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
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import mycore.common.MCRCache;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.datamodel.MCRParserInterface;

/**
 * This class is the user (and group) manager of the MyCoRe system. It is implemented
 * using the singleton design pattern in order to ensure that there is only one instance
 * of this class, i.e. one user manager, running. The class also serves as a facade
 * to all the other classes of the MyCoRe user component. Hence, it is the only class
 * other components of MyCoRe need to know. Clients who need to perform any action
 * with users or groups such as creating a new user, logging in to the system, setting
 * passwords etc. will use the public methods of this class and nothing else.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserMgr
{
  /** file separator, read from system properties */
  private String SLASH = "";

  /** the user cache */
  private MCRCache userCache;

  /** the group cache */
  private MCRCache groupCache;

  /** the XML parser we are using (configurable) */
  private MCRParserInterface mcrParser;

  /** the class responsible for persistent datastore (configurable ) */
  private MCRUserStore mcrUserStore;

  /** the DOM document, used for creation of users etc. */
  private Document mcrDocument = null;

  /** The one and only instance of this class */
  private static MCRUserMgr theInstance = null;

  /**
   * private constructor to create the singleton instance.
   */
  private MCRUserMgr() throws MCRException, Exception
  {
    SLASH = new String((System.getProperties()).getProperty("file.separator"));
    String userStoreName = MCRConfiguration.instance().getString("MCR.userstore_class_name");
    mcrUserStore = (MCRUserStore)Class.forName(userStoreName).newInstance();

    String parserName = MCRConfiguration.instance().getString("MCR.parser_class_name");
    mcrParser = (MCRParserInterface)Class.forName(parserName).newInstance();

    userCache  = new MCRCache(10);  // ok, this will be read from mycore.properties later
    groupCache = new MCRCache(10);  //                   -"-
  }

  /**
   * This method is the only way to get an instance of this class. It calls the
   * private constructor to create the singleton.
   *
   * @return returns the one and only instance of <CODE>MCRUserMgr</CODE>
   */
  public final static synchronized MCRUserMgr instance() throws Exception
   {
     if (theInstance == null)
       theInstance = new MCRUserMgr();
     return theInstance;
   }

  /**
   * This method tests the consistency between the user and group objects. Users are
   * members of groups and groups have members (users and other groups). Sometimes
   * (for example when a user/group system is reloaded from xml files) the consistency
   * of this information should be checked.
   *
   * At the moment failures will be printed to standard output. Later this will be
   * redirected to a logging service.
   */
  public final void checkUserGroupConsistency() throws Exception
  {
    // I'm sure there is some tricky algorithm which saves time in doing the checks in
    // this method. But since this method will not be invoked very often I just don't
    // care, at least for the moment. For the same reason I don't care that the user and
    // group caches will be updated unneccessarily.

    Vector userIDs  = null;
    Vector groupIDs = null;

    // At first we check that all the groups listed in a given user object really know him/her.
    userIDs = new Vector(mcrUserStore.getAllUserIDs());
    for (int i=0; i<userIDs.size(); i++)
    {
      MCRUser checkUser = getUserFromCacheOrDB((String)userIDs.elementAt(i));
      groupIDs = new Vector(checkUser.getGroups());
      for (int j=0; j<groupIDs.size(); j++)
      {
        MCRGroup checkGroup = getGroupFromCacheOrDB((String)groupIDs.elementAt(j));
        Vector groupMembers = new Vector(checkGroup.getUsers());

        if (!groupMembers.contains(userIDs.elementAt(i)))
          System.out.println("User with ID \""+userIDs.elementAt(i)+"\" claims to be a member "+
                             "of group \""+groupIDs.elementAt(j)+"\" but this group does not "+
                             "know him or her!");
      }
    }

    // Now we check whether all the members of a given group really do know that they are members.
    groupIDs = new Vector(mcrUserStore.getAllGroupIDs());
    for (int i=0; i<groupIDs.size(); i++)
    {
      MCRGroup checkGroup = getGroupFromCacheOrDB((String)groupIDs.elementAt(i));
      userIDs = new Vector(checkGroup.getUsers());
      for (int j=0; j<userIDs.size(); j++)
      {
        MCRUser checkUser = getUserFromCacheOrDB((String)userIDs.elementAt(j));
        Vector userGroups = new Vector(checkUser.getGroups());

        if (!userGroups.contains(groupIDs.elementAt(i)))
          System.out.println("Group with ID \""+groupIDs.elementAt(i)+"\" claims that the user "+
                             "with ID \""+userIDs.elementAt(j)+"\" is a member but this user is not "+
                             "aware of that!");
      }
    }
  }

  /**
   * deletes a group from the datastore (and the cache as well)
   * @param groupID the group ID which will be deleted
   */
  public final synchronized void deleteGroup(String groupID) throws Exception
  {
    // At the moment the permission to delete a group is not checked...
    if (mcrUserStore.existsGroup(groupID))
    {
      mcrUserStore.deleteGroup(groupID);
      groupCache.remove(groupID);
    }
    else
      throw new MCRException("MCRUserMgr.deleteGroup(): Group unknown!");
  }

  /**
   * deletes a user from the datastore (and the cache as well)
   * @param userID the user ID which will be deleted
   */
  public final synchronized void deleteUser(String userID) throws Exception
  {
    // At the moment the permission to delete a user is not checked...
    if (mcrUserStore.existsUser(userID))
    {
      mcrUserStore.deleteUser(userID);
      userCache.remove(userID);
    }
    else
      throw new MCRException("MCRUserMgr.deleteUser(): User unknown!");
  }

  /**
   * This method gets all group IDs from the persistent datastore and returns them as a
   * Vector of strings.
   *
   * @return   Vector of strings including the group IDs of the system
   */
  public final Vector getAllGroupIDs() throws Exception
  { return mcrUserStore.getAllGroupIDs(); }

  /**
   * This method gets all user IDs from the persistent datastore and returns them as a
   * Vector of strings.
   *
   * @return   Vector of strings including the user IDs of the system
   */
  public final Vector getAllUserIDs() throws Exception
  { return mcrUserStore.getAllUserIDs(); }

  /**
   * returns an XML representation of a given group
   *
   * @param groupID group ID for which the XML representation is needed
   * @param NL new line character or sequence to separate the XML elements. This
   *           may be an empty string.
   * @return returns an XML representation of a group
   */
  public String getGroupAsXML(String groupID, String NL) throws Exception
  {
    MCRGroup reqGroup = getGroupFromCacheOrDB(groupID);
    return reqGroup.getGroupAsXML(NL);
  }

  /**
   * Returns information about the group cache as a formatted string - ready for
   * printing it with System.out.println() or so.
   *
   * @return returns information about the group cache as a formatted string
   */
  public final String getGroupCacheInfo()
  { return groupCache.toString(); }

  /**
   * This method returns the group information of a specified group.
   *
   * @param groupID   the group for which the group information is requested
   * @return group information, all in one string
   */
  public String getGroupInfo(String groupID) throws Exception
  {
    MCRGroup reqGroup = getGroupFromCacheOrDB(groupID);
    StringBuffer sb = new StringBuffer();
    sb.append("group ID         : ").append(reqGroup.getGroupID()).append("\n");

    sb.append("privileges       : ");
    Vector privileges = new Vector(reqGroup.getPrivileges());
    for (int i=0; i<privileges.size(); i++) {
      sb.append(privileges.elementAt(i)).append(",");
    }
    sb.append("\n");
    sb.append("members [groups] : ");
    Vector groups = new Vector(reqGroup.getGroups());
    for (int i=0; i<groups.size(); i++) {
      sb.append(groups.elementAt(i)).append(",");
    }
    sb.append("\n");
    sb.append("members [users]  : ");
    Vector users = new Vector(reqGroup.getUsers());
    for (int i=0; i<users.size(); i++) {
      sb.append(users.elementAt(i)).append(",");
    }
    sb.append("\n");
    return sb.toString();
  }

  /**
   * returns an XML representation of the user
   *
   * @param userID user ID for which the XML representation is needed
   * @param NL new line character or sequence to separate the XML elements. This
   *           may be an empty string.
   * @return returns an XML representation of the user
   */
  public String getUserAsXML(String userID, String NL) throws Exception
  {
    MCRUser reqUser = getUserFromCacheOrDB(userID);
    return reqUser.getUserAsXML(NL);
  }

  /**
   * Returns information about the user cache as a formatted string - ready for
   * printing it with System.out.println() or so.
   *
   * @return returns information about the user cache as a formatted string
   */
  public final String getUserCacheInfo()
  { return userCache.toString(); }

  /**
   * This method returns the user information of a specified user. The password
   * will not be returned.
   *
   * @param userID   the user for which the user information is requested
   * @return user information, all in one string
   */
  public String getUserInfo(String userID) throws Exception
  {
    MCRUser reqUser = getUserFromCacheOrDB(userID);
    StringBuffer sb = new StringBuffer();
    sb.append("user ID       : ").append(reqUser.getUserID()).append("\n");
    sb.append("creation date : ").append(reqUser.getCreationDateAsString()).append("\n");
    sb.append(reqUser.getAddress()).append("\n");
    sb.append("groups        : ");

    Vector groups = new Vector(reqUser.getGroups());
    for (int i=0; i<groups.size(); i++) {
      sb.append(groups.elementAt(i)).append(",");
    }
    sb.append("\n");
    return sb.toString();
  }

  /**
   * This method expects a string as a parameter which represents a normal file or a
   * directory. From this file (or from the files in the directory) user or group
   * information is loaded. The file(s) containing user or group information must
   * be xml-files (must have the extension .xml). However, this method does not really
   * extract the user or group information from the XML file(s). It rather detects
   * whether the given parameter directly is an XML file or a directory, checks the
   * extension and then invokes loadUsersOrGroupsFromXMLFile() for any .xml-file found.
   *
   * @param filename              name of file or directory
   * @exception MCRException      general MyCoRe exception
   */
  public final void loadUsersOrGroupsFromFile(String filename) throws Exception
  {
    int fnLength;  // Length of the file name
    File inFile = new File(filename);

    if (inFile.isDirectory())
    {
      // Ok, we found a directory and now expect one or more .xml-files with
      // user or group information inside.

      String [] fileList = inFile.list();
      if (fileList.length == 0)
        throw new MCRException("MCRUserMgr.loadUsersOrGroupsFromFile : The given "+
                               "directory is empty!");

      int xmlFileCounter = 0;  // counts number of .xml-files in the directory
      for (int i=0; i<fileList.length; i++)
      {
        // The files containing the user or group information must have the
        // extension .xml

        fnLength = fileList[i].length();
        if (fileList[i].substring(fnLength-4, fnLength).equals(".xml"))
        {
          xmlFileCounter++;
          String uri = filename+SLASH+fileList[i];
          loadUsersOrGroupsFromXMLFile(uri);
        }
      }
      if (xmlFileCounter == 0)
        throw new MCRException("MCRUserMgr.loadUsersOrGroupsFromFile : The given "+
                               "directory contains no .xml file!");

      return;  // All .xml-files in the given directory are read.
    }  // No, the given parameter is *not* a directory...

    if (inFile.isFile() && filename.substring(filename.length()-4, filename.length()).equals(".xml"))
      loadUsersOrGroupsFromXMLFile(filename);
    else
      throw new MCRException("MCRUserMgr.loadUsersOrGroupsFromFile : file not "+
                             "valid or not existent!");
  }

  /**
   * login to the system
   *
   * @param userID user ID for the login
   * @param passwd password for the user
   * @return true if the password matches the password stored, false otherwise
   */
  public boolean login(String userID, String passwd) throws Exception
  {
    MCRUser loginUser = getUserFromCacheOrDB(userID);

    // Now check the password...
    // For the moment we have clear text passwords...
    if (loginUser.getPassword().equals(passwd))
      return true;
    else
      return false;
  }

  /**
   * sets the password of a user
   *
   * @param userID user ID to set the password for
   * @param newPassword the new password
   */
  public final void setPassword(String userID, String newPassword) throws Exception
  {
    // At the moment the permission to set the password of a user is not checked...
    MCRUser user = getUserFromCacheOrDB(userID);
    user.setPassword(newPassword);
    updateUser(userID);
  }

  /**
   * This method first looks for a given groupID in the group cache and returns this
   * group object. In case that the group object is not in the cache, the group will
   * be retrieved from the database. Then the group object is put into the cache.
   *
   * @param groupID    string representing the requested group object
   * @return MCRGroup  group object (if available)
   * @exception MCRException  if group object is not known
   */
  private MCRGroup getGroupFromCacheOrDB (String groupID) throws Exception
  {
    MCRGroup reqGroup = (MCRGroup)groupCache.get(groupID);
    if (reqGroup == null)
    {
      // We do not have this group in the cache. Hence we retrieve it from
      // the persistent datastore as an XML Stream. The group object will be
      // created and put into the cache.

      String reqGroupXML = mcrUserStore.retrieveGroup(groupID);
      if (reqGroupXML == null)
        throw new MCRException("Unknown group!");
      else
      {
        mcrDocument = mcrParser.parseXML(reqGroupXML);
        NodeList domGroupList = mcrDocument.getElementsByTagName("group");
        reqGroup = new MCRGroup((Element)domGroupList.item(0));
        groupCache.put(groupID, reqGroup);
        return reqGroup;
      }
    }
    else
      return reqGroup;
  }

  /**
   * This method first looks for a given userID in the user cache and returns this
   * user object. In case that the user object is not in the cache, the user will
   * be retrieved from the database. Then the user object is put into the cache.
   *
   * @param userID    string representing the requested user object
   * @return MCRUser  user object (if available)
   * @exception MCRException  if user object is not known
   */
  private MCRUser getUserFromCacheOrDB (String userID) throws Exception
  {
    MCRUser reqUser = (MCRUser)userCache.get(userID);
    if (reqUser == null)
    {
      // We do not have this user in the cache. Hence we retrieve him or her
      // from the persistent datastore as an XML Stream. The user object will be
      // created and put into the cache.

      String reqUserXML = mcrUserStore.retrieveUser(userID);
      if (reqUserXML == null)
        throw new MCRException("Unknown user!");
      else
      {
        mcrDocument = mcrParser.parseXML(reqUserXML);
        NodeList domUserList = mcrDocument.getElementsByTagName("user");
        reqUser = new MCRUser((Element)domUserList.item(0));
        userCache.put(userID, reqUser);
        return reqUser;
      }
    }
    else
      return reqUser;
  }

  /**
   * This method is invoked by loadUsersOrGroupsFromFile(). It takes a filename (String) as
   * a parameter and expects that this file is an XML file. The actual XML-Parser of
   * the MyCoRe system is invoked and the file is parsed. User Objects (instances of
   * MCRUser) or group objects (instances of MCRGroup) are created using the information
   * provided by the XML files. The objects are stored in the persistent user datastore
   * and the user or group cache as well.
   *
   * @param filename              name of the XML-file
   * @exception MCRException      general MyCoRe exception
   */
  private final void loadUsersOrGroupsFromXMLFile(String filename) throws Exception
  {
    System.out.print("Reading file : "+filename+"\n");  // output only during the debugging phase!
    mcrDocument = mcrParser.parseURI(filename);

    // At first we check whether we are loading users or groups. This information is provided
    // by the XML-Representation (attribute of the first element).

    NodeList domElementList = mcrDocument.getElementsByTagName("mycore_user_and_group_info");
    Element domElement = (Element)domElementList.item(0);
    String infoType = domElement.getAttribute("type").trim();

    if (infoType.equals("user"))
    {
      MCRUser newUser;
      NodeList domUserList = mcrDocument.getElementsByTagName("user");
      int iNumUsers = domUserList.getLength();
      System.out.println("Number of users to load: " + iNumUsers);

      // Create all users found in the XML file and put them into the cache and
      // eventually into the datastore as well.

      for (int i=0; i<iNumUsers; i++)
      {
        newUser = new MCRUser((Element)domUserList.item(i));

        // Check if user already exists in the datastore. If yes, notify the
        // administrator who read in the user file. In later stages of the software
        // development we may examine if the new user differs from the persistent
        // user object.

        if (!mcrUserStore.existsUser(newUser.getUserID())) {
          mcrUserStore.createUser(newUser.getUserID(), newUser.getUserAsXML(""));
          userCache.put(newUser.getUserID(), newUser);
        }
        else { // Later this will go to some logging mechanism rather then to stdout
          System.out.println("\nUser " + newUser.getUserID() + " already exists!");
          System.out.println("If you want to overwrite the existent user object you must first delete it.");
        }
      }// end for
    }// end if
    else if (infoType.equals("group"))
    {
      MCRGroup newGroup;
      NodeList domGroupList = mcrDocument.getElementsByTagName("group");
      int iNumGroups = domGroupList.getLength();
      System.out.println("Number of groups to load: " + iNumGroups);

      // Create all groups found in the XML file and put them into the cache and
      // eventually into the datastore as well.

      for (int i=0; i<iNumGroups; i++)
      {
        newGroup = new MCRGroup((Element)domGroupList.item(i));

        // Check if the group already exists in the datastore. If yes, notify the
        // administrator who read in the group file. In later stages of the software
        // development we may examine if the new group differs from the persistent
        // group object.

        if (!mcrUserStore.existsGroup(newGroup.getGroupID())) {
          mcrUserStore.createGroup(newGroup.getGroupID(), newGroup.getGroupAsXML(""));
          groupCache.put(newGroup.getGroupID(), newGroup);
        }
        else { // Later this will go to some logging mechanism rather then to stdout
          System.out.println("\nGroup " + newGroup.getGroupID() + " already exists!");
          System.out.println("If you want to overwrite the existent group object you must first delete it.");
        }
      }// end for
    }// end if
    else
      throw new MCRException("MCRUserMgr.loadUsersOrGroupsFromXMLFile : The type attribute"+
                             "of <mycore_user_and_group_info> is incorrect!");
  }

  /**
   * updates a group in the datastore (and the cache as well)
   * @param groupID the ID of the group object which will be updated
   */
  private final synchronized void updateGroup(String groupID) throws Exception
  {
    // At the moment the permission to update a group is not checked...
    mcrUserStore.updateGroup(groupID, getGroupAsXML(groupID, ""));
  }

  /**
   * updates a user in the datastore (and the cache as well)
   * @param userID the ID of the user object which will be updated
   */
  private final synchronized void updateUser(String userID) throws Exception
  {
    // At the moment the permission to update a user is not checked...
    mcrUserStore.updateUser(userID, getUserAsXML(userID, ""));
  }
}