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
import mycore.xml.MCRXMLHelper;

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
   * creates a group in the datastore (and the cache as well)
   * @param group    The group object which will be created
   */
  public final synchronized void createGroup(MCRGroup group) throws Exception
  {
    // Check if the group already exists in the datastore. If yes, notify the creator
    // and write a message to a log file.
    if (!mcrUserStore.existsGroup(group.getID())) {
      mcrUserStore.createGroup(group.getID(), group);
      groupCache.put(group.getID(), group);
    }
    else { // Later this will go to some logging mechanism rather then to stdout
           // Or should we throw an exception?? (MCRPersistenceExeption)
      System.out.println("\nGroup " + group.getID() + " already exists!");
      System.out.println("If you want to overwrite the existent group object you must first delete it.");
    }
  }

  /**
   * creates a user in the datastore (and the cache as well)
   * @param user     The user object which will be created
   */
  public final synchronized void createUser(MCRUser user) throws Exception
  {
    // Check if user already exists in the datastore. If yes, notify the creator
    // and write a message to a log file.
    if (!mcrUserStore.existsUser(user.getID()))
    {
      // All of the groups this newly created user is a member of have to be updated.
      // However, in the case that we reload all user and groups from XML files
      // the group objects already know their members. Therefore we must check it.

      String userID = user.getID();
      mcrUserStore.createUser(userID, user);
      for (int i=0; i<user.getGroups().size(); i++) {
        MCRGroup updGroup = retrieveGroup((String)user.getGroups().elementAt(i));
        if (!updGroup.getUsers().contains(userID))
          updGroup.addUser(user.getID());
      }
      userCache.put(user.getID(), user);
    }
    else { // Later this will go to some logging mechanism rather then to stdout
      System.out.println("\nUser " + user.getID() + " already exists!");
      System.out.println("If you want to overwrite the existent user object you must first delete it.");
      throw new MCRException("User " + user.getID() + " already exists!");
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
    if (mcrUserStore.existsUser(userID))
    {
      // All of the groups this deleted user was a member of have to be updated
      MCRUser user = retrieveUser(userID);
      for (int i=0; i<user.getGroups().size(); i++) {
        MCRGroup updGroup = retrieveGroup((String)user.getGroups().elementAt(i));
        updGroup.removeUser(user.getID());
      }
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
  public final synchronized Vector getAllGroupIDs() throws Exception
  { return mcrUserStore.getAllGroupIDs(); }

  /**
   * This method gets all user IDs from the persistent datastore and returns them as a
   * Vector of strings.
   *
   * @return   Vector of strings including the user IDs of the system
   */
  public final synchronized Vector getAllUserIDs() throws Exception
  { return mcrUserStore.getAllUserIDs(); }

  /**
   * This method returns all users of the system as DOM document.
   * @return  DOM representation of all user objects
   */
  public final synchronized Document getAllUsersAsDOM() throws IOException, Exception
  {
    // Actually this method is not very well designed since we transform the data
    // from XML string representation to DOM representation back and forth. There
    // is much to much parsing involved. This has to be updated... Additionally
    // the method saveAllUsersToXMLFile() is very similar.

    MCRUser currentUser;
    Document userDoc;
    StringBuffer allUsersBuffer = new StringBuffer();
    allUsersBuffer.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n")
                  .append("<mycore_user_and_group_info type=\"user\">");

    Vector allUserIDs = mcrUserStore.getAllUserIDs();
    for (int i=0; i<allUserIDs.size(); i++)
    {
      currentUser = mcrUserStore.retrieveUser((String)allUserIDs.elementAt(i));
      userDoc = currentUser.toDOM();
      NodeList domUserList = userDoc.getElementsByTagName("user");
      StringBuffer sb = new StringBuffer();
      MCRXMLHelper.getNodeAsString(domUserList.item(0), "  ", sb);
      allUsersBuffer.append(sb.toString());
    }

    allUsersBuffer.append("\n</mycore_user_and_group_info>");
    return MCRXMLHelper.parseXML(allUsersBuffer.toString());
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
   * Returns information about the user cache as a formatted string - ready for
   * printing it with System.out.println() or so.
   *
   * @return returns information about the user cache as a formatted string
   */
  public final String getUserCacheInfo()
  { return userCache.toString(); }

  /**
   * This method returns the user store. This is used by the singleton MCRPrivilegeSet.
   * @return   returns the user store
   */
  public final MCRUserStore getUserStore()
  { return mcrUserStore; }

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
  public synchronized boolean login(String userID, String passwd) throws Exception
  {
    MCRUser loginUser = retrieveUser(userID);

    // Now check the password...
    // For the moment we have clear text passwords...
    if (loginUser.getPassword().equals(passwd))
      return true;
    else
      return false;
  }

  /**
   * This method first looks for a given groupID in the group cache and returns this
   * group object. In case that the group object is not in the cache, the group will
   * be retrieved from the database. Then the group object is put into the cache.
   *
   * @param groupID    string representing the requested group object
   * @param bFromDataStore  group must be retrieved directly from the data store
   * @return MCRGroup  group object (if available)
   * @exception MCRException  if group object is not known
   */
  public MCRGroup retrieveGroup(String groupID) throws Exception
  { return this.retrieveGroup(groupID, false); }

  public synchronized MCRGroup retrieveGroup (String groupID, boolean bFromDataStore) throws Exception
  {
    // In order to compare a modified group object with the persistent one we must
    // be able to force this method to get the group from the store
    MCRGroup reqGroup;
    if (bFromDataStore)
      reqGroup = null;
    else
      reqGroup = (MCRGroup)groupCache.get(groupID);

    if (reqGroup == null)
    {
      // We do not have this group in the cache. Hence we retrieve it from
      // the persistent datastore. The group object is put into the cache.

      reqGroup = mcrUserStore.retrieveGroup(groupID);
      if (reqGroup == null)
        throw new MCRException("Unknown group!");
      else
      {
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
   * @param userID          string representing the requested user object
   * @param bFromDataStore  user must be retrieved directly from the data store
   * @return MCRUser        user object (if available), otherwise null
   */
  public MCRUser retrieveUser(String userID) throws Exception
  { return this.retrieveUser(userID, false); }

  public synchronized MCRUser retrieveUser(String userID, boolean bFromDataStore) throws Exception
  {
    // In order to compare a modified user object with the persistent one we must
    // be able to force this method to get the user from the store
    MCRUser reqUser;
    if (bFromDataStore)
      reqUser = null;
    else
      reqUser = (MCRUser)userCache.get(userID);

    if (reqUser == null)
    {
      // We do not have this user in the cache. Hence we retrieve him or her
      // from the persistent datastore. The user object is put into the cache.

      reqUser = mcrUserStore.retrieveUser(userID);
      if (reqUser == null)
        return null; // no such user available
        //throw new MCRException("Unknown user!");
      else
      {
        userCache.put(userID, reqUser);
        return reqUser;
      }
    }
    else
      return reqUser;
  }

  /**
   * This method saves all groups of the system to an XML file. This is usefull for
   * exporting the groups to another system, e.g. by upgrading the DL.
   *
   * @param fileName  Name of the file the groups will be saved in
   */
  public final void saveAllGroupsToXMLFile(String fileName) throws IOException, Exception
  {
    MCRGroup currentGroup;
    Document groupDoc;
    FileWriter outFile = new FileWriter(new File(fileName));
    outFile.write("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n");
    outFile.write("<mycore_user_and_group_info type=\"group\">");

    Vector allGroupIDs = mcrUserStore.getAllGroupIDs();
    for (int i=0; i<allGroupIDs.size(); i++)
    {
      currentGroup = mcrUserStore.retrieveGroup((String)allGroupIDs.elementAt(i));
      groupDoc = currentGroup.toDOM();
      NodeList domGroupList = groupDoc.getElementsByTagName("group");
      StringBuffer sb = new StringBuffer();
      MCRXMLHelper.getNodeAsString(domGroupList.item(0), "  ", sb);
      outFile.write(sb.toString());
    }

    outFile.write("\n</mycore_user_and_group_info>");
    outFile.flush();
    outFile.close();
  }

  /**
   * This method saves all users of the system to an XML file. This is usefull for
   * exporting the users to another system, e.g. by upgrading the DL.
   *
   * @param fileName  Name of the file the users will be saved in
   */
  public final void saveAllUsersToXMLFile(String fileName) throws IOException, Exception
  {
    MCRUser currentUser;
    Document userDoc;
    FileWriter outFile = new FileWriter(new File(fileName));
    outFile.write("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n");
    outFile.write("<mycore_user_and_group_info type=\"user\">");

    Vector allUserIDs = mcrUserStore.getAllUserIDs();
    for (int i=0; i<allUserIDs.size(); i++)
    {
      currentUser = mcrUserStore.retrieveUser((String)allUserIDs.elementAt(i));
      userDoc = currentUser.toDOM();
      NodeList domUserList = userDoc.getElementsByTagName("user");
      StringBuffer sb = new StringBuffer();
      MCRXMLHelper.getNodeAsString(domUserList.item(0), "  ", sb);
      outFile.write(sb.toString());
    }

    outFile.write("\n</mycore_user_and_group_info>");
    outFile.flush();
    outFile.close();
  }

  /**
   * updates a group in the datastore (and the cache as well)
   * @param group    The group object which will be updated
   */
  public final synchronized void updateGroup(MCRGroup group) throws MCRException, Exception
  {
    if (mcrUserStore.existsGroup(group.getID())) {
      mcrUserStore.updateGroup(group.getID(), group);
      groupCache.remove(group.getID());
      groupCache.put(group.getID(), group);
    }
    else
      throw new MCRException("Tried to update group "+group.getID()+
                             " which is not yet stored in the database.");
  }

  /**
   * updates a user in the datastore (and the cache as well)
   * @param user     The user object which will be updated
   */
  public final synchronized void updateUser(MCRUser user) throws MCRException, Exception
  {
    if (mcrUserStore.existsUser(user.getID()))
    {
      // We have to check whether the membership to some of the groups of this user changed.
      // For example, the user might be removed from one of the groups he or she was
      // a member of. This group must be notified! To get information about which groups
      // have been added or removed, we compare the current (updated) user object with
      // the one from the datastore before the update process takes place.

      String userID = user.getID();
      MCRUser oldUser = retrieveUser(userID, true); // get the user directly from the datastore
      for (int i=0; i<user.getGroups().size(); i++)
      {
        MCRGroup updGroup = retrieveGroup((String)user.getGroups().elementAt(i));
        if (!oldUser.getGroups().contains(updGroup.getID())) {
          // The user is a new member of this group
          updGroup.addUser(userID);
        }
      }
      for (int i=0; i<oldUser.getGroups().size(); i++)
      {
        MCRGroup updGroup = retrieveGroup((String)oldUser.getGroups().elementAt(i));
        if (!user.getGroups().contains(updGroup.getID())) {
          // The user is no longer member of this group
          updGroup.removeUser(userID);
        }
      }

      // Now we really update the current user
      mcrUserStore.updateUser(user.getID(), user);
      userCache.remove(user.getID());
      userCache.put(user.getID(), user);
    }
    else
      throw new MCRException("Tried to update user "+user.getID()+
                             " which is not yet stored in the database.");
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
    mcrDocument = MCRXMLHelper.parseURI(filename);

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

      // Create all users found in the XML file. In their constructors the objects will
      // call MCRUserMgr.createUser() where they will be created in the datastore and
      // put into the cache.

      for (int i=0; i<iNumUsers; i++) {
        newUser = new MCRUser((Element)domUserList.item(i));
      }
    }
    else if (infoType.equals("group"))
    {
      MCRGroup newGroup;
      NodeList domGroupList = mcrDocument.getElementsByTagName("group");
      int iNumGroups = domGroupList.getLength();
      System.out.println("Number of groups to load: " + iNumGroups);

      // Create all groups found in the XML file. In their constructors the objects will
      // call MCRUserMgr.createGroup() where they will be created in the datastore and
      // put into the cache.

      for (int i=0; i<iNumGroups; i++) {
        newGroup = new MCRGroup((Element)domGroupList.item(i));
      }
    }
    else if (infoType.equals("privilege"))
    {
      NodeList domPrivList = mcrDocument.getElementsByTagName("privilege");
      int iNumPrivs = domPrivList.getLength();
      System.out.println("Number of privileges to load: " + iNumPrivs);
      MCRPrivilegeSet.instance().loadPrivileges(domPrivList, true);
    }
    else
      throw new MCRException("MCRUserMgr.loadUsersOrGroupsFromXMLFile : The type attribute"+
                             "of <mycore_user_and_group_info> is incorrect!");
  }
}
