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

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.user.MCRCrypt;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRPrivilege;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

/**
 * This class provides a set of commands for the org.mycore.user management which
 * can be used by the command line interface.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUserCommands implements MCRExternalCommandInterface
{
  /** the file separator */
  private static String SLASH = System.getProperty( "file.separator" );

  /** The logger and the CONFIGuration */
  private static Logger LOGGER = Logger.getLogger(MCRUserCommands.class.getName());
  private static MCRConfiguration CONFIG = null;

  /** The array holding all known commands */
  private static ArrayList command  = null;

  /**
   * The empty constructor.
   */
  public MCRUserCommands()
  {
    command  = new ArrayList();

    // init the user system
    command.add("init superuser");
    command.add("org.mycore.frontend.cli.MCRUserCommands.initSuperuser MCRSession");
    // check the consistence of the user system
    command.add("check user data consistency");
    command.add("org.mycore.frontend.cli.MCRUserCommands.checkConsistency MCRSession");
    // encrypt a the passwords of data from the first UserSystem version
    command.add("encrypt passwords in user xml file {0} to file {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.encryptPasswordsInXMLFile MCRSession String String");
    // set a new password for the user
    command.add("set password for user {0} to {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.setPassword MCRSession String String");
    // set user management to read only mode
    command.add("set user management to ro mode");
    command.add("org.mycore.frontend.cli.MCRUserCommands.setLock MCRSession");
    // set user management to read/write mode
    command.add("set user management to rw mode");
    command.add("org.mycore.frontend.cli.MCRUserCommands.unLock MCRSession");
    // enable a user
    command.add("enable user {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.enableUser MCRSession String");
    // disable a user
    command.add("disable user {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.disableUser MCRSession String");

    //update privileges for a file
    command.add("update privileges data from file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.updatePrivilegesFromFile MCRSession String");
    // create new group from data of a file
    command.add("create group data from file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.createGroupFromFile MCRSession String");
    // import new group from data of a file (repair process)
    command.add("import group data from file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.importGroupFromFile MCRSession String");
    // update group data from a file
    command.add("update group data from file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.updateGroupFromFile MCRSession String");
    // delete a group
    command.add("delete group {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.deleteGroup MCRSession String");
    // add a group as a member of an other group
    command.add("add group {0} as member to group {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.addMemberGroupToGroup MCRSession String String");
    // remove a group as member of an other group
    command.add("remove group {0} as member from group {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.removeMemberGroupFromGroup MCRSession String String");
    // add a new privilege to this group
    command.add("add to group {0} the privilege {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.addPrivilegeToGroup MCRSession String String");
    // remove a  privilege from this group
    command.add("remove from group {0} the privilege {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.removePrivilegeFromGroup MCRSession String String");
    // create new user from data of a file
    command.add("create user data from file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.createUserFromFile MCRSession String");
    // import new user from data of a file (repair process)
    command.add("import user data from file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.importUserFromFile MCRSession String");
    // update user data from a file
    command.add("update user data from file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.updateUserFromFile MCRSession String");
    // delete a user
    command.add("delete user {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.deleteUser MCRSession String");
    // add user as a member of a group
    command.add("add user {0} as member to group {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.addMemberUserToGroup MCRSession String String");
    // remove the user as a member of a group
    command.add("remove user {0} as member from group {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.removeMemberUserFromGroup MCRSession String String");

    // list all privileges
    command.add("list all privileges");
    command.add("org.mycore.frontend.cli.MCRUserCommands.listAllPrivileges MCRSession");
    // list all groups with their privileges and subgroups
    command.add("list all groups");
    command.add("org.mycore.frontend.cli.MCRUserCommands.listAllGroups MCRSession");
    // list data of one group
    command.add("list group {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.listGroup MCRSession String");
    // list all users with their groups
    command.add("list all users");
    command.add("org.mycore.frontend.cli.MCRUserCommands.listAllUsers MCRSession");
    // list data of one user
    command.add("list user {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.listUser MCRSession String");

    // save all privilege data to a file
    command.add("save all privileges to file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.saveAllPrivilegesToFile MCRSession String");
    // save all group data to a file
    command.add("save all groups to file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.saveAllGroupsToFile MCRSession String");
    // save one group to a file
    command.add("save group {0} to file {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.saveGroupToFile MCRSession String String");
    // save all user data to a file
    command.add("save all users to file {0}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.saveAllUsersToFile MCRSession String");
    // save one user to a file
    command.add("save user {0} to file {1}");
    command.add("org.mycore.frontend.cli.MCRUserCommands.saveGroupToFile MCRSession String String");
  }

  /**
   * The method return the list of possible commands of this class.
   * Each command has TWO Strings, a String of the user command syntax and
   * a String of the called method.
   * @return a command pair RArrayList
   **/
  public final ArrayList getPossibleCommands()
    { return command; }

  /**
   * Initialize common data.
   */
  private static void init() {
    CONFIG = MCRConfiguration.instance();
  }

  /**
   * This method initializes the user and group system an creates a
   * superuser with values set in mycore.properties.private As 'super'
   * default, if no properties were set, mcradmin with password mycore
   * will be used.
   *
   * @param session the MCRSession object
   **/
  public static void initSuperuser(MCRSession session) throws MCRException
  {
    init();
    String suser = CONFIG.getString("MCR.users_superuser_username", "mcradmin");
    String spasswd = CONFIG.getString("MCR.users_superuser_userpasswd", "mycore");
    String sgroup = CONFIG.getString("MCR.users_superuser_groupname", "mcrgroup");
    String guser = CONFIG.getString("MCR.users_guestuser_username", "gast");
    String gpasswd = CONFIG.getString("MCR.users_guestuser_userpasswd", "gast");
    String ggroup = CONFIG.getString("MCR.users_guestuser_groupname", "mcrgast");

    // If CONFIGuration parameter defines that we use password encryption: encrypt!
    String useCrypt = CONFIG.getString("MCR.users_use_password_encryption", "false");
    boolean useEncryption = (useCrypt.trim().equals("true")) ? true : false;
    if (useEncryption) {
      String cryptPwd = MCRCrypt.crypt(spasswd);
      spasswd = cryptPwd;
      cryptPwd = MCRCrypt.crypt(gpasswd);
      gpasswd = cryptPwd;
    }

    session.setCurrentUserID(suser);
    try {
      if (MCRUserMgr.instance().retrieveUser(suser) != null)
      if (MCRUserMgr.instance().retrieveGroup(sgroup) != null)
	LOGGER.info("The superuser already exists!");
      return;
    } catch (Exception e) {}

    try {
	ArrayList privList = new ArrayList();
	privList.add(
		new MCRPrivilege(
			"create user",
			"Users with this privilege may create new users."));
	privList.add(
		new MCRPrivilege(
			"create group",
			"Users with this privilege may create new groups."));
	privList.add(
		new MCRPrivilege(
			"delete user",
			"Users with this privilege may delete other users."));
	privList.add(
		new MCRPrivilege(
			"delete group",
			"Users with this privilege may delete groups."));
	privList.add(
		new MCRPrivilege(
			"modify user",
			"Users with this privilege may modify data of other users."));
	privList.add(
		new MCRPrivilege(
			"modify group",
			"Users with this privilege may modify data of groups."));
	privList.add(
		new MCRPrivilege(
			"modify privileges",
			"Users with this privilege may modify data of privileges."));
	privList.add(
		new MCRPrivilege(
			"user administrator",
			"Users with this privilege have administrator rights in the system."));
	privList.add(
		new MCRPrivilege(
			"list all users",
			"Users with this privilege may list the users of the system."));
	privList.add(
		new MCRPrivilege(
			"list all privileges",
			"Users with this privilege may list the privileges of the system."));
	privList.add(
		new MCRPrivilege(
			"change owner of object",
			"Users with this privilege may change the owner of an object in the repository."));
	MCRUserMgr.instance().initializePrivileges(privList);
    } catch (Exception e) {
      throw new MCRException("Can't create the privileges.", e);
    }
    LOGGER.info("The privilege set is installed.");

    // the superuser group
    try {
      ArrayList admUserIDs = new ArrayList();
      admUserIDs.add(suser);
      ArrayList admGroupIDs = new ArrayList();
      ArrayList mbrUserIDs = new ArrayList();
      mbrUserIDs.add(suser);
      ArrayList mbrGroupIDs = new ArrayList();
      ArrayList groupIDs = new ArrayList();
      ArrayList privileges = new ArrayList();
      privileges.add("user administrator");
      privileges.add("change owner of object");

      MCRGroup g = new MCRGroup(sgroup, suser, null, null,
			"The superuser group", admUserIDs,
			admGroupIDs, mbrUserIDs, mbrGroupIDs,
			groupIDs, privileges);

      MCRUserMgr.instance().initializeGroup(g, suser);
    } catch (Exception e) {
      throw new MCRException("Can't create the superuser group.", e);
    }
    LOGGER.info("The group " + sgroup + " is installed.");

    // the guest group
    try {
      ArrayList admUserIDs = new ArrayList();
      admUserIDs.add(suser);
      ArrayList admGroupIDs = new ArrayList();
      admGroupIDs.add(sgroup);
      ArrayList mbrUserIDs = new ArrayList();
      mbrUserIDs.add(guser);
      mbrUserIDs.add(suser);
      ArrayList mbrGroupIDs = new ArrayList();
      mbrGroupIDs.add(sgroup);
      ArrayList groupIDs = new ArrayList();
      ArrayList privileges = new ArrayList();

      MCRGroup g = new MCRGroup(ggroup, suser, null, null,
			"The guest group", admUserIDs,
			admGroupIDs, mbrUserIDs, mbrGroupIDs,
			groupIDs, privileges);

      MCRUserMgr.instance().initializeGroup(g, suser);
    } catch (Exception e) {
      throw new MCRException("Can't create the guest group.", e);
    }
    LOGGER.info("The group " + ggroup + " is installed.");

    // the superuser
    try {
      ArrayList groupIDs = new ArrayList();
      groupIDs.add(sgroup);
      groupIDs.add(ggroup);

      MCRUser u = new MCRUser(1, suser, suser, null, null, true, true,
			"Superuser", spasswd, sgroup, groupIDs,
			null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null);

      MCRUserMgr.instance().initializeUser(u, suser);
    } catch (Exception e) {
      throw new MCRException("Can't create the superuser.", e);
    }
      LOGGER.info("The user "   + suser
				+ " with password "
				+ CONFIG.getString("MCR.users_superuser_userpasswd", "mycore")
				+ " is installed.");

    // the guest
    try {
      ArrayList groupIDs = new ArrayList();
      groupIDs.add(ggroup);

      MCRUser u = new MCRUser(2, guser, suser, null, null, true, true,
			"guest", gpasswd, ggroup, groupIDs,
			null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null);

      MCRUserMgr.instance().initializeUser(u, suser);
    } catch (Exception e) {
      throw new MCRException("Can't create the guest user.", e);
    }
      LOGGER.info("The user "	+ guser
				+ " with password "
				+ CONFIG.getString("MCR.users_guestuser_userpasswd", "gast")
				+ " is installed.");

      // check all
      session.setCurrentUserID(suser);
      MCRUserMgr.instance().checkConsistency();
      LOGGER.info("");
  }

  /**
   * This method checks the data consistency of the user management and should be
   * called after a system crash or after importing data from files, respectively.
   *
   * @param session the MCRSession object
   */
  public static void checkConsistency(MCRSession session) throws Exception {
    MCRUserMgr.instance().checkConsistency();
  }

  /**
   * This method invokes MCRUserMgr.deleteGroup() and permanently removes
   * a group from the system.
   *
   * @param session the MCRSession object
   * @param groupID the ID of the group which will be deleted
   **/
  public static void deleteGroup(MCRSession session, String groupID) throws Exception {
    init();
    MCRUserMgr.instance().deleteGroup(groupID);
    LOGGER.info("Group ID " + groupID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.deleteUser() and permanently removes
   * a user from the system.
   *
   * @param session the MCRSession object
   * @param userID the ID of the user which will be deleted
   **/
  public static void deleteUser(MCRSession session, String userID) throws Exception {
    init();
    MCRUserMgr.instance().deleteUser(userID);
    LOGGER.info("User ID " + userID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.enableUser() that enables a user
   *
   * @param session the MCRSession object
   * @param userID the ID of the user which will be enabled
   **/
  public static void enableUser(MCRSession session, String userID) throws Exception {
    init();
    MCRUserMgr.instance().enableUser(userID);
    LOGGER.info("User ID " + userID + " enabled!");
  }

  /**
   * A given XML file containing user data with cleartext passwords must be converted
   * prior to loading the user data into the system. This method reads all user objects
   * in the given XML file, encrypts the passwords and writes them back to a file
   * with name original-file-name_encrypted.xml.
   *
   * @param session the MCRSession object
   * @param oldFile the filename of the user data input
   * @param newFile the filename of the user data output (encrypted passwords)
   **/
  public static final void encryptPasswordsInXMLFile(MCRSession session, String oldFile, String newFile)
  throws MCRException
  {
    init();
    if (!checkFilename(oldFile)) return;
    LOGGER.info( "Reading file " + oldFile + " ..." );
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(oldFile, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoreuser")) {
        throw new MCRException("These data do not correspond to a user."); }
      List listelm = rootelm.getChildren();  // the <user> elements
      for (int i=0;i<listelm.size();i++)
      {
        // Get the passwords, encrypt and write it back into the document
        org.jdom.Element elm =(org.jdom.Element)listelm.get(i);
        String passwd = elm.getChildTextTrim("user.password");
        String encryptedPasswd = MCRCrypt.crypt(passwd);
        elm.getChild("user.password").setText(encryptedPasswd);
      }
      FileOutputStream outFile = new FileOutputStream(newFile);
      saveToXMLFile(doc, outFile);
    }
    catch (Exception e) {
      throw new MCRException("Error while encrypting cleartext passwords in user xml file.",e); }
  }

  /**
   * This method invokes MCRUserMgr.disableUser() that disables a user
   *
   * @param session the MCRSession object
   * @param userID the ID of the user which will be enabled
   **/
  public static void disableUser(MCRSession session, String userID) throws Exception {
    init();
    MCRUserMgr.instance().disableUser(userID);
    LOGGER.info("User ID " + userID + " disabled!");
  }

  /**
   * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList
   * of all users stored in the persistent datastore.
   *
   * @param session the MCRSession object
   **/
  public static void listAllUsers(MCRSession session) throws Exception {
    init();
    ArrayList users = MCRUserMgr.instance().getAllUserIDs();
    LOGGER.info("");
    for (int i = 0; i < users.size(); i++) {
      listUser(session,(String)users.get(i));
    }
  }

  /**
   * This method invokes MCRUserMgr.getAllGroupIDs() and retrieves a ArrayList
   * of all groups stored in the persistent datastore.
   *
   * @param session the MCRSession object
   **/
  public static void listAllGroups(MCRSession session) throws Exception {
    init();
    ArrayList groups = MCRUserMgr.instance().getAllGroupIDs();
    LOGGER.info("");
    for (int i = 0; i < groups.size(); i++) {
      listGroup(session,(String)groups.get(i));
    }
  }

  /**
   * This method invokes MCRUserMgr.getAllPrivileges() and retrieves a ArrayList
   * of all privileges stored in the persistent datastore.
   * @param session the MCRSession object
   **/
  public static void listAllPrivileges(MCRSession session) throws MCRException
  {
    try {
      init();
      ArrayList privs = MCRUserMgr.instance().getAllPrivileges();
      LOGGER.info("");
      for (int i = 0; i < privs.size(); i++) {
	MCRPrivilege currentPriv = (MCRPrivilege) privs.get(i);
	LOGGER.info(currentPriv.getName());
	LOGGER.info("    " + currentPriv.getDescription());
      }
    } catch (Exception e) {
      throw new MCRException("Error while command saveAllGroupsToFile()", e);
    }
  }

  /**
   * This command takes a file name as a parameter, retrieves all groups from
   * MCRUserMgr as JDOM document and saves this to the given file.
   *
   * @param session the MCRSession object
   * @param filename Name of the file the groups will be saved to
   */
  public static void saveAllGroupsToFile(MCRSession session, String filename)
  throws MCRException
  {
    try {
      org.jdom.Document jdomDoc = MCRUserMgr.instance().getAllGroups();
      FileOutputStream outFile = new FileOutputStream(filename);
      saveToXMLFile(jdomDoc, outFile);
    } catch (Exception e) {
      throw new MCRException("Error while command saveAllGroupsToFile()", e);
    }
  }

  /**
   * This command takes a file name as a parameter, retrieves all privileges
   * from MCRUserMgr as JDOM document and saves this to the given file.
   *
   * @param filename Name of the file the privileges will be saved to
   */
  public static void saveAllPrivilegesToFile(MCRSession session, String filename)
  throws MCRException
  {
    try {
      org.jdom.Document jdomDoc = MCRUserMgr.instance().getAllPrivilegesAsJDOMDocument();
      FileOutputStream outFile = new FileOutputStream(filename);
      saveToXMLFile(jdomDoc, outFile);
    } catch (Exception e) {
      throw new MCRException("Error while command saveAllPrivilegesToFile()", e);
    }
  }

  /**
   * This command takes a file name as a parameter, retrieves all users from
   * MCRUserMgr as JDOM document and saves this to the given file.
   *
   * @param session the MCRSession object
   * @param filename Name of the file the users will be saved to
   */
  public static void saveAllUsersToFile(MCRSession session, String filename)
  throws MCRException
  {
    try {
      org.jdom.Document jdomDoc = MCRUserMgr.instance().getAllUsers();
      FileOutputStream outFile = new FileOutputStream(filename);
      saveToXMLFile(jdomDoc, outFile);
    } catch (Exception e) {
      throw new MCRException("Error while command saveAllUsersToFile()", e);
    }
  }

  /**
   * This command takes a groupID and file name as a parameter, retrieves the
   * group from MCRUserMgr as JDOM document and saves this to the given file.
   *
   * @param session the MCRSession object
   * @param groupID  ID of the group to be saved
   * @param filename Name of the file the groups will be saved to
   */
  public static void saveGroupToFile(MCRSession session, String groupID, String filename)
  throws Exception
  {
    try {
      MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
      org.jdom.Document jdomDoc = group.toJDOMDocument();
      FileOutputStream outFile = new FileOutputStream(filename);
      saveToXMLFile(jdomDoc, outFile);
    } catch (Exception e) {
      throw new MCRException("Error while command saveGroupToFile()", e);
    }
  }

  /**
   * This command takes a userID and file name as a parameter, retrieves the
   * user from MCRUserMgr as JDOM document and saves this to the given file.
   *
   * @param session the MCRSession object
   * @param userID   ID of the user to be saved
   * @param filename Name of the file the user will be saved to
   */
  public static void saveUserToFile(MCRSession session, String userID, String filename)
  throws MCRException
  {
    try {
      MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
      org.jdom.Document jdomDoc = user.toJDOMDocument();
      FileOutputStream outFile = new FileOutputStream(filename);
      saveToXMLFile(jdomDoc, outFile);
    } catch (Exception e) {
      throw new MCRException("Error while command saveUserToFile()", e);
    }
  }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to change the password.
   *
   * @param session the MCRSession object
   * @param userID the ID of the user for which the password will be set
   */
  public static void setPassword(MCRSession session, String userID, String password)
  throws MCRException {
    if (password == null)
      return;
    init();
    MCRUserMgr.instance().setPassword(userID, password);
    LOGGER.info("The new password was set.");
  }

  /**
   *This method sets the user management component to read only mode
   *
   * @param session the MCRSession object
   */
  public static void setLock(MCRSession session) throws MCRException {
    init();
    MCRUserMgr.instance().setLock(true);
    LOGGER.info("Write access to the user component persistent database now is denied.");
  }

  /**
   * This method sets the user management component to read/write access mode
   *
   * @param session the MCRSession object
   */
  public static void unLock(MCRSession session) throws MCRException {
    init();
    MCRUserMgr.instance().setLock(false);
    LOGGER.info("Write access to the user component persistent database now is allowed.");
  }

  /**
   * This method invokes MCRUserMgr.retrieveGroup() and then works with the
   * retrieved group object to get an XML-Representation.
   *
   * @param session the MCRSession object
   * @param groupID the ID of the group for which the XML-representation is needed
   */
  public static final void listGroup(MCRSession session, String groupID)
  throws MCRException {
    MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
    StringBuffer sb = new StringBuffer();
    LOGGER.info(" ");
    sb.append("group=").append(group.getID());
    LOGGER.info(sb.toString());
    ArrayList ar = group.getMemberGroupIDs();
    for (int i=0;i<ar.size();i++) {
      sb = new StringBuffer();
      sb.append("   groups in this group=").append((String)ar.get(i));
      LOGGER.info(sb.toString());
      }
    ar = group.getMemberUserIDs();
    for (int i=0;i<ar.size();i++) {
      sb = new StringBuffer();
      sb.append("   user in this group=").append((String)ar.get(i));
      LOGGER.info(sb.toString());
      }
    ar = group.getAllPrivileges();
    for (int i=0;i<ar.size();i++) {
      sb = new StringBuffer();
      sb.append("   has privileg=").append((String)ar.get(i));
      LOGGER.info(sb.toString());
      }
    //org.jdom.Document jdomDoc = group.toJDOMDocument();
    //showAsXML(jdomDoc);
  }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to get an XML-Representation.
   *
   * @param session the MCRSession object
   * @param userID the ID of the user for which the XML-representation is needed
   */
  public static final void listUser(MCRSession session, String userID)
  throws MCRException {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
    StringBuffer sb = new StringBuffer();
    LOGGER.info(" ");
    sb.append("user=").append(user.getID()).append("   number=").append(user.getNumID()).append("   update=").append(user.isUpdateAllowed()).append("   enabled=").append(user.isEnabled());
    LOGGER.info(sb.toString());
    sb = new StringBuffer();
    sb.append("   primary group=").append(user.getPrimaryGroupID());
    LOGGER.info(sb.toString());
    ArrayList ar = user.getAllGroupIDs();
    for (int i=0;i<ar.size();i++) {
      sb = new StringBuffer();
      sb.append("   member in group=").append((String)ar.get(i));
      LOGGER.info(sb.toString());
      }
    ar = user.getPrivileges();
    for (int i=0;i<ar.size();i++) {
      sb = new StringBuffer();
      sb.append("   has privileg=").append((String)ar.get(i));
      LOGGER.info(sb.toString());
      }
    //org.jdom.Document jdomDoc = user.toJDOMDocument();
    //showAsXML(jdomDoc);
  }

  /**
   * Check the file name
   * @param filename the filename of the user data input
   * @return true if the file name is okay
   **/
  private static final boolean checkFilename(String filename)
  {
    init();
    if (!filename.endsWith(".xml")) {
      LOGGER.warn(filename + " ignored, does not end with *.xml");
      return false;
    }
    if (!new File(filename).isFile()) {
      LOGGER.warn(filename + " ignored, is not a file.");
      return false;
    }
      return true;
  }

  /**
   * This method invokes MCRUserMgr.createUser() with data from a file.
   * @param session the MCRSession object
   * @param filename the filename of the user data input
   **/
  public static final void createUserFromFile(MCRSession session, String filename) {
    init();
    String useCrypt = CONFIG.getString("MCR.users_use_password_encryption", "false");
    boolean useEncryption = (useCrypt.trim().equals("true")) ? true : false;
    createUserFromFile(session, filename, useEncryption);
  }

  /**
   * This method invokes MCRUserMgr.createUser() with data from a file.
   * @param session the MCRSession object
   * @param filename the filename of the user data input
   * @param useEncryption flag to determine whether we use password encryption or not
   **/
  private static final void createUserFromFile(MCRSession session, String filename, boolean useEncryption)
  throws MCRException
  {
    init();
    if (!checkFilename(filename))
      return;
    LOGGER.info("Reading file " + filename + " ...");
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoreuser")) {
	throw new MCRException("The data are not for user.");
      }
      List listelm = rootelm.getChildren();
      for (int i = 0; i < listelm.size(); i++) {
	MCRUser u = new MCRUser((org.jdom.Element) listelm.get(i), useEncryption);
	MCRUserMgr.instance().createUser(u);
      }
    } catch (Exception e) {
      throw new MCRException("Error while loading user data.", e);
    }
  }

  /**
   * This method imports user data from an xml file. It is assumed that the user data
   * previously have been exported from a running Mycore system. That is, if the running
   * system uses password encryption, the passwords in the xml file already are encrypted,
   * too, so that the must not be encrypted again.
   *
   * @param session the MCRSession object
   * @param filename the filename of the user data input
   **/
  public static final void importUserFromFile(MCRSession session, String filename)
  throws MCRException
  {
    init();
    if (!checkFilename(filename))
      return;
    LOGGER.info("Reading file " + filename + " ...");
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoreuser")) {
        throw new MCRException("The data are not for user.");
      }
      List listelm = rootelm.getChildren();
      for (int i = 0; i < listelm.size(); i++) {
        MCRUser u = new MCRUser((org.jdom.Element) listelm.get(i), false); // do not encrypt passwords
        MCRUserMgr.instance().importUserObject(u);
      }
    } catch (Exception e) {
      throw new MCRException("Error while loading user data.", e);
    }
  }

  /**
   * This method invokes MCRUserMgr.createGroup() with data from a file.
   * @param session the MCRSession object
   * @param filename the filename of the user data input
   **/
  public static final void createGroupFromFile(MCRSession session, String filename)
  throws MCRException
  {
    init();
    if (!checkFilename(filename))
      return;
    LOGGER.info("Reading file " + filename + " ...");
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoregroup")) {
	throw new MCRException("The data are not for group.");
      }
      List listelm = rootelm.getChildren();
      for (int i = 0; i < listelm.size(); i++) {
	MCRGroup g = new MCRGroup((org.jdom.Element) listelm.get(i));
	MCRUserMgr.instance().createGroup(g);
      }
    } catch (Exception e) {
      throw new MCRException("Error while loading group data.", e);
    }
  }

  /**
   * This method imports group data from an xml file. It simply calles createGroupFromFile().
   *
   * @param session the MCRSession object
   * @param filename the filename of the group data input
   **/
  public static final void importGroupFromFile(MCRSession session, String filename)
  throws MCRException
  {
    init();
    if (!checkFilename(filename))
      return;
    LOGGER.info("Reading file " + filename + " ...");
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoregroup")) {
        throw new MCRException("The data are not for group.");
      }
      List listelm = rootelm.getChildren();
      for (int i = 0; i < listelm.size(); i++) {
        MCRGroup g = new MCRGroup((org.jdom.Element) listelm.get(i));
        MCRUserMgr.instance().importUserObject(g);
      }
    } catch (Exception e) {
      throw new MCRException("Error while loading group data.", e);
    }
  }

  /**
    * This method invokes MCRUserMgr.updateUser() with data from a file.
    * @param session the MCRSession object
    * @param filename the filename of the user data input
    **/
   public static final void updateUserFromFile(MCRSession session, String filename) {
     init();
     String useCrypt = CONFIG.getString("MCR.users_use_password_encryption", "false");
     boolean useEncryption = (useCrypt.trim().equals("true")) ? true : false;
     updateUserFromFile(session, filename, useEncryption);
   }

  /**
   * This method invokes MCRUserMgr.updateUser() with data from a file.
   * @param session the MCRSession object
   * @param filename the filename of the user data input
   * @param useEncryption flag to determine whether we use password encryption or not
   **/
  private static final void updateUserFromFile(MCRSession session, String filename, boolean useEncryption)
  throws MCRException
  {
    init();
    if (!checkFilename(filename))
      return;
    LOGGER.info("Reading file " + filename + " ...");
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoreuser")) {
	throw new MCRException("These data are not defining a user.");
      }
      List listelm = rootelm.getChildren();
      for (int i = 0; i < listelm.size(); i++) {
	MCRUser u = new MCRUser((org.jdom.Element) listelm.get(i), useEncryption);
	MCRUserMgr.instance().updateUser(u);
      }
    } catch (Exception e) {
      throw new MCRException("Error while updating a user from file.", e);
    }
  }

  /**
   * This method invokes MCRUserMgr.updateGroup() with data from a file.
   * @param session the MCRSession object
   * @param filename the filename of the user data input
   **/
  public static final void updateGroupFromFile(MCRSession session, String filename)
  throws MCRException
  {
    init();
    if (!checkFilename(filename))
      return;
    LOGGER.info("Reading file " + filename + " ...");
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoregroup")) {
	throw new MCRException("The data are not for a group.");
      }
      List listelm = rootelm.getChildren();
      for (int i = 0; i < listelm.size(); i++) {
	MCRGroup g = new MCRGroup((org.jdom.Element) listelm.get(i));
	MCRUserMgr.instance().updateGroup(g);
      }
    } catch (Exception e) {
      throw new MCRException("Error while update group from file.", e);
    }
  }

  /**
   * This method adds a group as a member to another group
   *
   * @param session the MCRSession object
   * @param mbrGroupID the ID of the group which will be a member of the group
   *   represented by groupID
   * @param groupID the ID of the group to which the group with ID mbrGroupID
   *   will be added
   * @throws MCRException
   */
  public static final void addMemberGroupToGroup(MCRSession session,
    String mbrGroupID, String groupID) throws MCRException
  {
    init();
    try {
     MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
     group.addMemberGroupID(mbrGroupID);
     MCRUserMgr.instance().updateGroup(group);
    } catch (Exception e) {
      throw new MCRException("Error while adding group "+mbrGroupID+
                             " to group "+groupID+".", e);
    }
  }

  /**
   * This method removes a member group from another group
   *
   * @param session the MCRSession object
   * @param mbrGroupID the ID of the group which will be removed from the group
   *   represented by groupID
   * @param groupID the ID of the group from which the group with ID mbrGroupID
   *   will be removed
   * @throws MCRException
   */
  public static final void removeMemberGroupFromGroup(MCRSession session,
    String mbrGroupID, String groupID) throws MCRException
  {
    init();
    try {
     MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
     group.removeMemberGroupID(mbrGroupID);
     MCRUserMgr.instance().updateGroup(group);
    } catch (Exception e) {
      throw new MCRException("Error while removing group "+mbrGroupID+
                             " from group "+groupID+".", e);
    }
  }

  /**
   * The method add a new privilege to a group.
   *
   * @param groupID the group name
   * @param priv the privilege String
   **/
  public static final void addPrivilegeToGroup(MCRSession session, 
    String groupID, String priv)
    {
    init();
    try {
      MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
      ArrayList ar = MCRUserMgr.instance().getAllPrivileges();
      boolean test = false;
      for (int i=0;i<ar.size();i++) {
        if(((MCRPrivilege)ar.get(i)).getName().equals(priv)) { test=true; break; } }
      if (!test) {
        throw new MCRException("The privilege "+priv+" to add does not exist!"); }
      group.addPrivilege(priv);
      MCRUserMgr.instance().updateGroup(group);
      } 
    catch (Exception e) {
      throw new MCRException("Error while adding privilege "+priv+
        " to group "+groupID+".", e);
      }
    }
 
  /**
   * The method remove a  privilege from a group.
   *
   * @param group the group name
   * @param priv the privilege String
   **/
  public static final void removePrivilegeFromGroup(MCRSession session,
    String groupID, String priv)
    {
    init();
    try {
      MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
      if (!group.hasPrivilege(priv)) {
        throw new MCRException("The group "+groupID+" has not the privilege "+priv+"!"); }
      group.removePrivilege(priv);
      MCRUserMgr.instance().updateGroup(group);
      } 
    catch (Exception e) {
      throw new MCRException("Error while adding privilege "+priv+
        " to group "+groupID+".", e);
      }
    }
 
  /**
   * This method adds a user as a member to a group
   *
   * @param session the MCRSession object
   * @param mbrUserID the ID of the user which will be a member of the group
   *   represented by groupID
   * @param groupID the ID of the group to which the user with ID mbrUserID
   *   will be added
   * @throws MCRException
   */
  public static final void addMemberUserToGroup(MCRSession session,
    String mbrUserID, String groupID) throws MCRException
  {
    init();
    try {
     MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
     group.addMemberUserID(mbrUserID);
     MCRUserMgr.instance().updateGroup(group);
    } catch (Exception e) {
      throw new MCRException("Error while adding group "+mbrUserID+
                             " to group "+groupID+".", e);
    }
  }

  /**
   * This method removes a member user from a group
   *
   * @param session the MCRSession object
   * @param mbrUserID the ID of the user which will be removed from the group
   *   represented by groupID
   * @param groupID the ID of the group from which the user with ID mbrUserID
   *   will be removed
   * @throws MCRException
   */
  public static final void removeMemberUserFromGroup(MCRSession session,
    String mbrUserID, String groupID) throws MCRException
  {
    init();
    try {
     MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
     group.removeMemberUserID(mbrUserID);
     MCRUserMgr.instance().updateGroup(group);
    } catch (Exception e) {
      throw new MCRException("Error while removing group "+mbrUserID+
                             " from group "+groupID+".", e);
    }
  }

  /**
   * This method invokes MCRUserMgr.updatePrivileges() with data from a file.
   * @param session the MCRSession object
   * @param filename the filename of the privileg data input
   **/
  public static final void updatePrivilegesFromFile(MCRSession session, String filename)
  throws MCRException
  {
    init();
    if (!checkFilename(filename))
      return;
    LOGGER.info("Reading file " + filename + " ...");
    try {
      org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
      org.jdom.Element rootelm = doc.getRootElement();
      if (!rootelm.getName().equals("mycoreprivilege")) {
	throw new MCRException("The data are not for privileges.");
      }
      List listelm = rootelm.getChildren();
      ArrayList list = new ArrayList();
      for (int i = 0; i < listelm.size(); i++) {
	MCRPrivilege p = new MCRPrivilege((org.jdom.Element) listelm.get(i));
	if (p.isValid())
	  list.add(p);
      }
      MCRUserMgr.instance().updatePrivileges(list);
    } catch (Exception e) {
      throw new MCRException("Error while update privileges form file.", e);
    }
  }

  /**
   * This method just prints a pretty XML output to System.out.
   * @param jdomDoc  the JDOM XML document to be printed
   **/
  private static final void showAsXML(org.jdom.Document jdomDoc)
  throws MCRException
  {
    XMLOutputter outputter = new org.jdom.output.XMLOutputter(Format.getPrettyFormat());
    try {
      outputter.output(jdomDoc, System.out);
    } catch (Exception e) {
      throw new MCRException("Error while show XML to file.");
    }
  }

  /**
   * This method just saves a JDOM document to a file
   * @param jdomDoc  the JDOM XML document to be printed
   * @param outFile  a FileOutputStream object for the output
   */
  private static final void saveToXMLFile(org.jdom.Document jdomDoc, FileOutputStream outFile)
  throws MCRException
  {
    // get encoding
    CONFIG = MCRConfiguration.instance();
    String mcr_encoding = CONFIG.getString("MCR.metadata_default_encoding",
    				           MCRDefaults.ENCODING);

    // Create the output
    XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding(mcr_encoding));
    try {
      outputter.output(jdomDoc, outFile);
    } catch (Exception e) {
      throw new MCRException("Error while save XML to file.");
    }
  }
}
