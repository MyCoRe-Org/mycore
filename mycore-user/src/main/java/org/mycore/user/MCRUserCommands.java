/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user;

import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRCrypt;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.xml.sax.SAXParseException;

/**
 * This class provides a set of commands for the org.mycore.user management
 * which can be used by the command line interface.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date: 2007-12-19 17:31:52 +0100 (Mi, 19 Dez
 *          2007) $
 */
public class MCRUserCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRUserCommands.class.getName());

    private static final String SYSTEM = MCRConfiguration.instance().getString("MCR.CommandLineInterface.SystemName", "MyCoRe") + ":";

    /**
     * The constructor.
     */
    public MCRUserCommands() {
        super();

        MCRCommand com = null;

        addCommand(new MCRCommand("change to user {0} with {1}", "org.mycore.user.MCRUserCommands.changeToUser String String",
            "Change the user {0} with the given password in {1}."));
        addCommand(new MCRCommand("login {0}", "org.mycore.user.MCRUserCommands.login String", "Start the login dialog for the user {0}."));

        com = new MCRCommand("init superuser", "org.mycore.user.MCRUserCommands.initSuperuser",
            "Initialized the user system. This command runs only if the user database does not exist.");
        addCommand(com);

        com = new MCRCommand("check user data consistency", "org.mycore.user.MCRUserCommands.checkConsistency",
            "This command checks the user system for its consistency.");
        addCommand(com);

        com = new MCRCommand("encrypt passwords in user xml file {0} to file {1}", "org.mycore.user.MCRUserCommands.encryptPasswordsInXMLFile String String",
            "This is a migration tool to change old plain text password entries to encrpted entries.");
        addCommand(com);

        com = new MCRCommand("set password for user {0} to {1}", "org.mycore.user.MCRUserCommands.setPassword String String",
            "This command sets a new password for the user. You must be this user or you must have administrator access.");
        addCommand(com);

        com = new MCRCommand("set user management to ro mode", "org.mycore.user.MCRUserCommands.setLock",
            "The command changes the management mode of the user system to read-only.");
        addCommand(com);

        com = new MCRCommand("set user management to rw mode", "org.mycore.user.MCRUserCommands.setunLock",
            "The command changes the management mode of the user system to read-write.");
        addCommand(com);

        com = new MCRCommand("enable user {0}", "org.mycore.user.MCRUserCommands.enableUser String", "The command enables the user for the access.");
        addCommand(com);

        com = new MCRCommand("disable user {0}", "org.mycore.user.MCRUserCommands.disableUser String", "The command disables the user from the access.");
        addCommand(com);

        com = new MCRCommand("create group data from file {0}", "org.mycore.user.MCRUserCommands.createGroupFromFile String",
            "The command creates one or more new groups in the user system with data from the file {0}. This create makes a constency check.");
        addCommand(com);

        com = new MCRCommand("import user system from files {0} {1}", "org.mycore.user.MCRUserCommands.importUserSystemFromFiles String String",
            "The command imports the user system with data from the group file {0} and the user file {1}. The command is designd only for recovery processes.");
        addCommand(com);

        com = new MCRCommand("delete group {0}", "org.mycore.user.MCRUserCommands.deleteGroup String",
            "The command delete the group {0} from the user system, but only if it has no user members.");
        addCommand(com);

        com = new MCRCommand("create user data from file {0}", "org.mycore.user.MCRUserCommands.createUserFromFile String",
            "The command create one or more new users in the user system with data from the file {0}.");
        addCommand(com);

        com = new MCRCommand("update user data from file {0}", "org.mycore.user.MCRUserCommands.updateUserFromFile String",
            "The command update one or more users in the user system with data from the file {0}.");
        addCommand(com);

        com = new MCRCommand("delete user {0}", "org.mycore.user.MCRUserCommands.deleteUser String", "The command delete the user {0}.");
        addCommand(com);

        com = new MCRCommand("add user {0} as member to group {1}", "org.mycore.user.MCRUserCommands.addMemberUserToGroup String String",
            "The command add a user {0} as secondary member in the group {1}.");
        addCommand(com);

        com = new MCRCommand("remove user {0} as member from group {1}", "org.mycore.user.MCRUserCommands.removeMemberUserFromGroup String String",
            "The command remove the user {0} as secondary member from the group {1}.");
        addCommand(com);

        com = new MCRCommand("list all groups", "org.mycore.user.MCRUserCommands.listAllGroups", "The command list all groups.");
        addCommand(com);

        com = new MCRCommand("list group {0}", "org.mycore.user.MCRUserCommands.listGroup String", "The command list the group {0}.");
        addCommand(com);

        com = new MCRCommand("list all users", "org.mycore.user.MCRUserCommands.listAllUsers", "The command list all users.");
        addCommand(com);

        com = new MCRCommand("list user {0}", "org.mycore.user.MCRUserCommands.listUser String", "The command list the user {0}.");
        addCommand(com);

        com = new MCRCommand("export all groups to file {0}", "org.mycore.user.MCRUserCommands.exportAllGroupsToFile String",
            "The command exports all group data to the file {0}.");
        addCommand(com);

        com = new MCRCommand("export group {0} to file {1}", "org.mycore.user.MCRUserCommands.exportGroupToFile String String",
            "The command exports the data of group {0} to the file {1}.");
        addCommand(com);

        com = new MCRCommand("export all users to file {0}", "org.mycore.user.MCRUserCommands.exportAllUsersToFile String",
            "The command exports all user data to the file {0}.");
        addCommand(com);

        com = new MCRCommand("export user {0} to file {1}", "org.mycore.user.MCRUserCommands.exportUserToFile String String",
            "The command exports the data of user {0} to the file {1}.");
        addCommand(com);
    }

    /**
     * This command changes the user of the session context to a new user.
     * 
     * @param user
     *            the new user ID
     * @param password
     *            the password of the new user
     */
    public static void changeToUser(String user, String password) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        System.out.println(SYSTEM + " The old user ID is " + session.getUserInformation().getUserID());
        if (MCRUserMgr.instance().login(user, password)) {
            System.out.println(SYSTEM + " The new user ID is " + session.getUserInformation().getUserID());
        } else {
            LOGGER.warn("Wrong password, no changes of user ID in session context!");
        }
    }

    /**
     * This command changes the user of the session context to a new user.
     * 
     * @param user
     *            the new user ID
     */
    public static void login(String user) {
        char[] password = {};
        do {
            password = System.console().readPassword("{0} Enter password for user {1} :> ", SYSTEM, user);
        } while (password.length == 0);

        changeToUser(user, String.valueOf(password));
    }

    /**
     * This method initializes the user and group system an creates a superuser
     * with values set in mycore.properties.private As 'super' default, if no
     * properties were set, mcradmin with password mycore will be used.
     */
    public static void initSuperuser() throws MCRException {
        final String suser = CONFIG.getString("MCR.Users.Superuser.UserName", "administrator");
        final String spasswd = CONFIG.getString("MCR.Users.Superuser.UserPasswd", "alleswirdgut");
        final String sgroup = CONFIG.getString("MCR.Users.Superuser.GroupName", "admingroup");

        // If CONFIGuration parameter defines that we use password encryption:
        // encrypt!
        boolean useEncryption = CONFIG.getBoolean("MCR.Users.UsePasswordEncryption", false);

        //set to super user
        MCRSessionMgr.getCurrentSession().setUserInformation(new MCRUserInformation() {

            @Override
            public boolean isUserInRole(String role) {
                return true;
            }

            @Override
            public String getUserID() {
                return suser;
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        });

        if (MCRUserMgr.instance().existUser(suser)) {
            LOGGER.error("The superuser already exists!");
            return;
        }

        // the superuser group
        try {
            ArrayList<String> admUserIDs = new ArrayList<String>();

            ArrayList<String> admGroupIDs = new ArrayList<String>();
            ArrayList<String> mbrUserIDs = new ArrayList<String>();

            MCRGroup g = new MCRGroup(sgroup, suser, null, null, "The superuser group", admUserIDs, admGroupIDs, mbrUserIDs);

            MCRUserMgr.instance().initializeGroup(g, suser);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser group.", e);
        }

        LOGGER.info("The group " + sgroup + " is installed.");

        // the superuser
        try {
            ArrayList<String> groupIDs = new ArrayList<String>();
            groupIDs.add(sgroup);

            MCRUser u = new MCRUser(1, suser, suser, null, null, true, true, "Superuser", useEncryption ? MCRCrypt.crypt(spasswd) : spasswd, sgroup, groupIDs,
                null);

            MCRUserMgr.instance().initializeUser(u, suser);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser.", e);
        }

        LOGGER.info(MessageFormat.format("The user {0} with password {1} is installed.", suser, spasswd));

        // the default group
        try {
            ArrayList<String> admUserIDs = new ArrayList<String>();

            ArrayList<String> admGroupIDs = new ArrayList<String>();
            ArrayList<String> mbrUserIDs = new ArrayList<String>();

            MCRGroup g = new MCRGroup(MCRGroup.getDefaultGroupID(), suser, null, null, MessageFormat.format("The {0} group", MCRGroup.getDefaultGroupID()),
                admUserIDs, admGroupIDs, mbrUserIDs);

            MCRUserMgr.instance().initializeGroup(g, suser);
        } catch (Exception e) {
            throw new MCRException("Can't create the default group.", e);
        }

        LOGGER.info("The group " + sgroup + " is installed.");

        MCRUserMgr.instance().login(suser, spasswd);
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
    }

    /**
     * This method invokes MCRUserMgr.enableUser() that enables a user
     * 
     * @param userID
     *            the ID of the user which will be enabled
     */
    public static void enableUser(String userID) throws Exception {
        MCRUserMgr.instance().enableUser(userID);
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
    public static void encryptPasswordsInXMLFile(String oldFile, String newFile) throws MCRException {
        if (!checkFilename(oldFile)) {
            return;
        }

        LOGGER.info("Reading file " + oldFile + " ...");

        try {
            Document doc = MCRXMLParserFactory.getValidatingParser().parseXML(new MCRFileContent(oldFile));
            Element rootelm = doc.getRootElement();

            if (!rootelm.getName().equals("mycoreuser")) {
                throw new MCRException("These data do not correspond to a user.");
            }

            @SuppressWarnings("unchecked")
            List<Element> listelm = rootelm.getChildren(); // the <user>
            // elements

            for (int i = 0; i < listelm.size(); i++) {
                // Get the passwords, encrypt and write it back into the
                // document
                Element elm = (Element) listelm.get(i);
                String passwd = elm.getChildTextTrim("user.password");
                String encryptedPasswd = MCRCrypt.crypt(passwd);
                elm.getChild("user.password").setText(encryptedPasswd);
            }

            FileOutputStream outFile = new FileOutputStream(newFile);
            saveToXMLFile(doc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while encrypting cleartext passwords in user xml file.", e);
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
    }

    /**
     * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList
     * of all users stored in the persistent datastore.
     */
    public static void listAllUsers() throws Exception {
        List<String> users = MCRUserMgr.instance().getAllUserIDs();

        for (String uid : users) {
            listUser(uid);
        }
    }

    /**
     * This method invokes MCRUserMgr.getAllGroupIDs() and retrieves a ArrayList
     * of all groups stored in the persistent datastore.
     */
    public static void listAllGroups() throws Exception {
        List<String> groups = MCRUserMgr.instance().getAllGroupIDs();

        for (String gid : groups) {
            listGroup(gid);
        }
    }

    /**
     * This command takes a file name as a parameter, retrieves all groups from
     * MCRUserMgr as JDOM document and export this to the given file.
     * 
     * @param filename
     *            Name of the file in this the groups will be exported
     */
    public static void exportAllGroupsToFile(String filename) throws MCRException {
        try {
            Document jdomDoc = MCRUserMgr.instance().getAllGroups();
            FileOutputStream outFile = new FileOutputStream(filename);
            LOGGER.info("Writing to file " + filename + " ...");
            saveToXMLFile(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while command saveAllGroupsToFile()", e);
        }
    }

    /**
     * This command takes a file name as a parameter, retrieves all users from
     * MCRUserMgr as JDOM document and export this to the given file.
     * 
     * @param filename
     *            Name of the file in this the users will be exported
     */
    public static void exportAllUsersToFile(String filename) throws MCRException {
        try {
            Document jdomDoc = MCRUserMgr.instance().getAllUsers();
            FileOutputStream outFile = new FileOutputStream(filename);
            LOGGER.info("Writing to file " + filename + " ...");
            saveToXMLFile(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while command saveAllUsersToFile()", e);
        }
    }

    /**
     * This command takes a groupID and file name as a parameter, retrieves the
     * group from MCRUserMgr as JDOM document and export this to the given file.
     * 
     * @param groupID
     *            ID of the group to be saved
     * @param filename
     *            Name of the file to store the exported group
     */
    public static void exportGroupToFile(String groupID, String filename) throws Exception {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            Document jdomDoc = group.toJDOMDocument();
            Element elmroot = jdomDoc.getRootElement();
            Element elmgroup = elmroot.getChild("group");
            Element elmmember = elmgroup.getChild("group.members");
            if (elmmember != null) {
                elmgroup.removeChild("group.members");
            }
            FileOutputStream outFile = new FileOutputStream(filename);
            LOGGER.info("Writing to file " + filename + " ...");
            saveToXMLFile(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while command saveGroupToFile()", e);
        }
    }

    /**
     * This command takes a userID and file name as a parameter, retrieves the
     * user from MCRUserMgr as JDOM document and export this to the given file.
     * 
     * @param userID
     *            ID of the user to be saved
     * @param filename
     *            Name of the file to store the exported user
     */
    public static void exportUserToFile(String userID, String filename) throws MCRException {
        try {
            MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
            Document jdomDoc = user.toJDOMDocument();
            FileOutputStream outFile = new FileOutputStream(filename);
            LOGGER.info("Writing to file " + filename + " ...");
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
    public static void setPassword(String userID, String password) throws MCRException {
        MCRUserMgr.instance().setPassword(userID, password);
    }

    /**
     * This method sets the user management component to read only mode
     */
    public static void setLock() throws MCRException {
        MCRUserMgr.instance().setLock(true);
    }

    /**
     * This method sets the user management component to read/write access mode
     */
    public static void unLock() throws MCRException {
        MCRUserMgr.instance().setLock(false);
    }

    /**
     * This method invokes MCRUserMgr.retrieveGroup() and then works with the
     * retrieved group object to get an XML-Representation.
     * 
     * @param groupID
     *            the ID of the group for which the XML-representation is needed
     */
    public static void listGroup(String groupID) throws MCRException {
        MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
        StringBuffer sb = new StringBuffer();
        LOGGER.info("");
        sb.append("       group=").append(group.getID());
        LOGGER.info(sb.toString());
        ArrayList<String> ar = group.getMemberUserIDs();
        for (int i = 0; i < ar.size(); i++) {
            sb = new StringBuffer();
            sb.append("          user in this group=").append((String) ar.get(i));
            LOGGER.info(sb.toString());
        }
        LOGGER.info("");
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the
     * retrieved user object to get an XML-Representation.
     * 
     * @param userID
     *            the ID of the user for which the XML-representation is needed
     */
    public static void listUser(String userID) throws MCRException {
        MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
        LOGGER.info("");
        StringBuffer sb = new StringBuffer();
        sb.append("       user=")
            .append(user.getID())
            .append("   real name=")
            .append(user.getUserContact().getFirstName())
            .append(' ')
            .append(user.getUserContact().getLastName());
        LOGGER.info(sb.toString());
        sb = new StringBuffer();
        sb.append("          number=")
            .append(user.getNumID())
            .append("   update=")
            .append(user.isUpdateAllowed())
            .append("   enabled=")
            .append(user.isEnabled());
        LOGGER.info(sb.toString());
        sb = new StringBuffer();
        sb.append("          primary group=").append(user.getPrimaryGroupID());
        LOGGER.info(sb.toString());
        List<String> groups = user.getGroupIDs();
        for (String gid : groups) {
            sb = new StringBuffer();
            sb.append("          member in group=").append(gid);
            LOGGER.info(sb.toString());
        }
        LOGGER.info("");
    }

    /**
     * Check the file name
     * 
     * @param filename
     *            the filename of the user data input
     * @return true if the file name is okay
     */
    private static boolean checkFilename(String filename) {
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
     * @throws SAXParseException 
     */
    public static void createUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser[] users = getMCRUsersFromFile(filename);
        for (MCRUser user : users) {
            try {
                MCRUserMgr.instance().createUser(user);
            } catch (MCRException ex) {
                LOGGER.warn(ex.getMessage());
            }
        }
    }

    /**
     * This method invokes MCRUserMgr.createGroup() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     */
    public static void createGroupFromFile(String filename) throws MCRException {
        MCRUserMgr mcrUserMgr = MCRUserMgr.instance();
        if (!checkFilename(filename)) {
            return;
        }

        LOGGER.info("Reading file " + filename + " ...");

        try {
            Document doc = MCRXMLParserFactory.getValidatingParser().parseXML(new MCRFileContent(filename));
            Element rootelm = doc.getRootElement();

            if (!rootelm.getName().equals("mycoregroup")) {
                throw new MCRException("The data are not for group.");
            }

            @SuppressWarnings("unchecked")
            List<Element> listelm = rootelm.getChildren();

            for (int i = 0; i < listelm.size(); i++) {
                MCRGroup g = new MCRGroup((Element) listelm.get(i));
                if (!mcrUserMgr.existGroup(g.getID())) {
                    MCRUserMgr.instance().createGroup(g);
                } else {
                    LOGGER.info("Group " + g.getID() + " was not created, because it already exists.");
                }
            }
        } catch (Exception e) {
            throw new MCRException("Error while loading group data.", e);
        }
    }

    /**
     * This method imports groups and user data from XML files. It simply call
     * createGroupFromFile() of the MCRUserManager.
     * 
     * @param groupFileName
     *            the filename of the group data input
     * @param userFileName
     *            the filename of the user data input
     */
    public static void importUserSystemFromFiles(String groupFileName, String userFileName) throws MCRException {
        // check file names
        if (!checkFilename(groupFileName)) {
            return;
        }
        if (!checkFilename(userFileName)) {
            return;
        }
        MCRUserMgr umgr = MCRUserMgr.instance();
        try {
            // read groups
            LOGGER.info("Reading group file " + groupFileName + " ...");
            Document groupdoc = MCRXMLParserFactory.getValidatingParser().parseXML(new MCRFileContent(groupFileName));
            Element grouprootelm = groupdoc.getRootElement();
            if (!grouprootelm.getName().equals("mycoregroup")) {
                throw new MCRException("The data are not for group.");
            }
            // read users
            LOGGER.info("Reading user file " + userFileName + " ...");
            Document userdoc = MCRXMLParserFactory.getValidatingParser().parseXML(new MCRFileContent(userFileName));
            Element userrootelm = userdoc.getRootElement();
            if (!userrootelm.getName().equals("mycoreuser")) {
                throw new MCRException("The data are not for user.");
            }
            umgr.importUserSystemFromFiles(grouprootelm, userrootelm);
            // check the consistency
            umgr.checkConsistency();
        } catch (Exception e) {
            LOGGER.error("Error while loading group and user data.");
            LOGGER.error(e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }

    }

    /**
     * This method invokes MCRUserMgr.updateUser() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     * @throws SAXParseException if file could not be parsed
     */
    public static void updateUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser[] users = getMCRUsersFromFile(filename);
        for (MCRUser user : users) {
            MCRUserMgr.instance().updateUser(user);
        }
    }

    private static MCRUser[] getMCRUsersFromFile(String filename) throws SAXParseException, IOException {
        if (!checkFilename(filename)) {
            return new MCRUser[0];
        }
        LOGGER.info("Reading file " + filename + " ...");
        Document doc = MCRXMLParserFactory.getValidatingParser().parseXML(new MCRFileContent(filename));
        Element rootelm = doc.getRootElement();

        if (!rootelm.getName().equals("mycoreuser")) {
            throw new MCRException("These data are not defining a user.");
        }
        @SuppressWarnings("unchecked")
        List<Element> listelm = rootelm.getChildren();
        MCRUser[] users = new MCRUser[listelm.size()];
        for (int i = 0; i < users.length; i++) {
            users[i] = new MCRUser(listelm.get(i));
        }
        return users;
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
    public static void addMemberUserToGroup(String mbrUserID, String groupID) throws MCRException {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            group.addMemberUserID(mbrUserID);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while adding group " + mbrUserID + " to group " + groupID + ".", e);
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
    public static void removeMemberUserFromGroup(String mbrUserID, String groupID) throws MCRException {
        try {
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            group.removeMemberUserID(mbrUserID);
            MCRUserMgr.instance().updateGroup(group);
        } catch (Exception e) {
            throw new MCRException("Error while removing group " + mbrUserID + " from group " + groupID + ".", e);
        }
    }

    /**
     * This method just saves a JDOM document to a file
     * 
     * automatically closes {@link OutputStream}.
     * 
     * @param jdomDoc
     *            the JDOM XML document to be printed
     * @param outFile
     *            a FileOutputStream object for the output
     * @throws IOException if output file can not be closed
     */
    private static void saveToXMLFile(Document jdomDoc, FileOutputStream outFile) throws MCRException, IOException {
        String mcr_encoding = CONFIG.getString("MCR.Metadata.DefaultEncoding", DEFAULT_ENCODING);

        // Create the output
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding(mcr_encoding));

        try {
            outputter.output(jdomDoc, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while save XML to file.");
        } finally {
            outFile.close();
        }
    }
}
