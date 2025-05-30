/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.user2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.user2.utils.MCRRoleTransformer;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * This class provides a set of commands for the org.mycore.user2 management which can be used by the command line
 * interface.
 *
 * @author Thomas Scheffler (yagee)
 */
@MCRCommandGroup(
    name = "User Commands")
public class MCRUserCommands extends MCRAbstractCommands {
    /** The logger */
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SYSTEM = MCRConfiguration2.getStringOrThrow("MCR.CommandLineInterface.SystemName")
        + ":";

    private static final String FILENAME_EXTENSION_XML = ".xml";

    /**
     * This command changes the user of the session context to a new user.
     *
     * @param user
     *            the new user ID
     * @param password
     *            the password of the new user
     */
    @MCRCommand(
        syntax = "change to user {0} with {1}",
        help = "Changes to the user {0} with the given password {1}.",
        order = 10)
    public static void changeToUser(String user, String password) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        LOGGER.info(() -> SYSTEM + " The old user ID is " + session.getUserInformation().getUserID());
        if (MCRUserManager.login(user, password) != null) {
            LOGGER.info(() -> SYSTEM + " The new user ID is " + session.getUserInformation().getUserID());
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
    @MCRCommand(
        syntax = "login {0}",
        help = "Starts the login dialog for the user {0}.",
        order = 20)
    public static void login(String user) {
        char[] password;
        do {
            password = System.console().readPassword("{0} Enter password for user {1} :> ", SYSTEM, user);
        } while (password.length == 0);

        changeToUser(user, String.valueOf(password));
    }

    /**
     * This method initializes the user and role system an creates a superuser with values set in
     * mycore.properties.private As 'super' default, if no properties were set, mcradmin with password mycore will be
     * used.
     */
    @MCRCommand(
        syntax = "init superuser",
        help = "Initializes the user system. This command runs only if the user database does not exist.",
        order = 30)
    public static List<String> initSuperuser() {
        final String suser = MCRConfiguration2.getStringOrThrow("MCR.Users.Superuser.UserName");
        final String spasswd = MCRConfiguration2.getStringOrThrow("MCR.Users.Superuser.UserPasswd");
        final Optional<String> semail = MCRConfiguration2.getString("MCR.Users.Superuser.UserEmail");
        final String srole = MCRConfiguration2.getStringOrThrow("MCR.Users.Superuser.GroupName");

        if (MCRUserManager.exists(suser)) {
            LOGGER.error("The superuser already exists!");
            return null;
        }

        // the superuser role
        try {
            Set<MCRLabel> labels = new HashSet<>();
            labels.add(new MCRLabel("en", "The superuser role", null));

            MCRRole mcrRole = new MCRRole(srole, labels);
            MCRRoleManager.addRole(mcrRole);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser role.", e);
        }

        LOGGER.info("The role {} is installed.", srole);

        // the superuser
        try {
            MCRUser mcrUser = new MCRUser(suser);
            mcrUser.setRealName("Superuser");
            semail.ifPresent(mcrUser::setEMail);
            mcrUser.assignRole(srole);
            MCRUserManager.setUserPassword(mcrUser, spasswd);
            MCRUserManager.createUser(mcrUser);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser.", e);
        }

        LOGGER.info("The user {} with password {} is installed.", suser, spasswd);
        return Collections.singletonList("change to user " + suser + " with " + spasswd);
    }

    /**
     * This method invokes {@link MCRRoleManager#deleteRole(String)} and permanently removes a role from the system.
     *
     * @param roleID
     *            the ID of the role which will be deleted
     */
    @MCRCommand(
        syntax = "delete role {0}",
        help = "Deletes the role {0} from the user system, but only if it has no user assigned.",
        order = 80)
    public static void deleteRole(String roleID) {
        MCRRoleManager.deleteRole(roleID);
    }

    /**
     * Exports a single role to the specified directory.
     * @throws FileNotFoundException if target directory does not exist
     */
    @MCRCommand(
        syntax = "export role {0} to directory {1}",
        help = "Export the role {0} to the directory {1}. The filename will be {0}.xml")
    public static void exportRole(String roleID, String directory) throws IOException {
        MCRRole mcrRole = MCRRoleManager.getRole(roleID);
        File targetFile = new File(directory, roleID + FILENAME_EXTENSION_XML);
        try (OutputStream fileOutputStream = Files.newOutputStream(targetFile.toPath())) {
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat().setEncoding(MCRConstants.DEFAULT_ENCODING));
            out.output(MCRRoleTransformer.buildExportableXML(mcrRole), fileOutputStream);
        }
    }

    /**
     * Loads XML from a user and looks for roles currently not present in the system and creates them.
     *
     * @param fileName
     *            a valid user XML file
     */
    @MCRCommand(
        syntax = "import role from file {0}",
        help = "Imports a role from file, if that role does not exist",
        order = 90)
    public static void addRole(String fileName) throws IOException, JDOMException {
        LOGGER.info("Reading file {} …", fileName);
        Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(fileName));
        MCRRole role = MCRRoleTransformer.buildMCRRole(doc.getRootElement());
        if (MCRRoleManager.getRole(role.getName()) == null) {
            MCRRoleManager.addRole(role);
        } else {
            LOGGER.info("Role {} does already exist.", role::getName);
        }
    }

    /**
     * This method invokes MCRUserMgr.deleteUser() and permanently removes a user from the system.
     *
     * @param userID
     *            the ID of the user which will be deleted
     */
    @MCRCommand(
        syntax = "delete user {0}",
        help = "Delete the user {0}.",
        order = 110)
    public static void deleteUser(String userID) {
        MCRUserManager.deleteUser(userID);
    }

    /**
     * This method invokes MCRUserMgr.enableUser() that enables a user
     *
     * @param userID
     *            the ID of the user which will be enabled
     */
    @MCRCommand(
        syntax = "enable user {0}",
        help = "Enables the user for the access.",
        order = 60)
    public static void enableUser(String userID) {
        MCRUser mcrUser = MCRUserManager.getUser(userID);
        mcrUser.enableLogin();
        MCRUserManager.updateUser(mcrUser);
    }

    /**
     * A given XML file containing user data with cleartext passwords must be converted prior to loading the user data
     * into the system. This method reads all user objects in the given XML file, encrypts the passwords and writes them
     * back to a file with name original-file-name_encrypted.xml.
     *
     * @param oldFile
     *            the filename of the user data input
     * @param newFile
     *            the filename of the user data output (encrypted passwords)
     */
    @MCRCommand(
        syntax = "encrypt passwords in user xml file {0} to file {1}",
        help = "A migration tool to change old plain text password entries to encrpted entries.",
        order = 40)
    public static void encryptPasswordsInXMLFile(String oldFile, String newFile) throws IOException, JDOMException {
        File inputFile = getCheckedFile(oldFile);
        if (inputFile == null) {
            return;
        }
        LOGGER.info("Reading file {} …", inputFile::getAbsolutePath);

        Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(inputFile));
        Element rootelm = doc.getRootElement();
        MCRUser mcrUser = MCRUserTransformer.buildMCRUser(rootelm);

        if (mcrUser == null) {
            throw new MCRException("These data do not correspond to a user.");
        }

        MCRUserManager.setUserPassword(mcrUser, mcrUser.getHash());

        try (OutputStream fileOutputStream = Files.newOutputStream(Path.of(newFile))) {
            saveToXMLFile(mcrUser, fileOutputStream);
        }
    }

    /**
     * This method invokes MCRUserMgr.disableUser() that disables a user
     *
     * @param userID
     *            the ID of the user which will be enabled
     */
    @MCRCommand(
        syntax = "disable user {0}",
        help = "Disables access of the user {0}",
        order = 70)
    public static void disableUser(String userID) {
        MCRUser mcrUser = MCRUserManager.getUser(userID);
        mcrUser.disableLogin();
        MCRUserManager.updateUser(mcrUser);
    }

    /**
     * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList of all users stored in the persistent
     * datastore.
     */
    @MCRCommand(
        syntax = "list all users",
        help = "Lists all users.",
        order = 160)
    public static void listAllUsers() {
        List<MCRUser> users = MCRUserManager.listUsers(null, null, null, null);
        for (MCRUser uid : users) {
            listUser(uid);
        }
    }

    /**
     * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList of all users stored in the persistent
     * datastore.
     */
    @MCRCommand(
        syntax = "list users like {0}",
        help = "Lists users like {0} (in the same way as the user servlet, except w/o limit).",
        order = 160)
    public static void listAllUsers(String search) throws Exception {
        String pattern = "*" + search + "*";
        List<MCRUser> users = MCRUserManager.listUsers(pattern, null, pattern, pattern, null,
            pattern, 0, Integer.MAX_VALUE);
        for (MCRUser uid : users) {
            listUser(uid);
        }
    }

    /**
     * This method invokes {@link MCRRoleManager#listSystemRoles()} and retrieves a list of all roles stored in the
     * persistent datastore.
     */
    @MCRCommand(
        syntax = "list all roles",
        help = "List all roles.",
        order = 140)
    public static void listAllRoles() {
        List<MCRRole> roles = MCRRoleManager.listSystemRoles();

        for (MCRRole role : roles) {
            listRole(role);
        }
    }

    /**
     * This command takes a userID and file name as a parameter, retrieves the user from MCRUserMgr as JDOM document and
     * export this to the given file.
     *
     * @param userID
     *            ID of the user to be saved
     * @param filename
     *            Name of the file to store the exported user
     */
    @MCRCommand(
        syntax = "export user {0} to file {1}",
        help = "Exports the data of user {0} to the file {1}.",
        order = 180)
    public static void exportUserToFile(String userID, String filename) throws IOException {
        MCRUser user = MCRUserManager.getUser(userID);
        if (user == null) {
            LOGGER.warn("User '{}' does not exist.", userID);
            return;
        }
        if (user.getSystemRoleIDs().isEmpty()) {
            LOGGER.warn("User '{}' has not any system roles.", user::getUserID);
        }
        try (OutputStream fileOutputStream = Files.newOutputStream(Path.of(filename))) {
            LOGGER.info("Writing to file '{}' ...", filename);
            saveToXMLFile(user, fileOutputStream);
        }
    }

    @MCRCommand(
        syntax = "export all users to directory {0}",
        help = "Exports the data of all users to the directory {0}.")
    public static List<String> exportAllUserToDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new MCRException("Directory does not exist: " + dir.getAbsolutePath());
        }
        List<MCRUser> users = MCRUserManager.listUsers(null, null, null, null);
        List<String> commands = new ArrayList<>(users.size());
        for (MCRUser user : users) {
            File userFile = new File(dir, user.getUserID() + FILENAME_EXTENSION_XML);
            commands.add("export user " + user.getUserID() + " to file " + userFile.getAbsolutePath());
        }
        return commands;
    }

    @MCRCommand(
        syntax = "import all users from directory {0}",
        help = "Imports all users from directory {0}.")
    public static List<String> importAllUsersFromDirectory(String directory) throws FileNotFoundException {
        return batchLoadFromDirectory("import user from file", directory);
    }

    @MCRCommand(
        syntax = "update all users from directory {0}",
        help = "Updates all users from directory {0}.")
    public static List<String> updateAllUsersFromDirectory(String directory) throws FileNotFoundException {
        return batchLoadFromDirectory("update user from file", directory);
    }

    public static List<String> batchLoadFromDirectory(String cmd, String directory) throws FileNotFoundException {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            throw new FileNotFoundException(dir.getAbsolutePath() + " is not a directory.");
        }
        File[] listFiles = dir
            .listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(FILENAME_EXTENSION_XML));
        if (listFiles.length == 0) {
            LOGGER.warn("Did not find any user files in {}", dir::getAbsolutePath);
            return null;
        }
        Arrays.sort(listFiles);
        List<String> cmds = new ArrayList<>(listFiles.length);
        for (File file : listFiles) {
            cmds.add(new MessageFormat("{0} {1}", Locale.ROOT).format(new Object[] { cmd, file.getAbsolutePath() }));
        }
        return cmds;
    }

    /**
     * This command takes a file name as a parameter, creates the MCRUser instances stores it in the database if it does
     * not exists.
     *
     * @param filename
     *            Name of the file to import user from
     */
    @MCRCommand(
        syntax = "import user from file {0}",
        help = "Imports a user from file {0}.")
    public static void importUserFromFile(String filename) throws IOException, JDOMException {
        MCRUser user = getMCRUserFromFile(filename);
        if (MCRUserManager.exists(user.getUserName(), user.getRealmID())) {
            throw new MCRException("User already exists: " + user.getUserID());
        }
        MCRUserManager.createUser(user);
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the retrieved user object to change the
     * password.
     *
     * @param userID
     *            the ID of the user for which the password will be set
     */
    @MCRCommand(
        syntax = "set password for user {0} to {1}",
        help = "Sets a new password for the user. You must be this user or you must have administrator access.",
        order = 50)
    public static void setPassword(String userID, String password) throws MCRException {
        MCRUser user = MCRUserManager.getUser(userID);
        MCRUserManager.setUserPassword(user, password);
        MCRUserManager.updateUser(user);
    }

    /**
     * This method invokes {@link MCRRoleManager#getRole(String)} and then works with the retrieved role object to get
     * an XML-Representation.
     *
     * @param roleID
     *            the ID of the role for which the XML-representation is needed
     */
    @MCRCommand(
        syntax = "list role {0}",
        help = "Lists the role {0}.",
        order = 150)
    public static void listRole(String roleID) throws MCRException {
        MCRRole role = MCRRoleManager.getRole(roleID);
        listRole(role);
    }

    public static void listRole(MCRRole role) {
        LOGGER.info(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("       role=").append(role.getName());
            for (MCRLabel label : role.getLabels()) {
                sb.append("\n         ").append(label);
            }
            Collection<String> userIds = MCRRoleManager.listUserIDs(role);
            for (String userId : userIds) {
                sb.append("\n          user assigned to role=").append(userId);
            }
            return sb.toString();
        });
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the retrieved user object to get an
     * XML-Representation.
     *
     * @param userID
     *            the ID of the user for which the XML-representation is needed
     */
    @MCRCommand(
        syntax = "list user {0}",
        help = "Lists the user {0}.",
        order = 170)
    public static void listUser(String userID) throws MCRException {
        MCRUser user = MCRUserManager.getUser(userID);
        listUser(user);
    }

    public static void listUser(MCRUser user) {
        LOGGER.info(() -> {
            StringBuilder sb = new StringBuilder("\n");
            sb.append("       user=").append(user.getUserName()).append("   real name=").append(user.getRealName())
                .append('\n').append("   loginAllowed=").append(user.loginAllowed()).append('\n');
            List<String> roles = new ArrayList<>(user.getSystemRoleIDs());
            roles.addAll(user.getExternalRoleIDs());
            for (String rid : roles) {
                sb.append("          assigned to role=").append(rid).append('\n');
            }
            return sb.toString();
        });
    }

    /**
     * Check the file name
     *
     * @param filename
     *            the filename of the user data input
     * @return true if the file name is okay
     */
    private static File getCheckedFile(String filename) {
        if (!filename.endsWith(FILENAME_EXTENSION_XML)) {
            LOGGER.warn("{} ignored, does not end with *.xml", filename);

            return null;
        }

        File file = new File(filename);
        if (!file.isFile()) {
            LOGGER.warn("{} ignored, is not a file.", filename);

            return null;
        }

        return file;
    }

    /**
     * This method invokes MCRUserMgr.createUser() with data from a file.
     *
     * @param filename
     *            the filename of the user data input
     */
    public static void createUserFromFile(String filename) throws IOException, JDOMException {
        MCRUser user = getMCRUserFromFile(filename);
        MCRUserManager.createUser(user);
    }

    /**
     * This method invokes MCRUserMgr.updateUser() with data from a file.
     *
     * @param filename
     *            the filename of the user data input
     * @throws JDOMException
     *             if file could not be parsed
     */
    @MCRCommand(
        syntax = "update user from file {0}",
        help = "Updates a user from file {0}.",
        order = 200)
    public static void updateUserFromFile(String filename) throws IOException, JDOMException {
        MCRUser user = getMCRUserFromFile(filename);
        MCRUserManager.updateUser(user);
    }

    private static MCRUser getMCRUserFromFile(String filename) throws IOException, JDOMException {
        File inputFile = getCheckedFile(filename);
        if (inputFile == null) {
            return null;
        }
        LOGGER.info("Reading file {} …", inputFile::getAbsolutePath);
        Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(inputFile));
        return MCRUserTransformer.buildMCRUser(doc.getRootElement());
    }

    /**
     * This method adds a user as a member to a role
     *
     * @param userID
     *            the ID of the user which will be a member of the role represented by roleID
     * @param roleID
     *            the ID of the role to which the user with ID mbrUserID will be added
     */
    @MCRCommand(
        syntax = "assign user {0} to role {1}",
        help = "Adds a user {0} as secondary member in the role {1}.",
        order = 120)
    public static void assignUserToRole(String userID, String roleID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.assignRole(roleID);
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while assigning " + userID + " to role " + roleID + ".", e);
        }
    }

    /**
     * This method removes a member user from a role
     *
     * @param userID
     *            the ID of the user which will be removed from the role represented by roleID
     * @param roleID
     *            the ID of the role from which the user with ID mbrUserID will be removed
     */
    @MCRCommand(
        syntax = "unassign user {0} from role {1}",
        help = "Removes the user {0} as secondary member from the role {1}.",
        order = 130)
    public static void unassignUserFromRole(String userID, String roleID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.unassignRole(roleID);
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while unassigning " + userID + " from role " + roleID + ".", e);
        }
    }

    /**
     * This method sets a user attribute for a user.
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the value of the attribute
     * @param userID
     *            the ID of the user
     */
    @MCRCommand(
        syntax = "set attribute {0} to {1} for user {2}",
        help = "Sets the attribute {0} to {1} for user {2}",
        order = 130)
    public static void setUserAttribute(String name, String value, String userID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.setUserAttribute(name, value);
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while setting attribute " + name + " to " + value +
                " for user " + userID, e);
        }
    }

    /**
     * This method removes a user attribute for a user.
     *
     * @param name
     *            the name of the attribute
     * @param userID
     *            the ID of the user
     */
    @MCRCommand(
        syntax = "remove attribute {0} for user {1}",
        help = "Removes the attribute {0} for user {1}",
        order = 130)
    public static void setUserAttribute(String name, String userID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.getAttributes().removeIf(a -> a.getName().equals(name));
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while removing attribute " + name +
                " for user " + userID, e);
        }
    }

    /**
     * This method just saves a JDOM document to a file automatically closes {@link OutputStream}.
     *
     * @param mcrUser
     *            the JDOM XML document to be printed
     * @param outputStream
     *            a FileOutputStream object for the output
     * @throws IOException
     *             if output file can not be closed
     */
    private static void saveToXMLFile(MCRUser mcrUser, OutputStream outputStream) throws IOException {
        // Create the output
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding(MCRConstants.DEFAULT_ENCODING));
        outputter.output(MCRUserTransformer.buildExportableXML(mcrUser), outputStream);
    }
}
