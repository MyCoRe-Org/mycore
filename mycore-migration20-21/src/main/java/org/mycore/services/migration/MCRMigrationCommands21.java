package org.mycore.services.migration;

import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRXMLTABLE;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRMigrationCommands21 extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRMigrationCommands21.class);

    public MCRMigrationCommands21() {
        MCRCommand com = null;

        com = new MCRCommand("migrate xmltable", "org.mycore.services.migration.MCRMigrationCommands21.migrateXMLTable",
                "The command migrates all entries from MCRXMLTable to IFS2.");
        command.add(com);
    }

    public static void migrateXMLTable() {
        StatelessSession session = MCRHIBConnection.instance().getSessionFactory().openStatelessSession();
        Transaction tx = session.beginTransaction();
        MCRXMLTableManager manager = MCRXMLTableManager.instance();
        try {
            ScrollableResults xmlentries = session.createCriteria(MCRXMLTABLE.class).scroll(ScrollMode.FORWARD_ONLY);
            while (xmlentries.next()) {
                MCRXMLTABLE xmlentry = (MCRXMLTABLE) xmlentries.get(0);
                MCRObjectID mcrId = new MCRObjectID(xmlentry.getId());
                Date lastModified = xmlentry.getLastModified();
                byte[] xmlByteArray = xmlentry.getXmlByteArray();
                if (manager.exists(mcrId)){
                    LOGGER.warn(xmlentry.getId()+ " allready exists in IFS2 - skipping.");
                    continue;
                }
                LOGGER.info("Migrating " + xmlentry.getId() + " to IFS2.");
                manager.create(mcrId, xmlByteArray, lastModified);
            }
            tx.commit();
        } catch (Exception e) {
            LOGGER.error("Could not migrate...",e);
            if (tx.isActive())
                tx.rollback();
        } finally {
            session.close();
        }

    }
}
