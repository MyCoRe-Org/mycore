package org.mycore.services.migration;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
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
        //TODO add some migration magic here
    }
}
