/*
 * 
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRContentServlet;
import org.xml.sax.SAXException;

/**
 * This servlet delivers the contents of an MCRFilesystemNode to the client
 * browser. If the node is a ordinary MCRFile, the contents of that file will be
 * sent to the browser. If the node is an MCRFile with a MCRAudioVideoExtender,
 * the message that starts the associated streaming player will be delivered. If
 * the node is a MCRDirectory, the contents of that directory will be forwareded
 * to MCRLayoutService as XML data to display a detailed directory listing.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author A.Schaar
 * @author Robert Stephan
 * 
 * @version $Revision$ $Date: 2008-01-14 11:02:17 +0000 (Mo, 14 Jan
 *          2008) $
 */
public class MCRFileNodeServlet extends MCRContentServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class);

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRContentServlet#getContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public MCRContent getContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isParametersValid(request, response)) {
            return null;
        }
        String ownerID = getOwnerID(request);
        // local node to be retrieved
        MCRFilesystemNode root;

        try {
            root = MCRFilesystemNode.getRootNode(ownerID);
        } catch (org.mycore.common.MCRPersistenceException e) {
            // Could not get value from JDBC result set
            LOGGER.error("Error while getting root node!", e);
            root = null;
        }

        if (root == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No root node found for owner ID " + ownerID);
            return null;
        }

        if (root instanceof MCRFile) {
            if (request.getPathInfo().length() > ownerID.length() + 1) {
                // request path is too long
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Error: No such file or directory " + request.getPathInfo());
                return null;
            }
            return sendFile(request, response, (MCRFile) root);
        }

        // root node is a directory
        MCRDirectory dir = (MCRDirectory) root;
        String path = getPath(request);
        MCRFilesystemNode node = dir.getChildByPath(path);

        if (node == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Error: No such file or directory " + path);
            return null;
        } else if (node instanceof MCRFile) {
            return sendFile(request, response, (MCRFile) node);
        } else {
            try {
                return sendDirectory(request, response, (MCRDirectory) node);
            } catch (TransformerException | SAXException e) {
                throw new IOException(e);
            }
        }
    }

    private boolean isParametersValid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestPath = request.getPathInfo();
        LOGGER.info("request path = " + requestPath);

        if (requestPath == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error: HTTP request path is null");
            return false;
        }
        return true;
    }

    /**
     *  retrieves the derivate ID of the owning derivate from request path.
     *  @param request - the http request object
     */
    public static String getOwnerID(HttpServletRequest request) {
        String pI = request.getPathInfo();
        StringBuilder ownerID = new StringBuilder(request.getPathInfo().length());
        boolean running = true;
        for (int i = pI.charAt(0) == '/' ? 1 : 0; i < pI.length() && running; i++) {
            switch (pI.charAt(i)) {
                case '/':
                    running = false;
                    break;
                default:
                    ownerID.append(pI.charAt(i));
                    break;
            }
        }
        return ownerID.toString();
    }

    /**
     *  Retrieves the path of the file to display from request path.
     *  @param request - the http request object
     */
    protected String getPath(HttpServletRequest request) {
        String ownerID = getOwnerID(request);
        int pos = ownerID.length() + 1;
        StringBuilder path = new StringBuilder(request.getPathInfo().substring(pos));
        if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
            path.deleteCharAt(path.length() - 1);
        }
        if (path.length() == 0) {
            return "/";
        }
        return path.toString();
    }

    /**
     * Sends the contents of an MCRFile to the client. If the MCRFile provides
     * an MCRAudioVideoExtender, the file's content is NOT sended to the client,
     * instead the stream that starts the associated streaming player is sended
     * to the client. The HTTP request may then contain StartPos and StopPos
     * parameters that contain the timecodes where to start and/or stop
     * streaming.
     */
    private MCRContent sendFile(HttpServletRequest req, HttpServletResponse res, MCRFile file) throws IOException {
        if (!MCRAccessManager.checkPermissionForReadingDerivate(file.getOwnerID())) {
            LOGGER.info("AccessForbidden to " + file.getName());
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        LOGGER.info("Sending file " + file.getName() + ("HEAD".equals(req.getMethod()) ? " (HEAD only)" : ""));

        if (file.hasAudioVideoExtender()) {
            // Start streaming player
            MCRAudioVideoExtender ext = file.getAudioVideoExtender();

            String startPos = req.getParameter("StartPos");
            String stopPos = req.getParameter("StopPos");

            return ext.getPlayerStarter(startPos, stopPos);
        } else {
            // Send contents of ordinary file
            return file.getContent();
        }
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     * @throws SAXException 
     * @throws TransformerException 
     */
    private MCRContent sendDirectory(HttpServletRequest req, HttpServletResponse res, MCRDirectory dir)
        throws IOException, TransformerException, SAXException {
        LOGGER.info("Sending listing of directory " + dir.getName()
            + ("HEAD".equals(req.getMethod()) ? " (HEAD only)" : ""));
        Document jdom = MCRDirectoryXML.getInstance().getDirectoryXML(dir);
        MCRJDOMContent source = new MCRJDOMContent(jdom);
        source.setLastModified(dir.getLastModified().getTimeInMillis());
        source.setName(dir.getName());
        return getLayoutService().getTransformedContent(req, res, source);
    }

}
