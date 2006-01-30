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

package org.mycore.services.fieldquery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRExternalCommandInterface;

/**
 * Provides commands to test the query classes using the command line interface
 * 
 * @author Frank Lützenkirchen
 * @author Arne Seifert
 */
public class MCRQueryCommands implements MCRExternalCommandInterface {

    public ArrayList getPossibleCommands() {
        ArrayList commands = new ArrayList();
        commands.add(new MCRCommand("run query from file {0} using searcher {1}", "org.mycore.services.fieldquery.MCRQueryCommands.runQueryFromFile String String", "Runs a query that is specified as XML in the given file, using the MCRSearcher implementation with the given ID"));
        return commands;
    }

    /**
     * Runs a query that is specified as XML in the given file, using the
     * MCRSearcher implementation with the given ID. The results are written to
     * stdout.
     */
    public static void runQueryFromFile(String filename, String searcherID) throws JDOMException, IOException {
        File file = new File(filename);
        if (!(file.exists() && file.canRead())) {
            String msg = "File containing XML query does not exist: " + filename;
            throw new org.mycore.common.MCRUsageException(msg);
        }

        Document query = new SAXBuilder().build(new File(filename));
        MCRSearcher searcher = MCRSearcherFactory.getSearcher(searcherID);
        MCRResults results = searcher.search(query);
        System.out.println(results);
    }
}
