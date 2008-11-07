package org.mycore.services.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRMigrationCommands extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRMigrationCommands.class);

    public MCRMigrationCommands() {
        MCRCommand com = null;

        com = new MCRCommand("migrate user", "org.mycore.services.migration.MCRMigrationCommands.migrateUser",
                "The command migrates the user management to MyCoRe 2.0.");
        command.add(com);
        com = new MCRCommand("internal usermigration step {0}", "org.mycore.services.migration.MCRMigrationCommands.migrateUser int",
                "Internal commands for user migration");
        command.add(com);
        com = new MCRCommand("migrate mcraccess", "org.mycore.services.migration.MCRMigrationCommands.migrateAccess",
                "The command migrates the access system to MyCoRe 2.0.");
        command.add(com);
        com = new MCRCommand("internal accessmigration step {0}", "org.mycore.services.migration.MCRMigrationCommands.migrateAccess int",
                "Internal commands for access system migration");
        command.add(com);
        com = new MCRCommand("migrate classifications", "org.mycore.services.migration.MCRMigrationCommands.migrateClassifications",
                "Migrates the version 1 classification system to the current version");
        command.add(com);
        com = new MCRCommand("internal classificationmigration step {0}", "org.mycore.services.migration.MCRMigrationCommands.migrateClassifications int",
                "Internal commands for classification migration");
        command.add(com);
        com = new MCRCommand("migrate history date in type {0}", "org.mycore.services.migration.MCRMigrationCommands.migrateMCRMetaHistoryDate String",
                "Internal commands for the migration of the MCRMetaHistoryDate text lines to multi languages for MyCoRe type {0}");
        command.add(com);
    }

    public static List<String> migrateClassifications() throws JDOMException {
        List<String> cmds = new ArrayList<String>();
        for (int i = 1; i < 4; i++) {
            cmds.add("internal classificationmigration step " + i);
            cmds.add("internal classificationmigration step " + i);
        }
        return cmds;
    }

    public static void migrateClassifications(int step) throws JDOMException {
        switch (step) {
        case 1:
            MCRClassificationMigrationHelper.createCategories();
            break;
        case 2:
            MCRClassificationMigrationHelper.migrateCategoryLinks();
            break;
        case 3:
            MCRClassificationMigrationHelper.deleteOldCategoryLinks();
            break;
        default:
            throw new MCRException("Classification migration step " + step + " is unknown.");
        }
    }

    public static List<String> migrateUser() {
        List<String> cmds = new ArrayList<String>();
        LOGGER.info("User Migration started\n");
        final int userMigrationSteps = 11;
        for (int i = 1; i <= userMigrationSteps; i++) {
            cmds.add("internal usermigration step " + i);
        }
        return cmds;
    }

    public static List<String> migrateUser(int step) throws Exception {
        switch (step) {
        case 1:
            return Collections.nCopies(1, "export all groups to file " + MCRUserMigrationHelper.getGroupFile().getAbsolutePath());
        case 2:
            return Collections.nCopies(1, "export all users to file " + MCRUserMigrationHelper.getUserFile().getAbsolutePath());
        case 3:
            MCRUserMigrationHelper.dropTables();
            return Collections.emptyList();
        case 4:
            return Collections.nCopies(1, "init hibernate");
        case 5:
            return Collections.nCopies(1, "init superuser");
        case 6:
            MCRUserMigrationHelper.cleanupGroupFile();
            return Collections.emptyList();
        case 7:
            MCRUserMigrationHelper.cleanupUserFile();
            return Collections.emptyList();
        case 8:
            ArrayList<String> groupImportCommands = new ArrayList<String>(2);
            //check if super user has the right to modify group (delete permission - if not)
            if (!MCRAccessManager.checkPermission("modify-group"))
                groupImportCommands.add("delete permission modify-group for id POOLPRIVILEGE");
            groupImportCommands.add("import group data from file " + MCRUserMigrationHelper.getGroupFile().getAbsolutePath());
            return groupImportCommands;
        case 9:
            ArrayList<String> userImportCommands = new ArrayList<String>(2);
            //check if super user has the right to modify user (delete permission - if not)
            if (!MCRAccessManager.checkPermission("modify-user"))
                userImportCommands.add("delete permission modify-user for id POOLPRIVILEGE");
            userImportCommands.add("import user data from file " + MCRUserMigrationHelper.getUserFile().getAbsolutePath());
            return userImportCommands;
        case 10:
            MCRUserMigrationHelper.updateAdmins();
            return Collections.emptyList();
        case 11:
            MCRUserMigrationHelper.deleteTempFiles();
            return Collections.emptyList();
        default:
            throw new MCRException("User migration step " + step + " is unknown.");
        }
    }

    public static List<String> migrateAccess() {
        List<String> cmds = new ArrayList<String>();
        LOGGER.info("MCRAccess Migration started\n");
        final int userMigrationSteps = 5;
        for (int i = 1; i <= userMigrationSteps; i++) {
            cmds.add("internal accessmigration step " + i);
        }
        return cmds;
    }

    public static List<String> migrateAccess(int step) throws Exception {
        switch (step) {
        case 1:
            return Collections.nCopies(1, "export acl mappings to file " + MCRAccessMigrationHelper.getExportFile().getAbsolutePath());
        case 2:
            MCRAccessMigrationHelper.dropTable();
            return Collections.emptyList();
        case 3:
            return Collections.nCopies(1, "init hibernate");
        case 4:
            return Collections.nCopies(1, "import acl mappings from file " + MCRAccessMigrationHelper.getExportFile().getAbsolutePath());
        case 5:
            MCRAccessMigrationHelper.deleteExportFile();
            return Collections.emptyList();
        default:
            throw new MCRException("MCRACCESS migration step " + step + " is unknown.");
        }
    }

    /**
     * This method migrate the MCRMetaHistoryDate text entries from a single text to multi language texts as sequence of XML text elements. The method read the
     * data to the API and store it to the backend again.
     * 
     * @param type
     *            the MyCoRe data type which includes a MCRMetaHistoryDate element
     * @throws Exception
     */
    public static void migrateMCRMetaHistoryDate(String type) throws Exception {
        MCRXMLTableManager tm = MCRXMLTableManager.instance();
        MCRObject obj = null;
        for (String id : tm.retrieveAllIDs(type)) {
            obj = new MCRObject();
            MCRObjectID oid = new MCRObjectID(id);
            obj.receiveFromDatastore(oid);
            obj.updateInDatastore();
        }
    }
}
