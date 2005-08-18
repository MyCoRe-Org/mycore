package org.mycore.backend.query;

import org.apache.log4j.Logger;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRClassificationCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRQueryCommands extends MCRAbstractCommands {
    
    /** The logger */
    public static Logger LOGGER = Logger
            .getLogger(MCRClassificationCommands.class.getName());

    /**
     * constructor with commands.
     */

    public MCRQueryCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand("initial load querytable",
                "org.mycore.backend.query.MCRQueryManager.initialLoad",
                "The command imports objects of given type into querytable.");
        command.add(com);
        
        com = new MCRCommand("refresh query object {0}",
                "org.mycore.backend.query.MCRQueryManager.refreshObject String",
                "The command imports objects of given type into querytable.");
        command.add(com);
       
        com = new MCRCommand("delete query object {0}",
                "org.mycore.backend.query.MCRQueryManager.deleteObject String",
                "The command imports objects of given type into querytable.");
        command.add(com);

        com = new MCRCommand("run query {0}",
                "org.mycore.backend.query.MCRQueryManager.runQuery int",
                "lists all MCRID for query");
        command.add(com);
        
    }

}
