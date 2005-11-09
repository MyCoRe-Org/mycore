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

package org.mycore.datamodel.ifs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.backend.remote.MCRRemoteAccessInterface;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRLayoutServlet;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet delivers the contents of an MCRFilesystemNode to the client
 * browser. If the node is a ordinary MCRFile, the contents of that file will be
 * sent to the browser. If the node is an MCRFile with a MCRAudioVideoExtender,
 * the message that starts the associated streaming player will be delivered. If
 * the node is a MCRDirectory, the contents of that directory will be forwareded
 * to MCRLayoutServlet as XML data to display a detailed directory listing.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRFileNodeServlet extends MCRServlet {
    /*
     * Die folgenden Dinge will ich hier unbedingt raus haben: - language und
     * damit verbundene Stylesheet Auswahl -> gehöhrt ins MCRLayoutServlet
     * und/oder MCRServlet - remote handling -> muss das so kompliziert sein?
     * Vielleicht nicht, aber ich behebe hier nur Fehler ;o)
     */

    // The Log4J logger
    private static Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class.getName());

    // The list of hosts from the configuration
    private ArrayList remoteAliasList = null;

    /**
     * Initializes the servlet and reads the default language and the remote
     * host list from the configuration.
     */
    public void init() throws MCRConfigurationException, ServletException {
        super.init();

        // read host list from configuration
        String hostconf = CONFIG.getString("MCR.remoteaccess_hostaliases", "local");
        remoteAliasList = new ArrayList();

        if (hostconf.indexOf("local") < 0) {
            remoteAliasList.add("local");
        }

        StringTokenizer st = new StringTokenizer(hostconf, ", ");

        while (st.hasMoreTokens())
            remoteAliasList.add(st.nextToken());
    }

    /**
     * Handles the HTTP request
     */
    public void doGetPost(MCRServletJob job) throws IOException, ServletException {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();

        // get the host alias
        String hostAlias = getProperty(req, "hosts");

        if ((hostAlias == null) || (hostAlias.trim().length() == 0)) {
            hostAlias = "local";
        }

        LOGGER.debug("MCRFileNodeServlet : host = " + hostAlias);

        if (!remoteAliasList.contains(hostAlias)) {
            String msg = "Error: HTTP request host is not in the alias list";
            LOGGER.error(msg);
            generateErrorPage(req, res, HttpServletResponse.SC_BAD_REQUEST, msg, new MCRException(hostAlias + " is not in the host alias list!"), false);

            return;
        }

        String requestPath = req.getPathInfo();
        LOGGER.info("MCRFileNodeServlet: request path = " + requestPath);

        if (requestPath == null) {
            String msg = "Error: HTTP request path is null";
            LOGGER.error(msg);
            generateErrorPage(req, res, HttpServletResponse.SC_BAD_REQUEST, msg, new MCRException("No path was given in the request"), false);

            return;
        }

        StringTokenizer st = new StringTokenizer(requestPath, "/");

        if (!st.hasMoreTokens()) {
            String msg = "Error: HTTP request path is empty";
            LOGGER.error(msg);
            generateErrorPage(req, res, HttpServletResponse.SC_BAD_REQUEST, msg, new MCRException("Empty path was given in the request"), false);

            return;
        }

        String ownerID = st.nextToken();

        if (hostAlias.equals("local")) {
            // local node to be retrieved
            MCRFilesystemNode root;

            try {
                root = MCRFilesystemNode.getRootNode(ownerID);
            } catch (org.mycore.common.MCRPersistenceException e) {
                // Could not get value from JDBC result set
                LOGGER.error("MCRFileNodeServlet: Error while getting root node!", e);
                root = null;
            }

            if (root == null) {
                String msg = "Error: No root node found for owner ID " + ownerID;
                LOGGER.error(msg);
                generateErrorPage(req, res, HttpServletResponse.SC_NOT_FOUND, msg, new MCRException(msg), false);

                return;
            }

            if (root instanceof MCRFile) {
                if (st.hasMoreTokens()) {
                    // request path is too long
                    String msg = "Error: No such file or directory " + st.nextToken();
                    LOGGER.error(msg);
                    generateErrorPage(req, res, HttpServletResponse.SC_NOT_FOUND, msg, new MCRException(msg), false);

                    return;
                }

                sendFile(req, res, (MCRFile) root);

                return;
            }

            // root node is a directory
            int pos = ownerID.length() + 1;
            String path = requestPath.substring(pos);

            MCRDirectory dir = (MCRDirectory) root;
            MCRFilesystemNode node = dir.getChildByPath(path);

            if (node == null) {
                String msg = "Error: No such file or directory " + path;
                LOGGER.error(msg);
                generateErrorPage(req, res, HttpServletResponse.SC_NOT_FOUND, msg, new MCRException(msg), false);

                return;
            } else if (node instanceof MCRFile) {
                sendFile(req, res, (MCRFile) node);

                return;
            } else {
                sendDirectory(req, res, (MCRDirectory) node, lang);

                return;
            }
        }

        // remote node to be retrieved
        String prop = "MCR.remoteaccess_" + hostAlias + "_query_class";
        MCRRemoteAccessInterface comm = (MCRRemoteAccessInterface) (CONFIG.getInstanceOf(prop));

        BufferedInputStream in = comm.requestIFS(hostAlias, requestPath);

        if (in == null) {
            return;
        }

        String headercontext = comm.getHeaderContent();

        if (!headercontext.equals("text/xml")) {
            res.setContentType(headercontext);

            OutputStream out = new BufferedOutputStream(res.getOutputStream());
            MCRUtils.copyStream(in, out);
            out.close();

            return;
        }

        org.jdom.Document jdom = null;
        String style = "";
        Properties parameters = MCRLayoutServlet.buildXSLParameters(req);

        boolean ismcrxml = true;
        MCRXMLContainer resarray = new MCRXMLContainer();

        try {
            resarray.importElements(in);
        } catch (org.jdom.JDOMException e) {
            res.setContentType(headercontext);

            OutputStream out = res.getOutputStream();
            MCRUtils.copyStream(in, out);
            out.close();

            return;
        } catch (MCRException e) {
            ismcrxml = false;
        }

        if (!ismcrxml) {
            org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();

            try {
                jdom = builder.build(in);
            } catch (org.jdom.JDOMException ignores) {
            }

            style = parameters.getProperty("Style");
        } else {
            resarray.setHost(0, hostAlias);
            jdom = resarray.exportAllToDocument();
            style = parameters.getProperty("Style", "IFSMetadata-" + lang);
        }

        LOGGER.debug("Style = " + style);

        if (style.equals("xml")) {
            res.setContentType("text/xml");

            OutputStream out = res.getOutputStream();
            new XMLOutputter(Format.getPrettyFormat()).output(jdom, out);
            out.close();
        } else {
            req.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
            req.setAttribute("XSL.Style", style);

            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(req, res);
        }
    }

    /**
     * Sends the contents of an MCRFile to the client. If the MCRFile provides
     * an MCRAudioVideoExtender, the file's content is NOT sended to the client,
     * instead the stream that starts the associated streaming player is sended
     * to the client. The HTTP request may then contain StartPos and StopPos
     * parameters that contain the timecodes where to start and/or stop
     * streaming.
     */
    private void sendFile(HttpServletRequest req, HttpServletResponse res, MCRFile file) throws IOException {
        LOGGER.info("MCRFileNodeServlet: Sending file " + file.getName());

        if (file.hasAudioVideoExtender()) // Start streaming player
        {
            MCRAudioVideoExtender ext = file.getAudioVideoExtender();

            String startPos = req.getParameter("StartPos");
            String stopPos = req.getParameter("StopPos");

            res.setContentType(ext.getPlayerStarterContentType());
            ext.getPlayerStarterTo(res.getOutputStream(), startPos, stopPos);
        } else // Send contents of ordinary file
        {
            res.setContentType(file.getContentType().getMimeType());
            res.setContentLength((int) (file.getSize()));

            OutputStream out = new BufferedOutputStream(res.getOutputStream());
            file.getContentTo(out);
            out.close();
        }
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     */
    private void sendDirectory(HttpServletRequest req, HttpServletResponse res, MCRDirectory dir, String lang) throws IOException, ServletException {
        LOGGER.info("MCRFileNodeServlet: Sending listing of directory " + dir.getName());
        Document jdom=MCRDirectoryXML.getInstance().getDirectoryXML(dir);
        // prepare the stylesheet name
        Properties parameters = MCRLayoutServlet.buildXSLParameters(req);
        String style = parameters.getProperty("Style", "IFSMetadata-" + lang);
        LOGGER.debug("Style = " + style);

        if (style.equals("xml")) {
            res.setContentType("text/xml");

            OutputStream out = res.getOutputStream();
            new org.jdom.output.XMLOutputter(Format.getPrettyFormat()).output(jdom, out);
            out.close();
        } else {
            req.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
            req.setAttribute("XSL.Style", style);

            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(req, res);
        }
    }
}
