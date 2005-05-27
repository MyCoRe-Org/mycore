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
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.user.MCRCrypt;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRPrivilege;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

/**
 * This class provides a set of commands for the org.mycore.user management
 * which can be used by the command line interface.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUserCommands extends MCRAbstractCommands {

    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRUserCommands.class
            .getName());

    /**
     * The constructor.
     */
    public MCRUserCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand(
                "init superuser",
                "org.mycore.frontend.cli.MCRUserCommands.initSuperuser",
                "Initialized the user system. This command run only if the user database does not exist.");
        command.add(com);

        com = new MCRCommand("check user data consistency",
                "org.mycore.frontend.cli.MCRUserCommands.checkConsistency",
                "This command check the user system of his consistency.");
        command.add(com);

        com = new MCRCommand(
                "encrypt passwords in user xml file {0} to file {1}",
                "org.mycore.frontend.cli.MCRUserCommands.encryptPasswordsInXMLFile String String",
                "This is a miration tool to change old plain text password enties to encrpted entries.");
        command.add(com);

        com = new MCRCommand(
                "set password for user {0} to {1}",
                "org.mycore.frontend.cli.MCRUserCommands.setPassword String String",
                "This command set a new password for the user. You must be this user or you must have administrator access.");
        command.add(com);

        com = new MCRCommand("set user management to ro mode",
                "org.mycore.frontend.cli.MCRUserCommands.setLock",
                "The command change the management mode of the user system to read-only.");
        command.add(com);

        com = new MCRCommand("set user management to rw mode",
                "org.mycore.frontend.cli.MCRUserCommands.setunLock",
                "The command change the management mode of the user system to read-write.");
        command.add(com);

        com = new MCRCommand("enable user {0}",
                "org.mycore.frontend.cli.MCRUserCommands.enableUser String",
                "The command enabled the user for the access.");
        command.add(com);

        com = new MCRCommand("disable user {0}",
                "org.mycore.frontend.cli.MCRUserCommands.disableUser String",
                "The command disabled the user from the access.");
        command.add(com);

        com = new MCRCommand(
                "update privileges data from file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.updatePrivilegesFromFile String",
                "The command update the privilege data of the user system with data from the file {0}.");
        command.add(com);

        com = new MCRCommand(
                "create group data from file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.createGroupFromFile String",
                "The command create one or more new groups in the user system with data from the file {0}. This create make a constency check.");
        command.add(com);

        com = new MCRCommand(
                "import group data from file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.importGroupFromFile String",
                "The command import one or more groups to the user system with data from the file {0}. This create make a NOT constency check. The command is designd only for repair processes.");
        command.add(com);

        com = new MCRCommand(
                "update group data from file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.updateGroupFromFile String",
                "The command update one or more groups of the user system with data from the file {0}.");
        command.add(com);

        com = new MCRCommand("delete group {0}",
                "org.mycore.frontend.cli.MCRUserCommands.deleteGroup String",
                "");
        command.add(com);

        com = new MCRCommand(
                "add group {0} as member to group {1}",
                "org.mycore.frontend.cli.MCRUserCommands.addMemberGroupToGroup String String",
                "");
        command.add(com);

        com = new MCRCommand(
                "remove group {0} as member from group {1}",
                "org.mycore.frontend.cli.MCRUserCommands.removeMemberGroupFromGroup String String",
                "");
        command.add(com);

        com = new MCRCommand(
                "add to group {0} the privilege {1}",
                "org.mycore.frontend.cli.MCRUserCommands.addPrivilegeToGroup String String",
                "");
        command.add(com);

        com = new MCRCommand(
                "remove from group {0} the privilege {1}",
                "org.mycore.frontend.cli.MCRUserCommands.removePrivilegeFromGroup String String",
                "");
        command.add(com);

        com = new MCRCommand(
                "create user data from file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.createUserFromFile String",
                "");
        command.add(com);

        com = new MCRCommand(
                "import user data from file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.importUserFromFile String",
                "");
        command.add(com);

        com = new MCRCommand(
                "update user data from file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.updateUserFromFile String",
                "");
        command.add(com);

        com = new MCRCommand("delete user {0}",
                "org.mycore.frontend.cli.MCRUserCommands.deleteUser String", "");
        command.add(com);

        com = new MCRCommand(
                "add user {0} as member to group {1}",
                "org.mycore.frontend.cli.MCRUserCommands.addMemberUserToGroup String String",
                "");
        command.add(com);

        com = new MCRCommand(
                "remove user {0} as member from group {1}",
                "org.mycore.frontend.cli.MCRUserCommands.removeMemberUserFromGroup String String",
                "");
        command.add(com);

        com = new MCRCommand("list all privileges",
                "org.mycore.frontend.cli.MCRUserCommands.listAllPrivileges", "");
        command.add(com);

        com = new MCRCommand("list all groups",
                "org.mycore.frontend.cli.MCRUserCommands.listAllGroups", "");
        command.add(com);

        com = new MCRCommand("list group {0}",
                "org.mycore.frontend.cli.MCRUserCommands.listGroup String", "");
        command.add(com);

        com = new MCRCommand("list all users",
                "org.mycore.frontend.cli.MCRUserCommands.listAllUsers", "");
        command.add(com);

        com = new MCRCommand("list user {0}",
                "org.mycore.frontend.cli.MCRUserCommands.listUser String", "");
        command.add(com);

        com = new MCRCommand(
                "save all privileges to file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.saveAllPrivilegesToFile String",
                "");
        command.add(com);

        com = new MCRCommand(
                "save all groups to file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.saveAllGroupsToFile String",
                "");
        command.add(com);

        com = new MCRCommand(
                "save group {0} to file {1}",
                "org.mycore.frontend.cli.MCRUserCommands.saveGroupToFile String String",
                "");
        command.add(com);

        com = new MCRCommand(
                "save all users to file {0}",
                "org.mycore.frontend.cli.MCRUserCommands.saveAllUsersToFile String",
                "");
        command.add(com);

        com = new MCRCommand(
                "save user {0} to file {1}",
                "org.mycore.frontend.cli.MCRUserCommands.saveUserToFile String String",
                "");
        command.add(com);

    }

    /**
     * This method initializes the user and group system an creates a superuser
     * with values set in mycore.properties.private As 'super' default, if no
     * properties were set, mcradmin with password mycore will be used.
     */
    public static void initSuperuser() throws MCRException {
        String suser = CONFIG.getString("MCR.users_superuser_username",
                "mcradmin");
        String spasswd = CONFIG.getString("MCR.users_superuser_userpasswd",
                "mycore");
        String sgroup = CONFIG.getString("MCR.users_superuser_groupname",
                "mcrgroup");
        String guser = CONFIG.getString("MCR.users_guestuser_username", "gast");
        String gpasswd = CONFIG.getString("MCR.users_guestuser_userpasswd",
                "gast");
        String ggroup = CONFIG.getString("MCR.users_guestuser_groupname",
                "mcrgast");

        // If CONFIGuration parameter defines that we use password encryption:
        // encrypt!
        String useCrypt = CONFIG.getString("MCR.users_use_password_encryption",
                "false");
        boolean useEncryption = (useCrypt.trim().equals("true")) ? true : false;
        if (useEncryption) {
            String cryptPwd = MCRCrypt.crypt(spasswd);
            spasswd = cryptPwd;
            cryptPwd = MCRCrypt.crypt(gpasswd);
            gpasswd = cryptPwd;
        }

        MCRSessionMgr.getCurrentSession().setCurrentUserID(suser);
        try {
            if (MCRUserMgr.instance().retrieveUser(suser) != null)
                if (MCRUserMgr.instance().retrieveGroup(sgroup) != null)
                    LOGGER.error("The superuser already exists!");
            return;
        } catch (Exception e) {
        }

        try {
            ArrayList privList = new ArrayList();
            privList.add(new MCRPrivilege("create user",
                    "Users with this privilege may create new users."));
            privList.add(new MCRPrivilege("create group",
                    "Users with this privilege may create new groups."));
            privList.add(new MCRPrivilege("delete user",
                    "Users with this privilege may delete other users."));
            privList.add(new MCRPrivilege("delete group",
                    "Users with this privilege may delete groups."));
            privList
                    .add(new MCRPrivilege("modify user",
                            "Users with this privilege may modify data of other users."));
            privList.add(new MCRPrivilege("modify group",
                    "Users with this privilege may modify data of groups."));
            privList
                    .add(new MCRPrivilege("modify privileges",
                            "Users with this privilege may modify data of privileges."));
            privList
                    .add(new MCRPrivilege("user administrator",
                            "Users with this privilege have administrator rights in the system."));
            privList
                    .add(new MCRPrivilege("list all users",
                            "Users with this privilege may list the users of the system."));
            privList
                    .add(new MCRPrivilege("list all privileges",
                            "Users with this privilege may list the privileges of the system."));
            privList
                    .add(new MCRPrivilege(
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
                    "The superuser group", admUserIDs, admGroupIDs, mbrUserIDs,
                    mbrGroupIDs, groupIDs, privileges);

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
                    "The guest group", admUserIDs, admGroupIDs, mbrUserIDs,
                    mbrGroupIDs, groupIDs, privileges);

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
                    "Superuser", spasswd, sgroup, groupIDs, null, null, null,
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null);

            MCRUserMgr.instance().initializeUser(u, suser);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser.", e);
        }
        LOGGER.info("The user " + suser + " with password "
                + CONFIG.getString("MCR.users_superuser_userpasswd", "mycore")
                + " is installed.");

        // the guest
        try {
            ArrayList groupIDs = new ArrayList();
            groupIDs.add(ggroup);

            MCRUser u = new MCRUser(2, guser, suser, null, null, true, true,
                    "guest", gpasswd, ggroup, groupIDs, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null,
                    null, null);

            MCRUserMgr.instance().initializeUser(u, suser);
        } catch (Exception e) {
            throw new MCRException("Can't create the guest user.", e);
        }
        LOGGER.info("The user " + guser + " with password "
                + CONFIG.getString("MCR.users_guestuser_userpasswd", "gast")
                + " is installed.");

        // check all
        MCRSessionMgr.getCurrentSession().setCurrentUserID(suser);
        MCRUserMgr.instance().checkConsistency();
        LOGGER.info("");
    }

    /**
     * This method checks the data consistency of the user management and should
     * be called after a system crash or after importing data from files,
     * respectively.
     */
    public static void checkConsistency() throws Exception {
        MCRUserMgr.instance().checkConsistency();
    }

    /**
     * This method invokes MCRUserMgr.deleteGroup() and permanently removes a
     * group from the system.
     * 
     * @param groupID
     *            the ID of the group which will be deleted
     */
    public static void deleteGroup(String groupID) throws Exception {
        MCRUserMgr.instance().deleteGroup(groupID);
        LOGGER.info("Group ID " + groupID + " deleted!");
    }

    /**
     * This method invokes MCRUserMgr.deleteUser() and permanently removes a
     * user from the system.
     * 
     * @param userID
     *            the ID of the user which will be deleted
     */
    public static void deleteUser(String userID) throws Exception {
        MCRUserMgr.instance().deleteUser(userID);
        LOGGER.info("User ID " + userID + " deleted!");
    }

    /**
     * This method invokes MCRUserMgr.enableUser() that enables a user
     * 
     * @param userID
     *            the ID of the user which will be enabled
     */
    public static void enableUser(String userID) throws Exception {
        MCRUserMgr.instance().enableUser(userID);
        LOGGER.info("User ID " + userID + " enabled!");
    }

    /**
     * A given XML file containing user data with cleartext passwords must be
     * converted prior to loading the user data into the system. This method
     * reads all user objects in the given XML file, encrypts the passwords and
     * writes them back to a file with name original-file-name_encrypted.xml.
     * 
     * @param oldFile
     *            the filename of the user data input
     * @param newFile
     *            the filename of the user data output (encrypted passwords)
     */
    public static final void encryptPasswordsInXMLFile(String oldFile,
            String newFile) throws MCRException {
        if (!checkFilename(oldFile))
            return;
        LOGGER.info("Reading file " + oldFile + " ...");
        try {
            org.jdom.Document doc = MCRXMLHelper.parseURI(oldFile, true);
            org.jdom.Element rootelm = doc.getRootElement();
            if (!rootelm.getName().equals("mycoreuser")) {
                throw new MCRException(
                        "These data do not correspond to a user.");
            }
            List listelm = rootelm.getChildren(); // the <user> elements
            for (int i = 0; i < listelm.size(); i++) {
                // Get the passwords, encrypt and write it back into the
                // document
                org.jdom.Element elm = (org.jdom.Element) listelm.get(i);
                String passwd = elm.getChildTextTrim("user.password");
                String encryptedPasswd = MCRCrypt.crypt(passwd);
                elm.getChild("user.password").setText(encryptedPasswd);
            }
            FileOutputStream outFile = new FileOutputStream(newFile);
            saveToXMLFile(doc, outFile);
        } catch (Exception e) {
            throw new MCRException(
                    "Error while encrypting cleartext passwords in user xml file.",
                    e);
        }
    }

    /**
     * This method invokes MCRUserMgr.disableUser() that disables a user
     * 
     * @param userID
     *            the ID of the user which will be enabled
     */
    public static void disableUser(String userID) throws Exception {
        MCRUserMgr.instance().disableUser(userID);
        LOGGER.info("User ID " + userID + " disabled!");
    }

    /**
     * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList
     * of all users stored in the persistent datastore.
     */
    public static void listAllUsers() throws Exception {
        ArrayList users = MCRUserMgr.instance().getAllUserIDs();
        System.out.println();
        for (int i = 0; i < users.size(); i++) {
            listUser((String) users.get(i));
        }
    }

    /**
     * This method invokes MCRUserMgr.getAllGroupIDs() and retrieves a ArrayList
     * of all groups stored in the persistent datastore.
     */
    public static void listAllGroups() throws Exception {
        ArrayList groups = MCRUserMgr.instance().getAllGroupIDs();
        System.out.println();
        for (int i = 0; i < groups.size(); i++) {
            listGroup((String) groups.get(i));
        }
    }

    /**
     * This method invokes MCRUserMgr.getAllPrivileges() and retrieves a
     * ArrayList of all privileges stored in the persistent datastore.
     */
    public static void listAllPrivileges() throws MCRException {
        try {
            ArrayList privs = MCRUserMgr.instance().getAllPrivileges();
            System.out.println();
            for (int i = 0; i < privs.size(); i++) {
                MCRPrivilege currentPriv = (MCRPrivilege) privs.get(i);
                System.out.println("       " + currentPriv.getName());
                System.out
                        .println("           " + currentPriv.getDescription());
            }
        } catch (Exception e) {
            throw new MCRException("Error while command saveAllGroupsToFile()",
                    e);
        }
    }

    /**
     * This command takes a file name as a parameter, retrieves all groups from
     * MCRUserMgr as JDOM document and saves this to the given file.
     * 
     * @param filename
     *            Name of the file the groups will be saved to
     */
    public static void saveAllGroupsToFile(String filename) throws MCRException {
        try {
            org.jdom.Document jdomDoc = MCRUserMgr.instance().getAllGroups();
            FileOutputStream outFile = new FileOutputStream(filename);
            saveToXMLFile(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while command saveAllGroupsToFile()",
                    e);
        }
    }

    /**
     * This command takes a file name as a parameter, retrieves all privileges
     * from MCRUserMgr as JDOM document and saves this to the given file.
     * 
     * @param filename
     *            Name of the file the privileges will be saved to
     */
    public static void saveAllPrivilegesToFile(String filename)
            throws MCRException {
        try {
            org.jdom.Document jdomDoc = MCRUserMgr.instance()
                    .getAllPrivilegesAsJDOMDocument();
            FileOutputStream outFile = new FileOutputStream(filename);
            saveToXMLFile(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException(
                    "Error while command saveAllPrivilegesToFile()", e);
        }
    }

    /**
     * This command takes a file name as a parameter, retrieves all users from
     * MCRUserMgr as JDOM document and saves this to the given file.
     * 
     * @param filename
     *            Name of the file the users will be saved to
     */
    public static void saveAllUsersToFile(String filename) throws MCRException {
        try {
            org.jdom.Document jdomDoc = MCRUserMgr.instance().getAllUsers();
            FileOutputStream outFile = new FileOutputStream(filename);
            saveToXMLFile(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while command saveAllUsersToFile()",
                    e);
        }
    }

    /**
     * This command takes a groupID and file name as a parameter, retrieves the
     * group from MCRUserMgr as JDOM document and saves this to the given file.
     * 
     * @param groupID
     *            ID of the group to be saved
     * @param filename
     *            Name of the file the groups will be saved to
     */
    public static void saveGroupToFile(String groupID, String filename)
            throws Exception {
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
     * @param userID
     *            ID of the user to be saved
     * @param filename
     *            Name of the file the user will be saved to
     */
    public static void saveUserToFile(String userID, String filename)
            throws MCRException {
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
     * @param userID
     *            the ID of the user for which the password will be set
     */
    public static void setPassword(String userID, String password)
            throws MCRException {
        if (password == null)
            return;
        MCRUserMgr.instance().setPassword(userID, password);
        LOGGER.info("The new password was set.");
    }

    /**
     * This method sets the user management component to read only mode
     */
    public static void setLock() throws MCRException {
        MCRUserMgr.instance().setLock(true);
        LOGGER
                .info("Write access to the user component persistent database now is denied.");
    }

    /**
     * This method sets the user management component to read/write access mode
     */
    public static void unLock() throws MCRException {
        MCRUserMgr.instance().setLock(false);
        LOGGER
                .info("Write access to the user component persistent database now is allowed.");
    }

    /**
     * This method invokes MCRUserMgr.retrieveGroup() and then works with the
     * retrieved group object to get an XML-Representation.
     * 
     * @param groupID
     *            the ID of the group for which the XML-representation is needed
     */
    public static final void listGroup(String groupID) throws MCRException {
        MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
        StringBuffer sb = new StringBuffer();
        System.out.println();
        sb.append("       group=").append(group.getID());
        System.out.println(sb.toString());
        ArrayList ar = group.getMemberGroupIDs();
        for (int i = 0; i < ar.size(); i++) {
            sb = new StringBuffer();
            sb.append("          groups in this group=").append(
                    (String) ar.get(i));
            System.out.println(sb.toString());
        }
        ar = group.getMemberUserIDs();
        for (int i = 0; i < ar.size(); i++) {
            sb = new StringBuffer();
            sb.append("          user in this group=").append(
                    (String) ar.get(i));
            System.out.println(sb.toString());
        }
        ar = group.getAllPrivileges();
        for (int i = 0; i < ar.size(); i++) {
            sb = new StringBuffer();
            sb.append("          has privileg=").append((String) ar.get(i));
            System.out.println(sb.toString());
        }
        //org.jdom.Document jdomDoc = group.toJDOMDocument();
        //showAsXML(jdomDoc);
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the
     * retrieved user object to get an XML-Representation.
     * 
     * @param userID
     *            the ID of the user for which the XML-representation is needed
     */
    public static final void listUser(String userID) throws MCRException {
        MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
        StringBuffer sb = new StringBuffer();
        System.out.println();
        sb.append("       user=").append(user.getID()).append("   number=")
                .append(user.getNumID()).append("   update=").append(
                        user.isUpdateAllowed()).append("   enabled=").append(
                        user.isEnabled());
        System.out.println(sb.toString());
        sb = new StringBuffer();
        sb.append("          primary group=").append(user.getPrimaryGroupID());
        System.out.println(sb.toString());
        ArrayList ar = user.getAllGroupIDs();
        for (int i = 0; i < ar.size(); i++) {
            sb = new StringBuffer();
            sb.append("          member in group=").append((String) ar.get(i));
            System.out.println(sb.toString());
        }
        ar = user.getPrivileges();
        for (int i = 0; i < ar.size(); i++) {
            sb = new StringBuffer();
            sb.append("          has privileg=").append((String) ar.get(i));
            System.out.println(sb.toString());
        }
        //org.jdom.Document jdomDoc = user.toJDOMDocument();
        //showAsXML(jdomDoc);
    }

    /**
     * Check the file name
     * 
     * @param filename
     *            the filename of the user data input
     * @return true if the file name is okay
     */
    private static final boolean checkFilename(String filename) {
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
     * 
     * @param filename
     *            the filename of the user data input
     */
    public static final void createUserFromFile(String filename) {
        String useCrypt = CONFIG.getString("MCR.users_use_password_encryption",
                "false");
        boolean useEncryption = (useCrypt.trim().equals("true")) ? true : false;
        createUserFromFile(filename, useEncryption);
    }

    /**
     * This method invokes MCRUserMgr.createUser() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     * @param useEncryption
     *            flag to determine whether we use password encryption or not
     */
    private static final void createUserFromFile(String filename,
            boolean useEncryption) throws MCRException {
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
                MCRUser u = new MCRUser((org.jdom.Element) listelm.get(i),
                        useEncryption);
                MCRUserMgr.instance().createUser(u);
            }
        } catch (Exception e) {
            throw new MCRException("Error while loading user data.", e);
        }
    }

    /**
     * This method imports user data from an xml file. It is assumed that the
     * user data previously have been exported from a running Mycore system.
     * That is, if the running system uses password encryption, the passwords in
     * the xml file already are encrypted, too, so that the must not be
     * encrypted again.
     * 
     * @param filename
     *            the filename of the user data input
     */
    public static final void importUserFromFile(String filename)
            throws MCRException {
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
                MCRUser u = new MCRUser((org.jdom.Element) listelm.get(i),
                        false); // do not encrypt passwords
                MCRUserMgr.instance().importUserObject(u);
            }
        } catch (Exception e) {
            throw new MCRException("Error while loading user data.", e);
        }
    }

    /**
     * This method invokes MCRUserMgr.createGroup() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     */
    public static final void createGroupFromFile(String filename)
            throws MCRException {
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
     * This method imports group data from an xml file. It simply calles
     * createGroupFromFile().
     * 
     * @param filename
     *            the filename of the group data input
     */
    public static final void importGroupFromFile(String filename)
            throws MCRException {
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
     * 
     * @param filename
     *            the filename of the user data input
     */
    public static final void updateUserFromFile(String filename) {
        String useCrypt = CONFIG.getString("MCR.users_use_password_encryption",
                "false");
        boolean useEncryption = (useCrypt.trim().equals("true")) ? true : false;
        updateUserFromFile(filename, useEncryption);
    }

    /**
     * This method invokes MCRUserMgr.updateUser() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     * @param useEncryption
     *            flag to determine whether we use password encryption or not
     */
    private static final void updateUserFromFile(String filename,
            boolean useEncryption) throws MCRException {
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
                MCRUser u = new MCRUser((org.jdom.Element) listelm.get(i),
                        useEncryption);
                MCRUserMgr.instance().updateUser(u);
            }
        } catch (Exception e) {
            throw new MCRException("Error while updating a user from file.", e);
        }
    }

    /**
     * This method invokes MCRUserMgr.updateGroup() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     */
    public static final void updateGroupFromFile(String filename)
            throws MCRException {
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
     * @param mbrGroupID
     *            the ID of the group which will be a member of the group
     *            represented by groupID
     * @param groupID
     *            the ID of the group to which the group with ID mbrGroupID will
     *            be added
     * @throws MCRException
     */
    public static final void addMemberGroupToGroup(String mbrGroupID,
            String groupID) throws MCRException {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            group.addMemberGroupID(mbrGroupID);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while adding group " + mbrGroupID
                    + " to group " + groupID + ".", e);
        }
    }

    /**
     * This method removes a member group from another group
     * 
     * @param mbrGroupID
     *            the ID of the group which will be removed from the group
     *            represented by groupID
     * @param groupID
     *            the ID of the group from which the group with ID mbrGroupID
     *            will be removed
     * @throws MCRException
     */
    public static final void removeMemberGroupFromGroup(String mbrGroupID,
            String groupID) throws MCRException {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            group.removeMemberGroupID(mbrGroupID);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while removing group " + mbrGroupID
                    + " from group " + groupID + ".", e);
        }
    }

    /**
     * The method add a new privilege to a group.
     * 
     * @param groupID
     *            the group name
     * @param priv
     *            the privilege String
     */
    public static final void addPrivilegeToGroup(String groupID, String priv) {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            ArrayList ar = MCRUserMgr.instance().getAllPrivileges();
            boolean test = false;
            for (int i = 0; i < ar.size(); i++) {
                if (((MCRPrivilege) ar.get(i)).getName().equals(priv)) {
                    test = true;
                    break;
                }
            }
            if (!test) {
                throw new MCRException("The privilege " + priv
                        + " to add does not exist!");
            }
            group.addPrivilege(priv);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while adding privilege " + priv
                    + " to group " + groupID + ".", e);
        }
    }

    /**
     * The method remove a privilege from a group.
     * 
     * @param group
     *            the group name
     * @param priv
     *            the privilege String
     */
    public static final void removePrivilegeFromGroup(String groupID,
            String priv) {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            if (!group.hasPrivilege(priv)) {
                throw new MCRException("The group " + groupID
                        + " has not the privilege " + priv + "!");
            }
            group.removePrivilege(priv);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while adding privilege " + priv
                    + " to group " + groupID + ".", e);
        }
    }

    /**
     * This method adds a user as a member to a group
     * 
     * @param mbrUserID
     *            the ID of the user which will be a member of the group
     *            represented by groupID
     * @param groupID
     *            the ID of the group to which the user with ID mbrUserID will
     *            be added
     * @throws MCRException
     */
    public static final void addMemberUserToGroup(String mbrUserID,
            String groupID) throws MCRException {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            group.addMemberUserID(mbrUserID);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while adding group " + mbrUserID
                    + " to group " + groupID + ".", e);
        }
    }

    /**
     * This method removes a member user from a group
     * 
     * @param mbrUserID
     *            the ID of the user which will be removed from the group
     *            represented by groupID
     * @param groupID
     *            the ID of the group from which the user with ID mbrUserID will
     *            be removed
     * @throws MCRException
     */
    public static final void removeMemberUserFromGroup(String mbrUserID,
            String groupID) throws MCRException {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            group.removeMemberUserID(mbrUserID);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while removing group " + mbrUserID
                    + " from group " + groupID + ".", e);
        }
    }

    /**
     * This method invokes MCRUserMgr.updatePrivileges() with data from a file.
     * 
     * @param filename
     *            the filename of the privileg data input
     */
    public static final void updatePrivilegesFromFile(String filename)
            throws MCRException {
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
                MCRPrivilege p = new MCRPrivilege((org.jdom.Element) listelm
                        .get(i));
                if (p.isValid())
                    list.add(p);
            }
            MCRUserMgr.instance().updatePrivileges(list);
        } catch (Exception e) {
            throw new MCRException("Error while update privileges form file.",
                    e);
        }
    }

    /**
     * This method just prints a pretty XML output to System.out.
     * 
     * @param jdomDoc
     *            the JDOM XML document to be printed
     */
    private static final void showAsXML(org.jdom.Document jdomDoc)
            throws MCRException {
        XMLOutputter outputter = new org.jdom.output.XMLOutputter(Format
                .getPrettyFormat());
        try {
            outputter.output(jdomDoc, System.out);
        } catch (Exception e) {
            throw new MCRException("Error while show XML to file.");
        }
    }

    /**
     * This method just saves a JDOM document to a file
     * 
     * @param jdomDoc
     *            the JDOM XML document to be printed
     * @param outFile
     *            a FileOutputStream object for the output
     */
    private static final void saveToXMLFile(org.jdom.Document jdomDoc,
            FileOutputStream outFile) throws MCRException {
        // get encoding
        CONFIG = MCRConfiguration.instance();
        String mcr_encoding = CONFIG.getString("MCR.metadata_default_encoding",
                MCRDefaults.ENCODING);

        // Create the output
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat()
                .setEncoding(mcr_encoding));
        try {
            outputter.output(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while save XML to file.");
        }
    }
}