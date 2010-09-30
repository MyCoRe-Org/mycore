package org.mycore.services.migration;

import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRXMLTABLE;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
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
        com = new MCRCommand("convert datamodel1 to datamodel2 from file {0}",
            "org.mycore.frontend.cli.command.MCRDatamodelToDatamodel2Command.convert String", "converts a datamodel 1 file to a new datamodel 2 one");
        command.add(com);
    }

    public static void migrateXMLTable() {
        Session session = MCRHIBConnection.instance().getSession();
        MCRXMLMetadataManager manager = MCRXMLMetadataManager.instance();
        ScrollableResults xmlentries = session.createCriteria(MCRXMLTABLE.class).scroll(ScrollMode.FORWARD_ONLY);
        while (xmlentries.next()) {
            MCRXMLTABLE xmlentry = (MCRXMLTABLE) xmlentries.get(0);
            MCRObjectID mcrId = MCRObjectID.getInstance(xmlentry.getId());
            Date lastModified = xmlentry.getLastModified();
            byte[] xmlByteArray = xmlentry.getXmlByteArray();
            session.evict(xmlentry);
            if (manager.exists(mcrId)) {
                LOGGER.warn(xmlentry.getId() + " allready exists in IFS2 - skipping.");
                continue;
            }
            LOGGER.info("Migrating " + mcrId + " to IFS2.");
            manager.create(mcrId, xmlByteArray, lastModified);
        }
        session.clear();

    }
}
