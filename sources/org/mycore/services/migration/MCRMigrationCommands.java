package org.mycore.services.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRLINKHREF;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRObjectReference;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.common.MCRXMLTableManager;
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
                "Internal commands for classification migration");
        command.add(com);
    }

    public static void migrateClassifications() throws JDOMException {
        List<String> classIds = MCRXMLTableManager.instance().retrieveAllIDs("class");
        MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        XPath classSelector = XPath.newInstance("/mycoreobject/metadata/*[@class='MCRMetaClassification']/*");
        LOGGER.info("Migrating classifications and categories...");
        for (String classID : classIds) {
            MCRObjectID objID = new MCRObjectID(classID);
            Document cl = MCRXMLTableManager.instance().readDocument(objID);
            MCRCategory cat = MCRXMLTransformer.getCategory(cl);
            dao.addCategory(null, cat);
            MCRXMLTableManager.instance().delete(objID);
        }

        LOGGER.info("Migrating object links....");
        // migrate object links
        List<String> objectTypes = MCRXMLTableManager.instance().getAllAllowedMCRObjectIDTypes();
        for (String objectType : objectTypes) {
            List<String> objID = MCRXMLTableManager.instance().retrieveAllIDs(objectType);
            // for
            for (String id : objID) {
                Collection<MCRCategoryID> categories = new ArrayList<MCRCategoryID>();

                Document obj = MCRXMLTableManager.instance().readDocument(new MCRObjectID(id));
                List<Element> classElements = classSelector.selectNodes(obj);
                for (Element el : classElements) {
                    String clid = el.getAttributeValue("classid");
                    String catid = el.getAttributeValue("categid");
                    categories.add(new MCRCategoryID(clid, catid));
                }
                if (categories.size() > 0) {
                    MCRObjectReference objectReference = new MCRObjectReference(id, objectType);
                    MCRCategLinkServiceFactory.getInstance().setLinks(objectReference, categories);

                }
            }

        }
        LOGGER.info("Deleting old object links...");
        final Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(MCRLINKHREF.class).add(Restrictions.eq("key.mcrtype", "classid"));
        for (Object classLink : c.list()) {
            session.delete(classLink);
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
            return Collections.nCopies(1, "import group data from file " + MCRUserMigrationHelper.getGroupFile().getAbsolutePath());
        case 9:
            return Collections.nCopies(1, "import user data from file " + MCRUserMigrationHelper.getUserFile().getAbsolutePath());
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
}
