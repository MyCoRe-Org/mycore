/*
 * 
 * $Revision: 1.11 $ $Date: 2008/11/27 07:58:28 $
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

package org.mycore.frontend.cli;

import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * This class provides a set of commands for the org.mycore.access management
 * which can be used by the command line interface.
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision: 1.11 $ $Date: 2008/11/27 07:58:28 $
 */

@MCRCommandGroup(name = "Access Commands")
public class MCRAccessCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = LogManager.getLogger(MCRAccessCommands.class.getName());

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
     * This method sets the new permissions given in a certain file
     * 
     * @param filename
     *            the filename of the file that contains the mcrpermissions
     * 
     */
    public static void createPermissionsFromFile(String filename) throws Exception {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        if (!checkFilename(filename)) {
            return;
        }
        LOGGER.info("Reading file " + filename + " ...");

        org.jdom2.Document doc = MCRXMLParserFactory.getValidatingParser().parseXML(new MCRFileContent(filename));
        org.jdom2.Element rootelm = doc.getRootElement();

        if (!rootelm.getName().equals("mcrpermissions")) {
            throw new MCRException("The data are not for mcrpermissions.");
        }

        List<Element> listelm = rootelm.getChildren("mcrpermission");

        for (Element mcrpermission : listelm) {
            String permissionName = mcrpermission.getAttributeValue("name").trim();
            String ruleDescription = mcrpermission.getAttributeValue("ruledescription");
            if (ruleDescription == null) {
                ruleDescription = "";
            }
            Element rule = (Element) mcrpermission.getChild("condition").clone();
            String objectid = mcrpermission.getAttributeValue("objectid");
            if (objectid == null) {
                AI.addRule(permissionName, rule, ruleDescription);
            } else {
                AI.addRule(objectid, permissionName, rule, ruleDescription);
            }
        }
    }

    /**
     * This method deletes the old permissions (if given any) and sets the new
     * permissions given in a certain file
     * 
     * @param filename
     *            the filename of the file that contains the mcrpermissions
     * @see #createPermissionsFromFile(String)
     */
    @MCRCommand(syntax = "load permissions data from file {0}",
        help = "The command loads the permissions data of the access control system with data from the file {0}.",
        order = 10)
    public static void loadPermissionsFromFile(String filename) throws Exception {
        createPermissionsFromFile(filename);
    }

    /**
     * delete all permissions
     */
    @MCRCommand(syntax = "delete all permissions",
        help = "Remove all permission entries from the Access Control System.",
        order = 40)
    public static void deleteAllPermissions() throws Exception {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        for (String permission : (List<String>) AI.getPermissions()) {
            AI.removeRule(permission);
        }
    }

    /**
     * delete the permission {0}
     * 
     * @param permission
     *            the name of the permission
     */
    @MCRCommand(syntax = "delete permission {0}",
        help = "Remove a named permission entriy from the Access Control System.",
        order = 30)
    public static void deletePermission(String permission) throws Exception {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        AI.removeRule(permission);
    }

    /**
     * This method invokes MCRUserMgr.getAllPrivileges() and retrieves a
     * ArrayList of all privileges stored in the persistent datastore.
     */
    @MCRCommand(syntax = "list all permissions", help = "List all permission entries.", order = 20)
    public static void listAllPermissions() throws MCRException {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        Collection<String> permissions = AI.getPermissions();
        boolean noPermissionsDefined = true;
        for (String permission : permissions) {
            noPermissionsDefined = false;
            String description = AI.getRuleDescription(permission);
            if (description.equals("")) {
                description = "No description";
            }
            org.jdom2.Element rule = AI.getRule(permission);
            LOGGER.info("       " + permission);
            LOGGER.info("           " + description);
            if (rule != null) {
                org.jdom2.output.XMLOutputter o = new org.jdom2.output.XMLOutputter();
                LOGGER.info("           " + o.outputString(rule));
            }
        }
        if (noPermissionsDefined) {
            LOGGER.warn("No permissions defined");
        }
        LOGGER.info("");
    }

    /**
     * This method just export the permissions to a file
     * 
     * @param filename
     *            the file written to
     */
    @MCRCommand(syntax = "export all permissions to file {0}",
        help = "Export all permissions from the Access Control System to the file {0}.",
        order = 50)
    public static void exportAllPermissionsToFile(String filename) throws Exception {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

        Element mcrpermissions = new Element("mcrpermissions");
        mcrpermissions.addNamespaceDeclaration(XSI_NAMESPACE);
        mcrpermissions.addNamespaceDeclaration(XLINK_NAMESPACE);
        mcrpermissions.setAttribute("noNamespaceSchemaLocation", "MCRPermissions.xsd", XSI_NAMESPACE);
        Document doc = new Document(mcrpermissions);
        Collection<String> permissions = AI.getPermissions();
        for (String permission : permissions) {
            Element mcrpermission = new Element("mcrpermission");
            mcrpermission.setAttribute("name", permission);
            String ruleDescription = AI.getRuleDescription(permission);
            if (!ruleDescription.equals("")) {
                mcrpermission.setAttribute("ruledescription", ruleDescription);
            }
            Element rule = AI.getRule(permission);
            mcrpermission.addContent(rule);
            mcrpermissions.addContent(mcrpermission);
        }
        File file = new File(filename);
        if (file.exists()) {
            LOGGER.warn("File " + filename + " yet exists, overwrite.");
        }
        FileOutputStream fos = new FileOutputStream(file);
        LOGGER.info("Writing to file " + filename + " ...");
        String mcr_encoding = CONFIG.getString("MCR.Metadata.DefaultEncoding", DEFAULT_ENCODING);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat().setEncoding(mcr_encoding));
        out.output(doc, fos);
    }

    private static Element getRuleFromFile(String fileName) throws Exception {
        if (!checkFilename(fileName)) {
            LOGGER.warn("Wrong file format or file doesn't exist");
            return null;
        }
        Document ruleDom = MCRXMLParserFactory.getParser().parseXML(new MCRFileContent(fileName));
        Element rule = ruleDom.getRootElement();
        if (!rule.getName().equals("condition")) {
            LOGGER.warn("ROOT element is not valid, a valid rule would be for example:");
            LOGGER.warn("<condition format=\"xml\"><boolean operator=\"true\" /></condition>");
            return null;
        }
        return rule;
    }

    /**
     * updates the permission for a given id and a given permission type with a
     * given rule
     * 
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param id
     *            String the id of the object the rule is assigned to
     * @param strFileRule
     *            String the path to the xml file, that contains the rule
     */
    @MCRCommand(syntax = "update permission {0} for id {1} with rulefile {2}",
        help = "The command updates access rule for a given id of a given permission with a given special rule",
        order = 70)
    public static void permissionUpdateForID(String permission, String id, String strFileRule) throws Exception {
        permissionUpdateForID(permission, id, strFileRule, "");
    }

    /**
     * updates the permission for a given id and a given permission type with a
     * given rule
     * 
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param id
     *            String the id of the object the rule is assigned to
     * @param strFileRule
     *            String the path to the xml file, that contains the rule
     * @param description
     *            String give a special description, if the semantics of your
     *            rule is multiple used
     */
    @MCRCommand(syntax = "update permission {0} for id {1} with rulefile {2} described by {3}",
        help = "The command updates access rule for a given id of a given permission with a given special rule",
        order = 60)
    public static void permissionUpdateForID(String permission, String id, String strFileRule, String description)
        throws Exception {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        Element rule = getRuleFromFile(strFileRule);
        if (rule == null) {
            return;
        }
        AI.addRule(id, permission, rule, description);
        return;
    }

    /**
     * updates the permissions for all ids of a given MCRObjectID-Type with a
     * given rule and a given permission
     * 
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param strFileRule
     *            String the path to the xml file, that contains the rule
     */
    @MCRCommand(
        syntax = "update permission {0} for selected with rulefile {1}",
        help = "The command updates access rule for a given permission and all ids of a given MCRObject-Type with a given special rule",
        order = 90)
    public static void permissionUpdateForSelected(String permission, String strFileRule) throws Exception {
        permissionUpdateForSelected(permission, strFileRule, "");
    }

    /**
     * updates the permissions for all ids of a given MCRObjectID-Type and for a
     * given permission type with a given rule
     * 
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param strFileRule
     *            String the path to the xml file, that contains the rule
     * @param description
     *            String give a special description, if the semantics of your
     *            rule is multiple used
     */

    @MCRCommand(
        syntax = "update permission {0} for selected with rulefile {1} described by {2}",
        help = "The command updates access rule for a given permission and all ids of a given MCRObject-Type with a given special rule",
        order = 80)
    public static void permissionUpdateForSelected(String permission, String strFileRule, String description)
        throws Exception {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        Element rule = getRuleFromFile(strFileRule);
        if (rule == null) {
            return;
        }
        for (String id : MCRObjectCommands.getSelectedObjectIDs()) {
            AI.addRule(id, permission, rule, description);
        }
    }

    /**
     * delete a given permission for a given id
     * 
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param id
     *            String the id of the object the rule is assigned to
     */
    @MCRCommand(syntax = "delete permission {0} for id {1}",
        help = "The command delete access rule for a given id of a given permission",
        order = 110)
    public static void permissionDeleteForID(String permission, String id) {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        AI.removeRule(id, permission);
        return;
    }

    /**
     * delete all permissions for a given id
     * 
     * @param id
     *            String the id of the object the rule is assigned to
     */
    @MCRCommand(syntax = "delete all permissions for id {1}",
        help = "The command delete all access rules for a given id",
        order = 120)
    public static void permissionDeleteAllForID(String id) {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        AI.removeAllRules(id);
        return;
    }

    /**
     * delete all permissions for all selected objects
     * 
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @see MCRObjectCommands#getSelectedObjectIDs()
     */
    @MCRCommand(syntax = "delete permission {0} for selected",
        help = "The command delete access rule for a query selected set of object ids of a given permission",
        order = 130)
    public static void permissionDeleteForSelected(String permission) {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        for (String id : MCRObjectCommands.getSelectedObjectIDs()) {
            AI.removeRule(id, permission);
        }
        return;
    }

    /**
     * delete all permissions for all selected objects
     * 
     * @see MCRObjectCommands#getSelectedObjectIDs()
     */

    @MCRCommand(syntax = "delete all permissions for selected",
        help = "The command delete all access rules for a query selected set of object ids",
        order = 140)
    public static void permissionDeleteAllForSelected() {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        for (String id : MCRObjectCommands.getSelectedObjectIDs()) {
            AI.removeAllRules(id);
        }
        return;
    }

    @MCRCommand(
        syntax = "set website read only {0}",
        help = "This command set the whole website into read only mode and provides the given message to users. Nobody, except super user can write on system, using web frontend. Parameter {0} specifies a message to be displayed",
        order = 150)
    public static void setWebsiteReadOnly(String message) {
        MCRWebsiteWriteProtection.activate(message);
    }

    @MCRCommand(
        syntax = "set website read only",
        help = "This command set the whole website into read only mode. An already configurated message will be displayed to users. Nobody, except super user can write on system, using web frontend",
        order = 160)
    public static void setWebsiteReadOnly() {
        MCRWebsiteWriteProtection.activate();

    }

    @MCRCommand(
        syntax = "unset website read only",
        help = "This command removes the write protection (read only) from website. After unsetting anybody can write as usual, using web frontend",
        order = 170)
    public static void unsetWebsiteReadOnly() {
        MCRWebsiteWriteProtection.deactivate();
    }

}
