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

package org.mycore.frontend.cli;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.services.query.MCRQueryCollector;

/**
 * This class implements the query command to start a query to a local Library
 * Server or to any remote Library Server in a configuration list or to a
 * dedicated named remote Library Server. The result was presided as a text
 * output.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Mathias Zarick
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
final class MCRQueryCommands extends MCRAbstractCommands {
    // logger
    private static final Logger LOGGER = Logger.getLogger(MCRQueryCommands.class);

    private static MCRQueryCollector COLLECTOR;

    /**
     * The constructor.
     */
    public MCRQueryCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("query host {0} {1} {2}", "org.mycore.frontend.cli.MCRQueryCommands.query String String String", "The command start a search against the host {0} for search type {1} with query {2}");
        command.add(com);

        com = new MCRCommand("query local {0} {1}", "org.mycore.frontend.cli.MCRQueryCommands.queryLocal String String", "The command start a search against the local host for search type {0} with query {1}");
        command.add(com);

        com = new MCRCommand("query remote {0} {1}", "org.mycore.frontend.cli.MCRQueryCommands.queryRemote String String", "The command start a search against all remote hosts for search type {0} with query {1}");
        command.add(com);
    }

    /** Executes a local query */
    public static void queryLocal(String type, String query) {
        query("local", type, query);
    }

    /** Executes a remote query */
    public static void queryRemote(String type, String query) {
        query("remote", type, query);
    }

    /**
     * The query command
     * 
     * @param host
     *            either "local", "remote" or hostname
     * @param type
     *            the document type, "class" or "document" or ...
     * @param query
     *            the query expression
     */
    public static void query(String host, String type, String query) {
        MCRConfiguration config = MCRConfiguration.instance();

        if (COLLECTOR == null) {
            int cThreads = config.getInt("MCR.Collector_Thread_num", 2);
            int aThreads = config.getInt("MCR.Agent_Thread_num", 6);
            COLLECTOR = new MCRQueryCollector(cThreads, aThreads);
        }

        // input parameters
        if (host == null) {
            host = "local";
        }

        if (type == null) {
            return;
        } else {
            type = type.toLowerCase();
        }

        if (query == null) {
            query = "";
        }

        LOGGER.info("Query Host = " + host);
        LOGGER.info("Query Type = " + type);
        LOGGER.info("Query      = " + query);

        MCRXMLContainer resarray = new MCRXMLContainer();

        if (type.equals("class")) // classifications
        {
            String squence = config.getString("MCR.classifications_search_sequence", "remote-local");

            synchronized (resarray) {
                if (squence.equalsIgnoreCase("local-remote")) {
                    COLLECTOR.collectQueryResults("local", type, query, resarray);

                    try {
                        resarray.wait();
                    } catch (InterruptedException ignored) {
                    }

                    if (resarray.size() == 0) {
                        COLLECTOR.collectQueryResults(host, type, query, resarray);

                        try {
                            resarray.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                } else {
                    COLLECTOR.collectQueryResults(host, type, query, resarray);

                    try {
                        resarray.wait();
                    } catch (InterruptedException ignored) {
                    }

                    if (resarray.size() == 0) {
                        COLLECTOR.collectQueryResults("local", type, query, resarray);

                        try {
                            resarray.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }

            if (resarray.size() == 0) {
                LOGGER.error("No classification or category exists");
            }
        } else // other types
        {
            synchronized (resarray) {
                COLLECTOR.collectQueryResults(host, type, query, resarray);

                try {
                    resarray.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        String xslfile = "mcr_results-PlainText-" + type + ".xsl";
        InputStream in = MCRQueryCommands.class.getResourceAsStream("/" + xslfile);

        if (in == null) {
            throw new MCRConfigurationException("Can't read stylesheet file " + xslfile);
        }

        try {
            StreamSource source = new StreamSource(in);
            TransformerFactory transfakt = TransformerFactory.newInstance();
            Transformer trans = transfakt.newTransformer(source);
            StreamResult sr = new StreamResult((OutputStream) System.out);
            trans.transform(new JDOMSource(resarray.exportAllToDocument()), sr);
        } catch (Exception ex) {
            LOGGER.error("Error while tranforming query result XML using XSLT");
            LOGGER.debug(ex.getMessage());
            LOGGER.info("Stop.");
            LOGGER.info("");

            return;
        }

        LOGGER.info("");
        LOGGER.info("Ready.");
        LOGGER.info("");
    }
}
