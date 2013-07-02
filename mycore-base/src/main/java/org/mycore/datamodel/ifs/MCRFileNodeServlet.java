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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

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
public class MCRFileNodeServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    // The Log4J logger
    private static Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class.getName());

    // initialize it with an empty string -if propertie is missing,
    // because in a case of MCRConfigurationException,
    // no Servlet will be instantiated, and thats more bad then a missing
    // property!
    private static String accessErrorPage = MCRConfiguration.instance().getString("MCR.Access.Page.Error", "");

    @Override
    protected long getLastModified(HttpServletRequest request) {
        Transaction tx = null;
        try {
            String ownerID = getOwnerID(request);
            tx = MCRHIBConnection.instance().getSession().beginTransaction();
            MCRFilesystemNode root = MCRFilesystemNode.getRootNode(ownerID);
            MCRFilesystemNode node = ((MCRDirectory) root).getChildByPath(getPath(request));
            final long lastModified = node.getLastModified().getTimeInMillis();
            tx.commit();
            LOGGER.debug("getLastModified returned: " + lastModified);
            return lastModified;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            // any error would let us return -1 here
            LOGGER.info("Error while getting last modified date.", e);
            return -1;
        } finally {
            /*
             * A new MCRSession may be created due to MCRHIBConnection implementation.
             * As the MCRSession is not bound to a HttpSession that is closed automatically,
             * we close it here, if no IP address is known.
             */
            MCRSession session = MCRSessionMgr.getCurrentSession();
            if (session.getCurrentIP().length() < 7) {
                //it's a stalled session close it
                session.close();
            }
        }
    }

    /**
     * Handles the HTTP request
     */
    @Override
    public void doGetPost(MCRServletJob job) throws IOException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        if (!isParametersValid(request, response)) {
            return;
        }
        handleLocalRequest(job);
    }

    private boolean isParametersValid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestPath = request.getPathInfo();
        LOGGER.info("MCRFileNodeServlet: request path = " + requestPath);

        if (requestPath == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error: HTTP request path is null");
            return false;
        }
        return true;
    }

    /**
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    private void handleLocalRequest(MCRServletJob job) throws IOException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        String ownerID = getOwnerID(request);
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
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No root node found for owner ID " + ownerID);
            return;
        }

        if (root instanceof MCRFile) {
            if (request.getPathInfo().length() > ownerID.length() + 1) {
                // request path is too long
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Error: No such file or directory " + request.getPathInfo());
                return;
            }
            sendFile(job, (MCRFile) root);
            return;
        }

        // root node is a directory
        MCRDirectory dir = (MCRDirectory) root;
        String path = getPath(request);
        MCRFilesystemNode node = dir.getChildByPath(path);

        if (node == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Error: No such file or directory " + path);
            return;
        } else if (node instanceof MCRFile) {
            sendFile(job, (MCRFile) node);
            return;
        } else {
            sendDirectory(request, response, (MCRDirectory) node);
            return;
        }
    }

    /**
     *  retrieves the derivate ID of the owning derivate from request path.
     *  @param request - the http request object
     */
    protected static String getOwnerID(HttpServletRequest request) {
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
    protected static String getPath(HttpServletRequest request) {
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
    private void sendFile(MCRServletJob job, MCRFile file) throws IOException {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (!MCRAccessManager.checkPermissionForReadingDerivate(file.getOwnerID())) {
            LOGGER.info("MCRFileNodeServlet: AccessForbidden to " + file.getName());
            res.sendRedirect(res.encodeRedirectURL(getBaseURL() + accessErrorPage));
            return;
        }

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
            res.setHeader("Content-Length", String.valueOf(file.getSize()));
            res.addHeader("Accept-Ranges", "none"); // Advice client not to attempt range requests
            // no transaction needed to copy long streams over slow connections
            MCRSessionMgr.getCurrentSession().commitTransaction();
            OutputStream out = new BufferedOutputStream(res.getOutputStream());
            file.getContentTo(out);
            out.close();
        }
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     */
    private void sendDirectory(HttpServletRequest req, HttpServletResponse res, MCRDirectory dir) throws IOException {
        LOGGER.info("MCRFileNodeServlet: Sending listing of directory " + dir.getName());
        Document jdom = MCRDirectoryXML.getInstance().getDirectoryXML(dir);
        layoutDirectory(req, res, jdom);

    }

    /**
     * Called to layout the directory structure
     * 
     * @param req
     *            the html request
     * @param res
     *            the html response
     * @param jdom
     *            the jdom document
     * @throws IOException
     * see overwritten in JSPDocportal
     */
    protected void layoutDirectory(HttpServletRequest req, HttpServletResponse res, Document jdom) throws IOException {
        getLayoutService().doLayout(req, res, new MCRJDOMContent(jdom));
    }

    /**
     * Forwards the error to generate the output
     * 
     * see its overwritten in jspdocportal
     */
    protected void errorPage(HttpServletRequest req, HttpServletResponse res, int error, String msg, Exception ex, boolean xmlstyle) throws IOException {
        generateErrorPage(req, res, error, msg, ex, xmlstyle);
    }
}
