/*
 * $Revision$ 
 * $Date$
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

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Provides commands to test the query classes using the command line interface
 * 
 * @author Frank Lützenkirchen
 * @author Arne Seifert
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date: 2013-01-31 14:14:26 +0100 (Do, 31 Jan
 *          2013) $
 */
@MCRCommandGroup(name = "MCR Query Commands")
public class MCRQueryCommands extends MCRAbstractCommands {

    /**
     * Runs a query that is specified as XML in the given file. The results are
     * written to stdout. To transform the result data it use the stylesheet
     * results-commandlinequery.xsl.
     * 
     * @param filename
     *            the name of the XML file with the query condition
     */
    @MCRCommand(syntax = "run query from file {0}", help = "Runs a query that is specified as XML in the given file",
            order = 10)
    public static void runQueryFromFile(String filename) throws JDOMException, IOException {
        File file = new File(filename);
        if (!file.exists()) {
            String msg = "File containing XML query does not exist: " + filename;
            throw new org.mycore.common.MCRUsageException(msg);
        }
        if (!file.canRead()) {
            String msg = "File containing XML query not readable: " + filename;
            throw new org.mycore.common.MCRUsageException(msg);
        }

        Document xml = new SAXBuilder().build(new File(filename));
        MCRQuery query = MCRQuery.parseXML(xml);
        MCRResults results = MCRQueryManager.search(query);
        buildOutput(results);
    }

    /**
     * Runs a query that is specified as String against the local host. The
     * results are written to stdout.
     * 
     * @param querystring
     *            the string with the query condition
     */
    @MCRCommand(syntax = "run local query {0}", help = "Runs a query specified as String on the local host", order = 20)
    public static void runLocalQueryFromString(String querystring) {
        MCRCondition<Object> cond = new MCRQueryParser().parse(querystring);
        MCRQuery query = new MCRQuery(cond);
        MCRResults results = MCRQueryManager.search(query);
        buildOutput(results);
    }

    /**
     * Runs a query that is specified as String against all hosts. The results
     * are written to stdout.
     * 
     * @param querystring
     *            the string with the query condition
     */
    @MCRCommand(syntax = "run distributed query {0}",
            help = "Runs a query specified as String on the local host and all remote hosts", order = 30)
    public static void runAllQueryFromString(String querystring) {
        MCRCondition<Object> cond = new MCRQueryParser().parse(querystring);
        MCRQuery query = new MCRQuery(cond);
        query.setHosts(MCRQueryClient.ALL_HOSTS);
        MCRResults results = MCRQueryManager.search(query);
        buildOutput(results);
    }

    /** Transform the results to an output using stylesheets */
    private static void buildOutput(MCRResults results) {
        String xslFile = "xsl/results-commandlinequery.xsl";
        MCRXSLTransformer transformer = new MCRXSLTransformer(xslFile);
        MCRContent input = new MCRJDOMContent(new Document(results.buildXML()));

        try {
            MCRContent output = transformer.transform(input);
            output.sendTo(System.out);
        } catch (Exception ex) {
            Logger LOGGER = Logger.getLogger(MCRQueryCommands.class);
            LOGGER.error("Error while tranforming query result XML using XSLT");
            LOGGER.debug(ex.getMessage());
            LOGGER.info("Stop.");
            LOGGER.info("");
            return;
        }
    }
}
