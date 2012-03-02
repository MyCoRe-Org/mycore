/*
 * 
 * $Revision: 23424 $ $Date: 2012-02-02 22:53:29 +0100 (Do, 02 Feb 2012) $
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

package org.mycore.user2;

import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.user2.utils.MCRUserTransformer;
import org.xml.sax.SAXParseException;

/**
 * This class provides a set of commands for the org.mycore.user2 management
 * which can be used by the command line interface.
 *
 * @author Thomas Scheffler (yagee)
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

        command.add(new MCRCommand("change to user {0} with {1}", "org.mycore.user2.MCRUserCommands.changeToUser String String",
                "Change the user {0} with the given password in {1}."));
        command.add(new MCRCommand("login {0}", "org.mycore.user2.MCRUserCommands.login String", "Start the login dialog for the user {0}."));

        com = new MCRCommand("init superuser", "org.mycore.user2.MCRUserCommands.initSuperuser",
                "Initialized the user system. This command runs only if the user database does not exist.");
        command.add(com);

        com = new MCRCommand("encrypt passwords in user xml file {0} to file {1}",
                "org.mycore.user2.MCRUserCommands.encryptPasswordsInXMLFile String String",
                "This is a migration tool to change old plain text password entries to encrpted entries.");
        command.add(com);

        com = new MCRCommand("set password for user {0} to {1}", "org.mycore.user2.MCRUserCommands.setPassword String String",
                "This command sets a new password for the user. You must be this user or you must have administrator access.");
        command.add(com);

        com = new MCRCommand("set user management to ro mode", "org.mycore.user2.MCRUserCommands.setLock",
                "The command changes the management mode of the user system to read-only.");
        command.add(com);

        com = new MCRCommand("set user management to rw mode", "org.mycore.user2.MCRUserCommands.setunLock",
                "The command changes the management mode of the user system to read-write.");
        command.add(com);

        com = new MCRCommand("enable user {0}", "org.mycore.user2.MCRUserCommands.enableUser String",
                "The command enables the user for the access.");
        command.add(com);

        com = new MCRCommand("disable user {0}", "org.mycore.user2.MCRUserCommands.disableUser String",
                "The command disables the user from the access.");
        command.add(com);

        com = new MCRCommand("delete group {0}", "org.mycore.user2.MCRUserCommands.deleteGroup String",
                "The command delete the group {0} from the user system, but only if it has no user members.");
        command.add(com);

        com = new MCRCommand("add groups from user file {0}", "org.mycore.user2.MCRUserCommands.addGroups String",
                "The command adds groups found in user file {0} that do not exist");
        command.add(com);

        com = new MCRCommand("delete user {0}", "org.mycore.user2.MCRUserCommands.deleteUser String", "The command delete the user {0}.");
        command.add(com);

        com = new MCRCommand("add user {0} as member to group {1}", "org.mycore.user2.MCRUserCommands.addMemberUserToGroup String String",
                "The command add a user {0} as secondary member in the group {1}.");
        command.add(com);

        com = new MCRCommand("remove user {0} as member from group {1}",
                "org.mycore.user2.MCRUserCommands.removeMemberUserFromGroup String String",
                "The command remove the user {0} as secondary member from the group {1}.");
        command.add(com);

        com = new MCRCommand("list all groups", "org.mycore.user2.MCRUserCommands.listAllGroups", "The command list all groups.");
        command.add(com);

        com = new MCRCommand("list group {0}", "org.mycore.user2.MCRUserCommands.listGroup String", "The command list the group {0}.");
        command.add(com);

        com = new MCRCommand("list all users", "org.mycore.user2.MCRUserCommands.listAllUsers", "The command list all users.");
        command.add(com);

        com = new MCRCommand("list user {0}", "org.mycore.user2.MCRUserCommands.listUser String", "The command list the user {0}.");
        command.add(com);

        com = new MCRCommand("export user {0} to file {1}", "org.mycore.user2.MCRUserCommands.exportUserToFile String String",
                "The command exports the data of user {0} to the file {1}.");

        com = new MCRCommand("import user from file {0}", "org.mycore.user2.MCRUserCommands.importUserFromFile String",
                "The command imports a user from file {0}.");
        command.add(com);
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
        if (MCRUserManager.login(user, password) != null) {
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
    public static List<String> initSuperuser() {
        final String suser = CONFIG.getString("MCR.Users.Superuser.UserName", "administrator");
        final String spasswd = CONFIG.getString("MCR.Users.Superuser.UserPasswd", "alleswirdgut");
        final String sgroup = CONFIG.getString("MCR.Users.Superuser.GroupName", "admingroup");

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

        if (MCRUserManager.exists(suser)) {
            LOGGER.error("The superuser already exists!");
            return null;
        }

        // the superuser group
        try {
            Set<MCRLabel> labels = new HashSet<MCRLabel>();
            labels.add(new MCRLabel("en", "The superuser group", null));

            MCRGroup mcrGroup = new MCRGroup(sgroup, labels);
            MCRGroupManager.addGroup(mcrGroup);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser group.", e);
        }

        LOGGER.info("The group " + sgroup + " is installed.");

        // the superuser
        try {
            MCRUser mcrUser = new MCRUser(suser);
            mcrUser.setRealName("Superuser");
            mcrUser.addToGroup(sgroup);
            MCRUserManager.updatePasswordHashToSHA1(mcrUser, spasswd);
            MCRUserManager.createUser(mcrUser);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser.", e);
        }

        LOGGER.info(MessageFormat.format("The user {0} with password {1} is installed.", suser, spasswd));
        return Arrays.asList("change to user " + suser + " with " + spasswd);
    }

    /**
     * This method invokes MCRUserMgr.deleteGroup() and permanently removes a
     * group from the system.
     * 
     * @param groupID
     *            the ID of the group which will be deleted
     */
    public static void deleteGroup(String groupID) {
        MCRGroupManager.deleteGroup(groupID);
    }

    /**
     * Loads XML from a user and looks for groups currently not present in the system and creates them.
     * 
     * @param fileName
     *            a valid user XML file
     * @throws IOException 
     * @throws SAXParseException 
     */
    public static void addGroups(String fileName) throws SAXParseException, IOException {
        LOGGER.info("Reading file " + fileName + " ...");
        Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(fileName));
        Element user = doc.getRootElement();
        Element groups = user.getChild("groups");
        if (groups == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Element> groupList = groups.getChildren("group");
        for (Element group : groupList) {
            String name = group.getAttributeValue("name");
            MCRGroup mcrGroup = MCRGroupManager.getGroup(name);
            if (mcrGroup == null) {
                @SuppressWarnings("unchecked")
                List<Element> labelList = group.getChildren("label");
                mcrGroup = new MCRGroup(name, MCRXMLTransformer.getLabels(labelList));
                MCRGroupManager.addGroup(mcrGroup);
            }
        }
    }

    /**
     * This method invokes MCRUserMgr.deleteUser() and permanently removes a
     * user from the system.
     * 
     * @param userID
     *            the ID of the user which will be deleted
     */
    public static void deleteUser(String userID) throws Exception {
        MCRUserManager.deleteUser(userID);
    }

    /**
     * This method invokes MCRUserMgr.enableUser() that enables a user
     * 
     * @param userID
     *            the ID of the user which will be enabled
     */
    public static void enableUser(String userID) throws Exception {
        MCRUser mcrUser = MCRUserManager.getUser(userID);
        mcrUser.enableLogin();
        MCRUserManager.updateUser(mcrUser);
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
    public static final void encryptPasswordsInXMLFile(String oldFile, String newFile) throws MCRException {
        if (!checkFilename(oldFile)) {
            return;
        }
        LOGGER.info("Reading file " + oldFile + " ...");

        try {
            Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(oldFile));
            Element rootelm = doc.getRootElement();
            MCRUser mcrUser = MCRUserTransformer.buildMCRUser(rootelm);

            if (mcrUser == null) {
                throw new MCRException("These data do not correspond to a user.");
            }

            MCRUserManager.updatePasswordHashToSHA1(mcrUser, mcrUser.getPassword());

            FileOutputStream outFile = new FileOutputStream(newFile);
            saveToXMLFile(mcrUser, outFile);
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
        MCRUser mcrUser = MCRUserManager.getUser(userID);
        mcrUser.disableLogin();
        MCRUserManager.updateUser(mcrUser);
    }

    /**
     * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList
     * of all users stored in the persistent datastore.
     */
    public static void listAllUsers() throws Exception {
        List<MCRUser> users = MCRUserManager.listUsers(null, null, null);

        for (MCRUser uid : users) {
            listUser(uid);
        }
    }

    /**
     * This method invokes MCRUserMgr.getAllGroupIDs() and retrieves a ArrayList
     * of all groups stored in the persistent datastore.
     */
    public static void listAllGroups() throws Exception {
        List<MCRGroup> groups = MCRGroupManager.listSystemGroups();

        for (MCRGroup group : groups) {
            listGroup(group);
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
            MCRUser user = MCRUserManager.getUser(userID);
            FileOutputStream outFile = new FileOutputStream(filename);
            LOGGER.info("Writing to file " + filename + " ...");
            saveToXMLFile(user, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while command saveUserToFile()", e);
        }
    }

    /**
     * This command takes a file name as a parameter, creates the
     * MCRUser instances stores it in the database if it does not exists.
     * 
     * @param filename
     *            Name of the file to import user from
     * @throws IOException 
     * @throws SAXParseException 
     */
    public static void importUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser user = getMCRUserFromFile(filename);
        if (MCRUserManager.exists(user.getUserName(), user.getRealmID())) {
            throw new MCRException("User already exists: " + user.getUserID());
        }
        MCRUserManager.createUser(user);
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the
     * retrieved user object to change the password.
     * 
     * @param userID
     *            the ID of the user for which the password will be set
     */
    public static void setPassword(String userID, String password) throws MCRException {
        MCRUser user = MCRUserManager.getUser(userID);
        MCRUserManager.updatePasswordHashToSHA1(user, password);
        MCRUserManager.updateUser(user);
    }

    /**
     * This method invokes MCRUserMgr.retrieveGroup() and then works with the
     * retrieved group object to get an XML-Representation.
     * 
     * @param groupID
     *            the ID of the group for which the XML-representation is needed
     */
    public static final void listGroup(String groupID) throws MCRException {
        MCRGroup group = MCRGroupManager.getGroup(groupID);
        listGroup(group);
    }

    public static final void listGroup(MCRGroup group) {
        StringBuilder sb = new StringBuilder();
        sb.append("       group=").append(group.getName());
        for (MCRLabel label : group.getLabels()) {
            sb.append("\n         ").append(label.toString());
        }
        Collection<String> userIds = MCRGroupManager.listUserIDs(group);
        for (String userId : userIds) {
            sb.append("\n          user in this group=").append(userId);
        }
        LOGGER.info(sb.toString());
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the
     * retrieved user object to get an XML-Representation.
     * 
     * @param userID
     *            the ID of the user for which the XML-representation is needed
     */
    public static final void listUser(String userID) throws MCRException {
        MCRUser user = MCRUserManager.getUser(userID);
        listUser(user);
    }

    public static void listUser(MCRUser user) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("       user=").append(user.getUserName()).append("   real name=").append(user.getRealName()).append('\n')
                .append("   loginAllowed=").append(user.loginAllowed()).append('\n');
        List<String> groups = new ArrayList<String>(user.getSystemGroupIDs());
        groups.addAll(user.getExternalGroupIDs());
        for (String gid : groups) {
            sb.append("          member in group=").append(gid).append('\n');
        }
        LOGGER.info(sb.toString());
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
     * @throws SAXParseException 
     */
    public static final void createUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser user = getMCRUserFromFile(filename);
        MCRUserManager.createUser(user);
    }

    /**
     * This method invokes MCRUserMgr.updateUser() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     * @throws SAXParseException if file could not be parsed
     */
    public static final void updateUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser user = getMCRUserFromFile(filename);
        MCRUserManager.updateUser(user);
    }

    private static final MCRUser getMCRUserFromFile(String filename) throws SAXParseException, IOException {
        if (!checkFilename(filename)) {
            return null;
        }
        LOGGER.info("Reading file " + filename + " ...");
        Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(filename));
        return MCRUserTransformer.buildMCRUser(doc.getRootElement());
    }

    /**
     * This method adds a user as a member to a group
     * 
     * @param userID
     *            the ID of the user which will be a member of the group
     *            represented by groupID
     * @param groupID
     *            the ID of the group to which the user with ID mbrUserID will
     *            be added
     * @throws MCRException
     */
    public static final void addMemberUserToGroup(String userID, String groupID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.addToGroup(groupID);
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while adding group " + userID + " to group " + groupID + ".", e);
        }
    }

    /**
     * This method removes a member user from a group
     * 
     * @param userID
     *            the ID of the user which will be removed from the group
     *            represented by groupID
     * @param groupID
     *            the ID of the group from which the user with ID mbrUserID will
     *            be removed
     * @throws MCRException
     */
    public static final void removeMemberUserFromGroup(String userID, String groupID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.removeFromGroup(groupID);
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while removing group " + userID + " from group " + groupID + ".", e);
        }
    }

    /**
     * This method just saves a JDOM document to a file
     * 
     * automatically closes {@link OutputStream}.
     * 
     * @param mcrUser
     *            the JDOM XML document to be printed
     * @param outFile
     *            a FileOutputStream object for the output
     * @throws IOException if output file can not be closed
     */
    private static final void saveToXMLFile(MCRUser mcrUser, FileOutputStream outFile) throws MCRException, IOException {
        String mcr_encoding = CONFIG.getString("MCR.Metadata.DefaultEncoding", DEFAULT_ENCODING);

        // Create the output
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding(mcr_encoding));

        try {
            outputter.output(MCRUserTransformer.buildExportableXML(mcrUser), outFile);
        } catch (Exception e) {
            throw new MCRException("Error while save XML to file.");
        } finally {
            outFile.close();
        }
    }
}
