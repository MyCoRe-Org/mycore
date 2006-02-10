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

package org.mycore.backend.remote;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This class implements the interface for communication between the local
 * MCRClient and a remote MCRServer via HTTP/HTTPS. <br>
 * This class use org.apache.log4j for logging.
 * 
 * @author Mathias Zarick
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public class MCRServletCommunication implements MCRRemoteAccessInterface {
    // logger
    static Logger logger = Logger.getLogger(MCRServletCommunication.class.getName());

    // internal data
    private String realhost;

    private String protocol;

    private int port;

    private String location;

    private String headercontext;

    private boolean hasifsdata;

    private MCRConfiguration config = MCRConfiguration.instance();

    /**
     * This is the constructor for the MCRSocketCommunication.
     */
    public MCRServletCommunication() {
        // get the instance of MCRConfiguration
        config = MCRConfiguration.instance();

        // set the defaults
        headercontext = "";
        hasifsdata = false;
    }

    /**
     * This method read the connection configuration from the property file.
     * 
     * @param hostAlias
     *            the alias name of the called host
     * @return true if no error was occure, else return false
     */
    private final boolean readConnectionData(String hostAlias) {
        realhost = config.getString("MCR.remoteaccess_" + hostAlias + "_host", "");

        if (realhost.length() == 0) {
            logger.error("Connection data for host " + hostAlias + " not found.");

            return false;
        }

        protocol = config.getString("MCR.remoteaccess_" + hostAlias + "_protocol", "");

        if (protocol.length() == 0) {
            logger.error("Connection data for host " + hostAlias + " not found.");

            return false;
        }

        protocol = protocol.toLowerCase();

        if (!protocol.equals("http")) {
            logger.error("Connection protocol for host " + hostAlias + " is not HTTP.");

            return false;
        }

        port = config.getInt("MCR.remoteaccess_" + hostAlias + "_port", 0);

        if (port == 0) {
            logger.error("Connection port for host " + hostAlias + " is false.");

            return false;
        }

        return true;
    }

    /**
     * This methode represide the query request methode for the communication.
     * For the connection parameter would the MCRConfiguration used.
     * 
     * @param hostAlias
     *            the list of hostnames as string they should requested.
     * @param reqtype
     *            the type value of the MCRObjectId
     * @param query
     *            the query as a stream
     * @exception MCRException
     *                general Exception of MyCoRe
     * @return the result of the query as MCRXMLContainer
     */
    public MCRXMLContainer requestQuery(String hostAlias, String reqtype, String query) throws MCRException {
        MCRXMLContainer result = new MCRXMLContainer();
        hasifsdata = false;

        logger.debug("HostAlias        = " + hostAlias);
        logger.debug("MCRObjectID Type = " + reqtype);
        logger.debug("Query            = " + query);

        if (!readConnectionData(hostAlias)) {
            return result;
        }

        location = config.getString("MCR.remoteaccess_" + hostAlias + "_query_servlet");

        if (location.length() == 0) {
            logger.error("Connection location for host " + hostAlias + " not found.");

            return result;
        }

        URL currentURL;

        try {
            currentURL = new URL(protocol, realhost, port, location);

            PostMethod method = new PostMethod(currentURL.toExternalForm());
            method.addParameter("type", reqtype);
            method.addParameter("host", "local");
            method.addParameter("XSL.Style", "xml");
            method.addParameter("query", query);

            // Check that we didn't run out of retries.
            if (getConnectionStatus(method) == -1) {
                logger.error("Failed to recover from exception.");
            } else {
                BufferedInputStream in = new BufferedInputStream(method.getResponseBodyAsStream());
                result.importElements(in);
                method.releaseConnection();
                in.close();
            }

            currentURL = null;

            for (int i = 0; i < result.size(); i++) {
                result.setHost(i, hostAlias);
            }
        } catch (MCRException mcre) {
            logger.error("Can't use the response from host:" + realhost + ".");
        } catch (UnknownHostException uhe) {
            logger.error("Don't know about host: " + realhost + ".");
        } catch (IOException ioe) {
            logger.error("Couldn't get I/O for the connection to: " + realhost + ".");
        } catch (Exception e) {
            logger.error("Error while getting result of remote query!", e);
        }

        return result;
    }

    /**
     * @param method
     *            the method and returning the status code
     * @return connection status
     * @throws URIException,
     *             IOException
     * @see HttpClient#executeMethod(org.apache.commons.httpclient.HttpMethod)
     */
    private int getConnectionStatus(PostMethod method) throws URIException, IOException {
        int statusCode = -1;
        logger.debug("Connecting to " + method.getURI().toString());
        logger.debug("RequestBody:\n" + method.getRequestBodyAsString());

        // We will retry up to 3 times.
        for (int attempt = 0; (statusCode == -1) && (attempt < 3); attempt++) {
            try {
                // execute the method.
                logger.debug("Connection Attempt: " + (attempt + 1));

                // HttpClient
                HttpClient client = new HttpClient();
                int timeout = 5000;
                logger.info("Setting timout to " + timeout + " ms!");
                client.setTimeout(timeout);
                statusCode = client.executeMethod(method);
            } catch (HttpRecoverableException e) {
                logger.warn("A recoverable exception occurred, retrying.  " + e.getMessage());
            } catch (IOException e) {
                logger.error("Failed to download file.", e);
            }
        }

        return statusCode;
    }

    /**
     * This methode represide the IFS request methode for the communication. For
     * the connection parameter would the MCRConfiguration used.
     * 
     * @param hostAlias
     *            the list of hostnames as string they should requested.
     * @param path
     *            the path to the IFS data
     * @exception MCRException
     *                general Exception of MyCoRe
     * @return the result of the query as MCRXMLContainer
     */
    public final BufferedInputStream requestIFS(String hostAlias, String path) throws MCRException {
        BufferedInputStream in = null;
        hasifsdata = false;
        logger.debug("HostAlias        = " + hostAlias);
        logger.debug("Path             = " + path);

        if (!readConnectionData(hostAlias)) {
            return in;
        }

        location = config.getString("MCR.remoteaccess_" + hostAlias + "_ifs_servlet");

        if (location.length() == 0) {
            logger.error("Connection location for host " + hostAlias + " not found.");

            return in;
        }

        if (path.length() == 0) {
            logger.error("Connection path for host " + hostAlias + " is empty.");

            return in;
        }

        URL currentURL;

        try {
            currentURL = new URL(protocol, realhost, port, location + path);

            PostMethod method = new PostMethod(currentURL.toExternalForm());
            method.addParameter("host", "local");

            if (path.endsWith("/")) {
                method.addParameter("XSL.Style", "xml");
            }

            if (getConnectionStatus(method) == -1) {
                logger.error("Failed to recover from exception.");
            } else {
                headercontext = method.getResponseHeader("Content-Type").getValue();
                hasifsdata = true;

                return new BufferedInputStream(method.getResponseBodyAsStream());
            }
        } catch (MCRException mcre) {
            logger.error("Can't use the response from host:" + realhost + ".");
        } catch (UnknownHostException uhe) {
            logger.error("Don't know about host: " + realhost + ".");
        } catch (IOException ioe) {
            logger.error("Couldn't get I/O for the connection to: " + realhost + ".");
        } catch (Exception e) {
            logger.error(System.err);
        }

        return null;
    }

    /**
     * This method returns the HPPT header content string, if a requestIFS was
     * successful running.
     * 
     * @return HPPT header content string
     */
    public final String getHeaderContent() {
        if (hasifsdata) {
            return headercontext;
        }
        return "";
    }
}
