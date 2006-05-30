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
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRExternalCommandInterface;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Provides commands to test the query classes using the command line interface
 * 
 * @author Frank Lützenkirchen
 * @author Arne Seifert
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRQueryCommands implements MCRExternalCommandInterface {

    /**
     * The method returns all available commands.
     * 
     * @return an ArrayList of MCRCommands
     */
    public ArrayList getPossibleCommands() {
        ArrayList commands = new ArrayList();
        commands.add(new MCRCommand("run query from file {0}", "org.mycore.services.fieldquery.MCRQueryCommands.runQueryFromFile String", "Runs a query that is specified as XML in the given file"));
        commands.add(new MCRCommand("run local query {0}", "org.mycore.services.fieldquery.MCRQueryCommands.runLocalQueryFromString String", "Runs a query specified as String on the local host"));
        commands.add(new MCRCommand("run distributed query {0}", "org.mycore.services.fieldquery.MCRQueryCommands.runAllQueryFromString String", "Runs a query specified as String on the local host and all remote hosts"));
        return commands;
    }

    /**
     * Runs a query that is specified as XML in the given file. The results are
     * written to stdout. To transform the result data it use the stylesheet
     * results-commandlinequery.xsl.
     * 
     * @param filename
     *            the name of the XML file with the query condition
     */
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
    public static void runLocalQueryFromString(String querystring) {
        MCRCondition cond = (new MCRQueryParser()).parse(querystring);
        MCRQuery query = new MCRQuery(cond);
        MCRResults results = MCRQueryManager.search(query);
        buildOutput(results);
    }

    /**
     * Runs a query that is specified as String against all hosts. The
     * results are written to stdout.
     * 
     * @param querystring
     *            the string with the query condition
     */
    public static void runAllQueryFromString(String querystring) {
        MCRCondition cond = (new MCRQueryParser()).parse(querystring);
        MCRQuery query = new MCRQuery(cond);
        query.setHosts( MCRQueryClient.ALL_HOSTS );
        MCRResults results = MCRQueryManager.search(query);
        buildOutput(results);
    }

    /** Transform the results to an output using stylesheets */
    private final static void buildOutput(MCRResults results) {
        // read stylesheet
        String xslfile = "results-commandlinequery.xsl";
        InputStream in = MCRQueryCommands.class.getResourceAsStream("/" + xslfile);
        if (in == null) {
            throw new MCRConfigurationException("Can't read stylesheet file " + xslfile);
        }

        // transform data
        try {
            StreamSource source = new StreamSource(in);
            TransformerFactory transfakt = TransformerFactory.newInstance();
            Transformer trans = transfakt.newTransformer(source);
            StreamResult sr = new StreamResult(System.out);
            trans.transform(new JDOMSource((new org.jdom.Document(results.buildXML()))), sr);
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
