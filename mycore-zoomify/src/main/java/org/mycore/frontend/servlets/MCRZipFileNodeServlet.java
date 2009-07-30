/*
 * 
 * $Revision: 15202 $ $Date: 2009-05-15 17:00:44 +0200 (Fr, 15 Mai 2009) $
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

package org.mycore.frontend.servlets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/**
 * This servlet support the extraction form a requested file from a zip archive. The name of the zip archive is like the root directory of the requested file path. As sample the request is http://localhost:1234/servlets/MCRZiFileNodeServlet/Project_derivate_00000001/test/dir/file2 the stored zip file is test.zip that includes test/dir/file2. This construct is helpful to use to free Zoomify solution.
 * 
 * @author Stefan Freitag
 * @author Jens Kupferschmidt
 * @author Frank L\u00fctzenkirchen
 * @version $Revision: 15202 $ $Date: 2008-01-14 11:02:17 +0000 (Mo, 14 Jan 2008) $
 */
public class MCRZipFileNodeServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    // The Log4J logger
    private static Logger LOGGER = Logger.getLogger(MCRZipFileNodeServlet.class.getName());

    // initialize it with an empty string -if propertie is missing,
    // because in a case of MCRConfigurationException,
    // no Servlet will be instantiated, and thats more bad then a missing
    // property!
    private static String accessErrorPage = MCRConfiguration.instance().getString("MCR.Access.Page.Error", "");

    /**
     * Handles the HTTP request
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        if (!isParametersValid(request, response)) {
            return;
        }
        handleLocalRequest(job);
    }

    /**
     * 
     * @param request the servlet request
     * @param response the servlet response
     * @return true if the path of the request is okay
     * @throws IOException if the presentation of error page fails
     */
    private boolean isParametersValid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestPath = request.getPathInfo();
        LOGGER.info("MCRZipFileNodeServlet: request path = " + requestPath);
        if (requestPath == null) {
            String msg = "Error: HTTP request path is null";
            LOGGER.error(msg);
            errorPage(request, response, HttpServletResponse.SC_BAD_REQUEST, msg, new MCRException("No path was given in the request"), false);
            return false;
        }
        return true;
    }

    /**
     * This method handle the request against the zip file and split the path infornation.
     * @param job the MyCoRe servlet job
     * @throws Exception all throwed exceptions
     */
    private void handleLocalRequest(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        // get path of requested file
        String ownerID = getOwnerID(request);
        String requestPath = request.getPathInfo();
        // get path for zip file
        StringBuffer zippath = new StringBuffer(128);
        StringBuffer filepath = new StringBuffer(128);
        StringTokenizer st = new StringTokenizer(requestPath, "/");
        zippath.append('/').append(st.nextToken()).append('/');
        filepath.append(st.nextToken());
        zippath.append(filepath).append(".zip");
        while (st.hasMoreTokens()) {
            filepath.append('/').append(st.nextToken());
        }

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
            errorPage(request, response, HttpServletResponse.SC_NOT_FOUND, msg, new MCRException(msg), false);

            return;
        }

        // root node is a directory
        int pos = ownerID.length() + 1;
        StringBuffer path = new StringBuffer(zippath.toString().substring(pos));
        if ((path.charAt(path.length() - 1) == '/') && path.length() > 1) {
            path.deleteCharAt(path.length() - 1);
        }

        MCRDirectory dir = (MCRDirectory) root;

        MCRFilesystemNode node = dir.getChildByPath(path.toString());
        LOGGER.debug("MCRFileNodeServlet: the requested path of the zip file is "+path.toString());
        
        if (node == null) {
            String msg = "Error: No such file or directory " + path;
            LOGGER.error(msg);
            errorPage(request, response, HttpServletResponse.SC_NOT_FOUND, msg, new MCRException(msg), false);
            return;
        } else if (node instanceof MCRFile) {
            // check access
            if (!MCRAccessManager.checkPermissionForReadingDerivate(node.getOwnerID())) {
                LOGGER.info("MCRFileNodeServlet: AccessForbidden to " + node.getName());
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + accessErrorPage));
                return;
            }
            sendFile(job, (MCRFile) node, filepath.toString());
            return;
        } else {
            LOGGER.warn("The access of directories is no supported by MCRZipFileNodeServlet");
            return;
        }
    }

    /**
     * The method return the MyCoRe IFS owner ID (DerivateID) from the requested path.
     * @param request the HTTP request
     * @return the string with the MyCoRe IFS owner ID (DerivateID)
     */
    protected static String getOwnerID(HttpServletRequest request) {
        String pI = request.getPathInfo();
        StringBuffer ownerID = new StringBuffer(request.getPathInfo().length());
        boolean running = true;
        for (int i = (pI.charAt(0) == '/') ? 1 : 0; (i < pI.length() && running); i++) {
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
     */
    private void sendFile(MCRServletJob job, MCRFile node, String filepath) throws Exception {
        // build zip file
        try {
            // get zip file (stored temporary local with nonotime as filename
            LOGGER.debug("MCRZipFileNodeServlet: the path of the requested file is " + filepath);
            String dir = MCRConfiguration.instance().getString("MCR.datadir", "");
            String filename = String.valueOf(System.nanoTime());
            File f = new File(dir, filename);
            node.getContentTo(f);
            ZipFile zf = new ZipFile(f);
            // build response
            HttpServletResponse res = job.getResponse();
            if (filepath.endsWith(".jpg")) {
                res.setContentType("image/jpeg");
            }
            if (filepath.endsWith(".xml")) {
                res.setContentType("text/xml");
            }
            // no transaction needed to copy long streams over slow connections
            ZipEntry entry = zf.getEntry(filepath);
            InputStream input = zf.getInputStream(entry);
            MCRSessionMgr.getCurrentSession().commitTransaction();
            OutputStream output = new BufferedOutputStream(res.getOutputStream());
            MCRUtils.copyStream(input, output);
            output.close();
            f.delete();
            LOGGER.debug("MCRZipFileNodeServlet: Sending file " + filepath);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Forwards the error to generate the output
     * 
     * @author A.Schaar
     * @see its overwritten in jspdocportal
     */
    protected void errorPage(HttpServletRequest req, HttpServletResponse res, int error, String msg, Exception ex, boolean xmlstyle) throws IOException {
        generateErrorPage(req, res, error, msg, ex, xmlstyle);
    }
}
