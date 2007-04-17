/*
 * $RCSfile$
 * $Revision$ $Date$
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

// package
package org.mycore.backend.sql;

// Imported java classes
import org.apache.log4j.Logger;
import org.mycore.backend.sql.MCRSQLClassificationStore;
import org.mycore.backend.sql.MCRSQLFileMetadataStore;
import org.mycore.backend.sql.MCRSQLLinkTableStore;
import org.mycore.backend.sql.MCRSQLXMLStore;
import org.mycore.common.MCRPersistenceException;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

/**
 * This class implements a command collection to create SQL tables via JDBC.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRSQLTableCreate extends MCRAbstractCommands {
    // LOGGER
    static Logger LOGGER = Logger.getLogger(MCRSQLTableCreate.class.getName());

    /**
     * The empty constructor.
     */
    public MCRSQLTableCreate() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("create SQL table for type {0}", "org.mycore.frontend.cli.MCRSQLTableCreate.createTableForType String", "The command create all SQL tables for the given MCRObjectID type.");
        command.add(com);

        com = new MCRCommand("create SQL link tables", "org.mycore.frontend.cli.MCRSQLTableCreate.createTableForLinks", "The command create all SQL tables for the link index.");
        command.add(com);

        com = new MCRCommand("create SQL classification tables", "org.mycore.frontend.cli.MCRSQLTableCreate.createTableForClassification", "The command create all SQL tables for the classification index.");
        command.add(com);

        com = new MCRCommand("create SQL IFS tables", "org.mycore.frontend.cli.MCRSQLTableCreate.createTableForIFS", "The command create all SQL tables for the IFS index.");
        command.add(com);
    }

    public static void createTableForType(String type) {
        LOGGER.info("Create the SQL table for type " + type);

        boolean test = CONFIG.getBoolean("MCR.Metadata.Type." + type, false);

        if (!test) {
            throw new MCRPersistenceException("Wront type " + type + " to create SQL tables!");
        }

        MCRSQLXMLStore store = new MCRSQLXMLStore();
        store.init(type);
        LOGGER.info("Ready.");
        LOGGER.info("");
    }

    public static void createTableForLinks() {
        LOGGER.info("Create the SQL table for links");

        new MCRSQLLinkTableStore();
        LOGGER.info("Ready.");
        LOGGER.info("");
    }

    public static void createTableForClassification() {
        LOGGER.info("Create the SQL table for classifications");

        new MCRSQLClassificationStore();
        LOGGER.info("Ready.");
        LOGGER.info("");
    }

    public static void createTableForIFS() {
        LOGGER.info("Create the SQL table for IFS");

        MCRSQLFileMetadataStore store = new MCRSQLFileMetadataStore();
        LOGGER.info("Ready.");
        LOGGER.info("");
    }
}
