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
import org.jdom.Document;
import org.jdom.Element;
import mycore.common.*;
import mycore.db2.MCRDB2UserStore;

/**
 * This class is the user (and group) manager of the MyCoRe system. It is implemented
 * using the singleton design pattern in order to ensure that there is only one instance
 * of this class, i.e. one user manager, running. The user manager has several
 * responsibilities. First it serves as a facade for client classes such as MyCoRe-
 * Servlets to retrieve objects from the persistent datastore. Then the manager is
 * used by the user and group objects themselves to manage their existence in the
 * underlying datastore.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserMgr
{
  /** file separator, read from system properties */
  private String SLASH = "";

  /** flag that determines whether write access to the data is denied (true) or allowed */
  private boolean locked = false;

  /** the user cache */
  private MCRCache userCache;

  /** the group cache */
  private MCRCache groupCache;

  /** the class responsible for persistent datastore (configurable ) */
  private MCRUserStore mcrUserStore;

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

    userCache  = new MCRCache(20);  // resonable values? This might also be
    groupCache = new MCRCache(10);  // read from mycore.properties....
  }

  /**
   * This method is the only way to get an instance of this class. It calls the
   * private constructor to create the singleton.
   *
   * @return   returns the one and only instance of <CODE>MCRUserMgr</CODE>
   */
  public final static synchronized MCRUserMgr instance() throws Exception
   {
     if (theInstance == null)
       theInstance = new MCRUserMgr();
     return theInstance;
   }

  /**
   * This method checks the consistency of the user and group data. It should be executed
   * after importing data from xml files, e.g.
   */
  public final void checkConsistency() throws MCRException, Exception
  {
    locked = true; // we now run in the read only mode

    // For all users in the system get their groups and check if the groups really exist at
    // all. We do not need to check if the user is a member of the groups listed in his or
    // her groups vector since the user object is constructed from the data - so he or she
    // *must* be a member by definition.

    Vector allUserIDs = mcrUserStore.getAllUserIDs();
    for (int i=0; i<allUserIDs.size(); i++) {
      MCRUser currentUser = retrieveUser((String)allUserIDs.elementAt(i), true);
      Vector currentGroupIDs = currentUser.getGroupIDs();
      for (int j=0; j<currentGroupIDs.size(); j++) {
        if (!mcrUserStore.existsGroup((String)currentGroupIDs.elementAt(j))) {
          System.out.println("user : '"+currentUser.getID()+"' error: unknown group '"
                             +(String)currentGroupIDs.elementAt(j)+"'!");
        }
      }
    }

    // For all groups get the admins and members (user and group lists, respectively) and
    // check if they have unknown users as admins or members, unknown groups as admins or
    // members etc.

    Vector allGroupIDs = mcrUserStore.getAllGroupIDs();
    for (int i=0; i<allGroupIDs.size(); i++) {
      MCRGroup currentGroup = retrieveGroup((String)allGroupIDs.elementAt(i), true);

      // check the admin users
      Vector admUserIDs = currentGroup.getAdminUserIDs();
      for (int j=0; j<admUserIDs.size(); j++) {
        if (!mcrUserStore.existsUser((String)admUserIDs.elementAt(j))) {
          System.out.println("group: '"+currentGroup.getID()+"' error: unknown admin user '"
                             +(String)admUserIDs.elementAt(j)+"'!");
        }
      }

      // check the admin groups
      Vector admGroupIDs = currentGroup.getAdminGroupIDs();
      for (int j=0; j<admGroupIDs.size(); j++) {
        if (!mcrUserStore.existsGroup((String)admGroupIDs.elementAt(j))) {
          System.out.println("group: '"+currentGroup.getID()+"' error: unknown admin group '"
                             +(String)admGroupIDs.elementAt(j)+"'!");
        }
      }

      // check the users (members)
      Vector mbrUserIDs = currentGroup.getMemberUserIDs();
      for (int j=0; j<mbrUserIDs.size(); j++) {
        if (!mcrUserStore.existsUser((String)mbrUserIDs.elementAt(j))) {
          System.out.println("group: '"+currentGroup.getID()+"' error: unknown user '"
                             +(String)mbrUserIDs.elementAt(j)+"'!");
        }
      }

      // check the groups (members)
      Vector mbrGroupIDs = currentGroup.getMemberGroupIDs();
      for (int j=0; j<mbrGroupIDs.size(); j++) {
        if (!mcrUserStore.existsGroup((String)mbrGroupIDs.elementAt(j))) {
          System.out.println("group: '"+currentGroup.getID()+"' error: unknown member group '"
                             +(String)mbrGroupIDs.elementAt(j)+"'!");
        }
        else if (currentGroup.getID().equals((String)mbrGroupIDs.elementAt(j))) {
          System.out.println("group: '"+currentGroup.getID()+"' error: the group "
                             +"must not contain itself as a member group!");
        }
      }

      // check the existence of the groups where the current group is a member of
      Vector groupIDs = currentGroup.getGroupIDs();
      for (int j=0; j<groupIDs.size(); j++) {
        if (!mcrUserStore.existsGroup((String)groupIDs.elementAt(j))) {
          System.out.println("group: '"+currentGroup.getID()+"' error: unknown group '"
                             +(String)groupIDs.elementAt(j)+"'!");
        }
      }

      // check if the current group implicitly is a member of itself
      if (currentGroup.isMemberOf(currentGroup.getID())) {
        System.out.println("group: '"+currentGroup.getID()+"' error: the group "
                           +"implicitly is a member of itself. Check the affiliations!");
      }

      // check if all privileges set for the groups exist in the privilege set
      Vector privs = currentGroup.getPrivileges();
      if (privs != null) {
        for (int j=0; j<privs.size(); j++) {
        if (!mcrUserStore.existsPrivilege((String)privs.elementAt(j)))
          System.out.println("group: '"+currentGroup.getID()
                            +"' error: unknown privilege '"+(String)privs.elementAt(j)+"'");
        }
      }
    }

    System.out.println("done.");
    locked = false; // write access is allowed again
  }

  /**
   * This method creates a group in the datastore (and the group cache as well).
   *
   * @param group    The group object which should be created
   * @param create   Boolean value, if true: create a new group, if false: import from file
   */
  public final synchronized void createGroup(MCRGroup group, boolean create) throws Exception
  {
    if (locked)
      throw new MCRException("The user component is locked. At the moment write access is denied.");

    // Check if the group already exists. If so, throw an exception
    if (!mcrUserStore.existsGroup(group.getID()))
    {
      try
      {
        // At first create the group. The group must be created before updating the groups this
        // group is a member of because the existence of the group will be checked while updating
        // the groups.

        mcrUserStore.createGroup(group);

        if (create) // The following tasks need not to be done while importing groups from file
        {
          // We first check whether this group has admins (users or groups) and if so, whether
          // they exist at all.

          Vector admUserIDs = group.getAdminUserIDs();
          for (int j=0; j<admUserIDs.size(); j++) {
            if (!mcrUserStore.existsUser((String)admUserIDs.elementAt(j))) {
              throw new MCRException("MCRUserMgr.createGroup(): unknown admin userID: "
                                    +(String)admUserIDs.elementAt(j));
            }
          }

          Vector admGroupIDs = group.getAdminGroupIDs();
          for (int j=0; j<admGroupIDs.size(); j++) {
            if (!mcrUserStore.existsGroup((String)admGroupIDs.elementAt(j))) {
              throw new MCRException("MCRUserMgr.createGroup(): unknown admin groupID: "
                                    +(String)admGroupIDs.elementAt(j));
            }
          }

          // We now check whether this group already has members (users or groups) and if so,
          // remove them from the cache such that they will have to be retrieved from the
          // datastore again. In addition we test if the members exist at all...

          Vector mbrUserIDs = group.getMemberUserIDs();
          if (mbrUserIDs != null) { // members (users) are already defined
            for (int i=0; i<mbrUserIDs.size(); i++) {
              if (mcrUserStore.existsUser((String)mbrUserIDs.elementAt(i)))
                userCache.remove((String)mbrUserIDs.elementAt(i));
              else
                throw new MCRException("MCRUserMgr.createGroup(): unknown userID: "+(String)mbrUserIDs.elementAt(i));
            }
          }

          Vector mbrGroupIDs = group.getMemberGroupIDs();
          if (mbrGroupIDs != null) { // members (groups) are already defined
            for (int i=0; i<mbrGroupIDs.size(); i++) {
              if (group.getID().equals((String)mbrGroupIDs.elementAt(i)))
                throw new MCRException("The group '"+group.getID()+ "' cannot contain itself.");
              if (mcrUserStore.existsGroup((String)mbrGroupIDs.elementAt(i)))
                groupCache.remove((String)mbrGroupIDs.elementAt(i));
              else
                throw new MCRException("MCRUserMgr.createGroup(): unknown groupID: "+(String)mbrGroupIDs.elementAt(i));
            }
          }

          // We now check if the privileges set for the group really exist at all.
          checkPrivsForGroup(group);

          // We now check if the group implicitly would be a member of itself. Attention: it is important
          // to do this *before* the following update of the groups this group will be a member of!

          if (MCRGroup.isImplicitMemberOf(group, group.getID())) {
            throw new MCRException("Create failed: the group '"+group.getID()
                                  +"' implicitly is a member of itself. Check the affiliations!");
          }

          // We finally update the groups this group will be a member of
          Vector groupIDs = group.getGroupIDs();
          if (groupIDs != null) {
            for (int i=0; i<groupIDs.size(); i++) {
              MCRGroup currentGroup = this.retrieveGroup((String)groupIDs.elementAt(i), true);
              currentGroup.addMemberGroupID(group.getID());
            }
          }
        }
      }

      catch (Exception ex)
      {
        // Since something went wrong we delete the previously created group. We do this
        // using this.deleteGroup() in order to ensure that already updated groups will
        // be resetted to the original state as well.
        deleteGroup(group.getID());
        throw ex;
      }
    }
    else
      throw new MCRException("The group '"+group.getID()+"' already exists!");
  }

  /**
   * This method creates a user in the datastore (and the user cache as well).
   * @param user   The user object which will be created
   */
  public final synchronized void createUser(MCRUser user) throws Exception
  {
    if (locked)
      throw new MCRException("The user component is locked. At the moment write access is denied.");

    // Check if the user already exists. If so, throw an exception
    if (!mcrUserStore.existsUser(user.getNumID(), user.getID()))
    {
      try
      {
        // At first create the user. The user must be created before updating the groups
        // because the existence of the user will be checked while updating the groups.
        mcrUserStore.createUser(user);

        // now update the groups
        Vector groupIDs = user.getGroupIDs();

        if (groupIDs != null) { // well, actually this cannot be since there is always the primary group...
          for (int i=0; i<groupIDs.size(); i++) {
            MCRGroup currentGroup = this.retrieveGroup((String)groupIDs.elementAt(i), true);
            currentGroup.addMemberUserID(user.getID());
          }
        }

      }
      catch (MCRException ex)
      {
        // Since something went wrong we delete the previously created user. We do this
        // using this.deleteUser() in order to ensure that already updated groups will
        // be resetted to the original state as well.
        deleteUser(user.getID());
        throw ex;
      }
    }
    else
      throw new MCRException("The user '"+user.getID()+"' or numerical ID '" +user.getNumID()+ "' already exists!");
  }

  /**
   * This method deletes a group from the datastore (and the group cache as well).
   * @param groupID   The group ID which will be deleted
   */
  public final synchronized void deleteGroup(String groupID) throws Exception
  {
    if (locked)
      throw new MCRException("The user component is locked. At the moment write access is denied.");

    // check if the group exists at all
    if (mcrUserStore.existsGroup(groupID))
    {
      // First we check if there are users in the system which have this group as their primary
      // group. If so, this group cannot be deleted. First the users must be updated.

      Vector primUserIDs = mcrUserStore.getUserIDsWithPrimaryGroup(groupID);
      if (primUserIDs.size() > 0) {
        throw new MCRException("Group '"+groupID+"' cannot be deleted since there are users with '"
                               + groupID+"' as their primary group. First update the users!");
      }

      // It is sufficient to remove the members (users and groups, respectively) from  the caches.
      // The next time they will be used they will be rebuild from the datastore and hence no
      // longer have this group in their group lists.

      MCRGroup delGroup = retrieveGroup(groupID);

      for (int i=0; i<delGroup.getMemberGroupIDs().size(); i++)
        groupCache.remove((String)delGroup.getMemberGroupIDs().elementAt(i));
      for (int i=0; i<delGroup.getMemberUserIDs().size(); i++)
        userCache.remove((String)delGroup.getMemberUserIDs().elementAt(i));

      // We have to notify the groups where this group is an administrative group
      for (int i=0; i<delGroup.getAdminGroupIDs().size(); i++) {
        String gid = (String)delGroup.getAdminGroupIDs().elementAt(i);
        if (mcrUserStore.existsGroup(gid)) { // this test must be!
          MCRGroup currentGroup = retrieveGroup(gid);
          currentGroup.removeAdminGroupID(groupID);
        }
      }

      // We have to notify the groups this group is a member of
      for (int i=0; i<delGroup.getGroupIDs().size(); i++) {
        String gid = (String)delGroup.getGroupIDs().elementAt(i);
        if (mcrUserStore.existsGroup(gid)) { // this test must be!
          MCRGroup currentGroup = retrieveGroup(gid);
          currentGroup.removeMemberGroupID(groupID);
        }
      }
      mcrUserStore.deleteGroup(groupID);
      groupCache.remove(groupID);
    }
    else
      throw new MCRException("MCRUserMgr.deleteGroup(): Group '"+groupID+"' is unknown!");
  }

  /**
   * This method deletes a user from the datastore (and the user cache as well).
   * @param userID   The user ID which will be deleted
   */
  public final synchronized void deleteUser(String userID) throws Exception
  {
    if (locked)
      throw new MCRException("The user component is locked. At the moment write access is denied.");

    // check if the user exists at all
    if (mcrUserStore.existsUser(userID))
    {
      try
      {
        // We have to notify the groups where this user is an administrative user
        Vector adminGroups = mcrUserStore.getGroupIDsWithAdminUser(userID);
        for (int i=0; i<adminGroups.size(); i++) {
          MCRGroup adminGroup = retrieveGroup((String)adminGroups.elementAt(i));
          adminGroup.removeAdminUserID(userID);
        }

        // We have to notify the groups this user is a member of
        MCRUser user = retrieveUser(userID);
        for (int i=0; i<user.getGroupIDs().size(); i++) {
          MCRGroup currentGroup = retrieveGroup((String)user.getGroupIDs().elementAt(i));
          currentGroup.removeMemberUserID(userID); // while updating the group it will be removed from the group cache
        }
      }

      catch (Exception ex)
      { throw ex; }

      finally {
        mcrUserStore.deleteUser(userID);
        userCache.remove(userID);
      }
    }
    else
      throw new MCRException("MCRUserMgr.deleteUser(): User '"+userID+"' is unknown!");
  }

  /**
   * This method gets all group IDs from the persistent datastore and returns them
   * as a Vector of strings.
   *
   * @return   Vector of strings containing the group IDs of the system.
   */
  public final synchronized Vector getAllGroupIDs() throws Exception
  { return mcrUserStore.getAllGroupIDs(); }

  /**
   * This method returns a JDOM presentation of all groups of the system
   * @return   JDOM document presentation of all groups of the system
   */
  public final synchronized Document getAllGroups() throws Exception
  {
    MCRGroup currentGroup;
    Element root = new Element("mcr_userobject");
    root.setAttribute("type", "group");

    Vector allGroupIDs = mcrUserStore.getAllGroupIDs();
    for (int i=0; i<allGroupIDs.size(); i++) {
      currentGroup = mcrUserStore.retrieveGroup((String)allGroupIDs.elementAt(i));
      root.addContent(currentGroup.toJDOMElement());
    }

    Document jdomDoc = new Document(root);
    return jdomDoc;
  }

  /**
   * This method gets all user IDs from the persistent datastore and returns them
   * as a Vector of strings.
   *
   * @return   Vector of strings containing the user IDs of the system.
   */
  public final synchronized Vector getAllUserIDs() throws Exception
  { return mcrUserStore.getAllUserIDs(); }

  /**
   * This method returns a JDOM presentation of all users of the system
   * @return    JDOM document presentation of all users of the system
   */
  public final synchronized Document getAllUsers() throws Exception
  {
    MCRUser currentUser;
    Element root = new Element("mcr_userobject");
    root.setAttribute("type", "user");

    Vector allUserIDs = mcrUserStore.getAllUserIDs();
    for (int i=0; i<allUserIDs.size(); i++) {
      currentUser = mcrUserStore.retrieveUser((String)allUserIDs.elementAt(i));
      root.addContent(currentUser.toJDOMElement());
    }

    Document jdomDoc = new Document(root);
    return jdomDoc;
  }

  /**
   * Returns information about the group cache as a formatted string - ready for
   * printing it with System.out.println() or so.
   *
   * @return   returns information about the group cache as a formatted string
   */
  public final String getGroupCacheInfo()
  { return groupCache.toString(); }

  /**
   * Returns information about the user cache as a formatted string - ready for
   * printing it with System.out.println() or so.
   *
   * @return   returns information about the user cache as a formatted string
   */
  public final String getUserCacheInfo()
  { return userCache.toString(); }


  /**
   * login to the system. This method just checks the password for a given user.
   * For the moment we only support clear text passwords...
   *
   * @param userID   user ID for the login
   * @param passwd   password for the user
   * @return         true if the password matches the password stored, false otherwise
   */
  public synchronized boolean login(String userID, String passwd) throws Exception
  {
    MCRUser loginUser = retrieveUser(userID);
    if (loginUser.isEnabled())
      return (loginUser.getPassword().equals(passwd)) ? true : false;
    else throw new MCRException("Login denied. User is disabled.");
  }

  /**
   * This method first looks for a given groupID in the group cache and returns this
   * group object. In case that the group object is not in the cache, the group will
   * be retrieved from the database. Then the group object is put into the cache.
   *
   * @param groupID           string representing the requested group object
   * @param bFromDataStore    boolean value, if true the group must be retrieved directly
   *                          from the data store
   * @return                  MCRGroup group object (if available)
   * @exception MCRException  if group object is not known
   */

  public MCRGroup retrieveGroup(String groupID) throws Exception
  { return this.retrieveGroup(groupID, false); }

  public synchronized MCRGroup retrieveGroup (String groupID, boolean bFromDataStore) throws Exception
  {
    // In order to compare a modified group object with the persistent one we must
    // be able to force this method to get the group from the store

    MCRGroup reqGroup;
    reqGroup = (bFromDataStore) ? null : (MCRGroup)groupCache.get(groupID);

    if (reqGroup == null) { // We do not have this group in the cache.
      reqGroup = mcrUserStore.retrieveGroup(groupID);
      if (reqGroup == null)
        throw new MCRException("MCRUserMgr.retrieveGroup(): Unknown group '"+groupID+"'!");
      else {
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
   * @param userID            string representing the requested user object
   * @param bFromDataStore    boolean value, if true the user must be retrieved directly
   *                          from the data store
   * @return MCRUser          user object (if available), otherwise null
   */

  public MCRUser retrieveUser(String userID) throws Exception
  { return this.retrieveUser(userID, false); }

  public synchronized MCRUser retrieveUser(String userID, boolean bFromDataStore) throws Exception
  {
    // In order to compare a modified user object with the persistent one we must
    // be able to force this method to get the user from the store

    MCRUser reqUser;
    reqUser = (bFromDataStore) ? null : (MCRUser)userCache.get(userID);

    if (reqUser == null) { // We do not have this user in the cache
      reqUser = mcrUserStore.retrieveUser(userID);
      if (reqUser == null)
        return null; // no such user available
      else {
        userCache.put(userID, reqUser);
        return reqUser;
      }
    }
    else
      return reqUser;
  }

  /**
   * This method sets the lock-status of the user manager.
   * @param locked   flag that determines whether write access to the data is denied (true) or allowed
   */
  public final synchronized void setLock(boolean locked)
  { this.locked = locked; }

  /**
   * return
   *   This method return true is if the user manager is in the locked state
   */
  public final boolean isLocked()
  { return locked; }

  /**
   * This method updates a group in the datastore (and the cache as well).
   * @param group   The group object which will be updated
   */
  public final synchronized void updateGroup(MCRGroup group) throws MCRException, Exception
  {
    if (locked)
      throw new MCRException("The user component is locked. At the moment write access is denied.");

    String groupID = group.getID();
    if (mcrUserStore.existsGroup(groupID))
    {
      // We have to check whether the list of users (members) of this group changed. If so,
      // we do not have to notify those users, since the users get their membership infor-
      // mation when they are constructed from the datastore. However, we have to remove
      // them from the user cache so that they will be retrieved from the datastore. In
      // addition we better check whether the newly added users really exist at all.

      MCRGroup oldGroup = retrieveGroup(groupID, true); // get the group directly from the datastore

      // We look for newly added admin users in this group and check if they exist at all
      for (int i=0; i<group.getAdminUserIDs().size(); i++) {
        String userID = (String)group.getAdminUserIDs().elementAt(i);
        if (!mcrUserStore.existsUser(userID))
          throw new MCRException("You tried to add the unknown admin user '"
                                +userID+"' to the group '"+groupID+ "'.");
      }

      // We look for newly added admin groups in this group and check if they exist at all
      for (int i=0; i<group.getAdminGroupIDs().size(); i++) {
        String gid = (String)group.getAdminGroupIDs().elementAt(i);
        if (!mcrUserStore.existsGroup(gid))
          throw new MCRException("You tried to add the unknown admin group '"
                                +gid+"' to the group '"+groupID+ "'.");
      }

      // We look for newly added users (members) in this group
      for (int i=0; i<group.getMemberUserIDs().size(); i++) {
        String userID = (String)group.getMemberUserIDs().elementAt(i);
        if (!oldGroup.getMemberUserIDs().contains(userID)) {
          if (mcrUserStore.existsUser(userID))
            userCache.remove(userID);
          else
            throw new MCRException("You tried to add the unknown user '"+userID+"' to the group '"+groupID+ "'.");
        }
      }

      // We look for recently deleted users (members)
      for (int i=0; i<oldGroup.getMemberUserIDs().size(); i++) {
        if (!group.getMemberUserIDs().contains(oldGroup.getMemberUserIDs().elementAt(i))) {
          userCache.remove((String)oldGroup.getMemberUserIDs().elementAt(i));
        }
      }

      // We check whether newly added groups (members) really exist at all. If so, just like
      // with users above we do not have to notify them but have to remove them from the group
      // cache. In addition we better check whether the newly added groups really exist at all.

      for (int i=0; i<group.getMemberGroupIDs().size(); i++) {
        String gid = (String)group.getMemberGroupIDs().elementAt(i);
        if (gid.equals(groupID))
          throw new MCRException("The group '"+groupID+ "' cannot contain itself.");
        if (!oldGroup.getMemberGroupIDs().contains(gid)) {
          if (mcrUserStore.existsGroup(gid))
            groupCache.remove(gid);
          else
            throw new MCRException("You tried to add the unknown group '"+gid+"' to the group '"+groupID+ "'.");
        }
      }

      // We look for recently deleted groups (members)
      for (int i=0; i<oldGroup.getMemberGroupIDs().size(); i++) {
        if (!group.getMemberGroupIDs().contains(oldGroup.getMemberGroupIDs().elementAt(i))) {
          groupCache.remove((String)oldGroup.getMemberGroupIDs().elementAt(i));
        }
      }

      // We now check if the privileges set for the group really exist at all.
      checkPrivsForGroup(group);

      // Now check if the group implicitly would be a member of itself
      if (MCRGroup.isImplicitMemberOf(group, groupID)) {
        throw new MCRException("Update failed: the group '"+groupID
                              +"' implicitly is a member of itself. Check the affiliations!");
      }

      // Now check and update changes in the membership to other groups
      update(group, oldGroup);

      // Now we really update the group object in the datastore
      mcrUserStore.updateGroup(group);

      groupCache.remove(groupID);
      groupCache.put(groupID, group);
    }
    else
      throw new MCRException("You tried to update the unknown group '"+groupID+"'.");
  }

  /**
   * This method updates a user in the datastore (and the cache as well).
   * @param user   The user object which will be updated
   */
  public final synchronized void updateUser(MCRUser updUser) throws MCRException, Exception
  {
    if (locked)
      throw new MCRException("The user component is locked. At the moment write access is denied.");

    String userID = updUser.getID();
    if (mcrUserStore.existsUser(userID))
    {
      // We have to check whether the membership to some of the groups of this user changed.
      // For example, the user might be removed from one of the groups he or she was
      // a member of. This group must be notified! To get information about which groups
      // have been added or removed, we compare the current (updated) user object with
      // the one from the datastore before the update process takes place.

      MCRUser oldUser = retrieveUser(userID, true); // get the user directly from the datastore
      update(updUser, oldUser); // Now check and update changes in the membership to groups

      // Now we really update the current user
      mcrUserStore.updateUser(updUser);
      userCache.remove(userID);
      userCache.put(userID, updUser);
    }
    else
      throw new MCRException("You tried to update the unknown user '"+userID+"'.");
  }

  /**
   * This private helper method checks if the privileges defined for a given group
   * really exist in the privilege set. It is used by createGroup() and updateGroup().
   * If a privilege does not exist an MCRException is thrown.
   */
  private final void checkPrivsForGroup(MCRGroup group) throws MCRException, Exception
  {
    Vector privs = group.getPrivileges();
    if (privs != null) {
      for (int i=0; i<privs.size(); i++) {
        String privName = (String)privs.elementAt(i);
        if (!mcrUserStore.existsPrivilege(privName))
          throw new MCRException("Create/update of group '"+group.getID()
                                +"' failed: unknown privilege: "+privName);
      }
    }
  }

  /**
   * This method updates groups where a given user or group object is a new member of or
   * is no longer a member of, respectively. The groups are determined by comparing the
   * user or group to be updated with the version out of the datastore, i.e. before the
   * update request.
   *
   * @param updObject   The user object which is to be updated
   * @param oldObject   The same user object as it was before the update request.
   */
  private final void update(MCRUserObject updObject, MCRUserObject oldObject)
                     throws MCRException, Exception
  {
    // It is important to first check whether *all* groups where the current object is a new member of
    // exist at all. If something goes wrong here an exception will be thrown and we do not have to
    // care about a rollback of data already modified. Hence do not combine the following two loops.

    String ID = updObject.getID();
    for (int i=0; i<updObject.getGroupIDs().size(); i++) {
      String gid = (String)updObject.getGroupIDs().elementAt(i);
      if (!mcrUserStore.existsGroup(gid)) {
        if (updObject instanceof MCRUser)
          throw new MCRException("You tried to update user '"+ID+"' with the unknown group '"+gid+"'.");
        else // it's a MCRGroup object
          throw new MCRException("You tried to update group '"+ID+"' with the unknown group '"+gid+"'.");
      }
    }

    // update groups where this object is a new member of
    for (int i=0; i<updObject.getGroupIDs().size(); i++) {
      MCRGroup updGroup = retrieveGroup((String)updObject.getGroupIDs().elementAt(i));
      if (!oldObject.getGroupIDs().contains(updGroup.getID())) {
        if (updObject instanceof MCRUser)
          updGroup.addMemberUserID(ID);
        else // it's a MCRGroup object
          updGroup.addMemberGroupID(ID);
      }
    }

    // update groups where this object is no longer a member of
    for (int i=0; i<oldObject.getGroupIDs().size(); i++) {
      MCRGroup updGroup = retrieveGroup((String)oldObject.getGroupIDs().elementAt(i));
      if (!updObject.getGroupIDs().contains(updGroup.getID())) { // The object is no longer member of this group
        if (updObject instanceof MCRUser)
          updGroup.removeMemberUserID(ID);
        else // it's a MCRGroup object
          updGroup.removeMemberGroupID(ID);
      }
    }
  }
}
