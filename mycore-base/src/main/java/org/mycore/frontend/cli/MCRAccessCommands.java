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

package org.mycore.frontend.cli;

import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * This class provides a set of commands for the org.mycore.access management
 * which can be used by the command line interface.
 *
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 */
@MCRCommandGroup(name = "Access Commands")
public class MCRAccessCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = LogManager.getLogger(MCRAccessCommands.class.getName());

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
     * This method sets the new permissions given in a certain file
     *
     * @param filename
     *            the filename of the file that contains the mcrpermissions
     *
     */
    public static void createPermissionsFromFile(String filename) throws Exception {
        createPermissionsFrom(filenameToUri(filename));
    }

    /**
     * This method deletes the old permissions (if given any) and sets the new
     * permissions given in a certain file
     *
     * @param permissionsUri
     *            the URI of to the XML resource that contains the mcrpermissions
     * @see #createPermissionsFrom(String)
     */
    @MCRCommand(syntax = "load permissions data from {0}",
        help = "The command loads the permissions data of the access control system with data from URI {0}.",
        order = 11)
    public static void loadPermissionsFrom(String permissionsUri) throws Exception {
        createPermissionsFrom(permissionsUri);
    }

    /**
     * This method sets the new permissions given in a certain file
     *
     * @param permissionsUri
     *            the URI of to the XML resource that contains the mcrpermissions
     *
     */
    public static void createPermissionsFrom(String permissionsUri) throws Exception {
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        LOGGER.info("Reading {} …", permissionsUri);

        Element permissions = getPermissionsFromUri(permissionsUri);

        if (permissions != null) {
            for (Element permission : permissions.getChildren("mcrpermission")) {
                String permissionName = permission.getAttributeValue("name").trim();
                String ruleDescription = permission.getAttributeValue("ruledescription");
                if (ruleDescription == null) {
                    ruleDescription = "";
                }
                Element rule = permission.getChild("condition").clone();
                String objectid = permission.getAttributeValue("objectid");
                if (objectid == null) {
                    accessImpl.addRule(permissionName, rule, ruleDescription);
                } else {
                    accessImpl.addRule(objectid, permissionName, rule, ruleDescription);
                }
            }
        }
    }

    private static Element getPermissionsFromUri(String uri) {
        Element permissions = MCRURIResolver.instance().resolve(uri);
        if (permissions == null || !Objects.equals(permissions.getName(), "mcrpermissions")) {
            LOGGER.warn("ROOT element is not valid, valid permissions would be for example:");
            LOGGER.warn("<mcrpermissions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"MCRPermissions.xsd\">");
            LOGGER.warn("  <mcrpermission name=\"anyone\" description=\"anyone\" ruledescription=\"anyone\">");
            LOGGER.warn("    <mcrpermissions format=\"xml\"><boolean operator=\"true\" /></condition>");
            LOGGER.warn("  </mcrpermission>");
            LOGGER.warn("</mcrpermissions>");
            return null;
        }
        return permissions;
    }

    /**
     * delete all permissions
     */
    @MCRCommand(syntax = "delete all permissions",
        help = "Remove all permission entries from the Access Control System.",
        order = 40)
    public static void deleteAllPermissions() {
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        for (String permission : accessImpl.getPermissions()) {
            accessImpl.removeRule(permission);
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
    public static void deletePermission(String permission) {
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        accessImpl.removeRule(permission);
    }

    /**
     * This method invokes MCRUserMgr.getAllPrivileges() and retrieves a
     * ArrayList of all privileges stored in the persistent datastore.
     */
    @MCRCommand(syntax = "list all permissions", help = "List all permission entries.", order = 20)
    public static void listAllPermissions() throws MCRException {
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        Collection<String> permissions = accessImpl.getPermissions();
        boolean noPermissionsDefined = true;
        for (String permission : permissions) {
            noPermissionsDefined = false;
            String description = accessImpl.getRuleDescription(permission);
            if (description.equals("")) {
                description = "No description";
            }
            Element rule = accessImpl.getRule(permission);
            LOGGER.info("       {}", permission);
            LOGGER.info("           {}", description);
            if (rule != null) {
                LOGGER.info("           {}", () -> new XMLOutputter().outputString(rule));
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
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();

        Element mcrpermissions = new Element("mcrpermissions");
        mcrpermissions.addNamespaceDeclaration(XSI_NAMESPACE);
        mcrpermissions.addNamespaceDeclaration(XLINK_NAMESPACE);
        mcrpermissions.setAttribute("noNamespaceSchemaLocation", "MCRPermissions.xsd", XSI_NAMESPACE);
        Document doc = new Document(mcrpermissions);
        Collection<String> permissions = accessImpl.getPermissions();
        for (String permission : permissions) {
            Element mcrpermission = new Element("mcrpermission");
            mcrpermission.setAttribute("name", permission);
            String ruleDescription = accessImpl.getRuleDescription(permission);
            if (!ruleDescription.equals("")) {
                mcrpermission.setAttribute("ruledescription", ruleDescription);
            }
            Element rule = accessImpl.getRule(permission);
            mcrpermission.addContent(rule);
            mcrpermissions.addContent(mcrpermission);
        }
        File file = new File(filename);
        if (file.exists()) {
            LOGGER.warn("File {} yet exists, overwrite.", filename);
        }
        LOGGER.info("Writing to file {} ...", filename);
        String mcrEncoding = MCRConfiguration2.getString("MCR.Metadata.DefaultEncoding").orElse(DEFAULT_ENCODING);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat().setEncoding(mcrEncoding));
        try(OutputStream fileOutputStream = Files.newOutputStream(file.toPath())) {
            out.output(doc, fileOutputStream);
        }
    }

    private static String filenameToUri(String filename) {
        return new File(filename).toURI().toString();
    }

    private static Element getRuleFromUri(String uri) {
        Element rule = MCRURIResolver.instance().resolve(uri);
        if (rule == null || !Objects.equals(rule.getName(), "condition")) {
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
     *            String the URI of to the XML resource, that contains the rule
     * @param ruleUri
     *            String the path to the xml file, that contains the rule
     */
    @MCRCommand(syntax = "update permission {0} for id {1} with rule {2}",
        help = "The command updates access rule for a given id of a given permission with a given special rule",
        order = 70)
    public static void permissionUpdateForID(String permission, String id, String ruleUri) {
        permissionUpdateForID(permission, id, ruleUri, "");
    }

    /**
     * updates the permission for a given id and a given permission type with a
     * given rule
     *
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param id
     *            String the URI of to the XML resource, that contains the rule
     * @param strFileRule
     *            String the path to the xml file, that contains the rule
     */
    @MCRCommand(syntax = "update permission {0} for id {1} with rulefile {2}",
        help = "The command updates access rule for a given id of a given permission with a given special rule",
        order = 71)
    public static void permissionFileUpdateForID(String permission, String id, String strFileRule) {
        permissionUpdateForID(permission, id, filenameToUri(strFileRule));
    }

    /**
     * updates the permission for a given id and a given permission type with a
     * given rule
     *
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param id
     *            String the id of the object the rule is assigned to
     * @param ruleUri
     *            String the URI of to the XML resource, that contains the rule
     * @param description
     *            String give a special description, if the semantics of your
     *            rule is multiple used
     */
    @MCRCommand(syntax = "update permission {0} for id {1} with rule {2} described by {3}",
        help = "The command updates access rule for a given id of a given permission with a given special rule",
        order = 60)
    public static void permissionUpdateForID(String permission, String id, String ruleUri, String description) {
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        Element rule = getRuleFromUri(ruleUri);
        if (rule == null) {
            return;
        }
        accessImpl.addRule(id, permission, rule, description);
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
        order = 61)
    public static void permissionFileUpdateForID(String permission, String id, String strFileRule, String description) {
        permissionUpdateForID(permission, id, filenameToUri(strFileRule), description);
    }

    /**
     * updates the permissions for all ids of a given MCRObjectID-Type with a
     * given rule and a given permission
     *
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param ruleUri
     *            String the URI of the rule XML resource, that contains the rule
     */
    @MCRCommand(
        syntax = "update permission {0} for selected with rule {1}",
        help = "The command updates access rule for a given permission and all ids "
            + "of a given MCRObject-Type with a given special rule",
        order = 90)
    public static void permissionUpdateForSelected(String permission, String ruleUri) {
        permissionUpdateForSelected(permission, ruleUri, "");
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
        help = "The command updates access rule for a given permission and all ids "
            + "of a given MCRObject-Type with a given special rule",
        order = 91)
    public static void permissionFileUpdateForSelected(String permission, String strFileRule) {
        permissionUpdateForSelected(permission, filenameToUri(strFileRule));
    }

    /**
     * updates the permissions for all ids of a given MCRObjectID-Type and for a
     * given permission type with a given rule
     *
     * @param permission
     *            String type of permission like read, writedb, etc.
     * @param ruleUri
     *            String the URI of the rule XML resource, that contains the rule
     * @param description
     *            String give a special description, if the semantics of your
     *            rule is multiple used
     */
    @MCRCommand(
        syntax = "update permission {0} for selected with rule {1} described by {2}",
        help = "The command updates access rule for a given permission and all ids "
            + "of a given MCRObject-Type with a given special rule",
        order = 80)
    public static void permissionUpdateForSelected(String permission, String ruleUri, String description) {
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        Element rule = getRuleFromUri(ruleUri);
        if (rule == null) {
            return;
        }
        for (String id : MCRObjectCommands.getSelectedObjectIDs()) {
            accessImpl.addRule(id, permission, rule, description);
        }
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
        help = "The command updates access rule for a given permission and all ids "
            + "of a given MCRObject-Type with a given special rule",
        order = 80)
    public static void permissionFileUpdateForSelected(String permission, String strFileRule, String description) {
        permissionUpdateForSelected(permission, filenameToUri(strFileRule), description);
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
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        accessImpl.removeRule(id, permission);
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
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        accessImpl.removeAllRules(id);
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
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        for (String id : MCRObjectCommands.getSelectedObjectIDs()) {
            accessImpl.removeRule(id, permission);
        }
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
        MCRRuleAccessInterface accessImpl = MCRAccessManager.requireRulesInterface();
        for (String id : MCRObjectCommands.getSelectedObjectIDs()) {
            accessImpl.removeAllRules(id);
        }
    }

    @MCRCommand(
        syntax = "set website read only {0}",
        help = "This command set the whole website into read only mode and provides the given message to users. "
            + "Nobody, except super user can write on system, using web frontend. Parameter {0} specifies a message "
            + "to be displayed",
        order = 150)
    public static void setWebsiteReadOnly(String message) {
        MCRWebsiteWriteProtection.activate(message);
    }

    @MCRCommand(
        syntax = "set website read only",
        help = "This command set the whole website into read only mode. "
            + "An already configurated message will be displayed to users. "
            + "Nobody, except super user can write on system, using web frontend",
        order = 160)
    public static void setWebsiteReadOnly() {
        MCRWebsiteWriteProtection.activate();

    }

    @MCRCommand(
        syntax = "unset website read only",
        help = "This command removes the write protection (read only) from website. "
            + "After unsetting anybody can write as usual, using web frontend",
        order = 170)
    public static void unsetWebsiteReadOnly() {
        MCRWebsiteWriteProtection.deactivate();
    }

}
