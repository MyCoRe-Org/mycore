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

package org.mycore.backend.query;

import org.apache.log4j.Logger;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRClassificationCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.services.fieldquery.MCRResults;

public class MCRQueryCommands extends MCRAbstractCommands {
    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRClassificationCommands.class.getName());

    /**
     * constructor with commands.
     */
    public MCRQueryCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("initial load querytable", "org.mycore.backend.query.MCRQueryCommands.init", "The command imports objects of given type into querytable.");
        command.add(com);

        com = new MCRCommand("run query", "org.mycore.backend.query.MCRQueryCommands.runQuery", "lists all MCRID for query");
        command.add(com);
    }

    public static void init() {
        MCRQueryManager.getInstance().createDataBase();
    }

    public static void runQuery() {
        MCRResults res = MCRQueryManager.getInstance().search(MCRQueryManager.getInstance().getQuery());
        System.out.println(res.toString());
    }
    
    
}
