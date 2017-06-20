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
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRPathXML;
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
 * @version $Revision$ $Date$
 */
public class MCRFileNodeServlet extends MCRContentServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LogManager.getLogger(MCRFileNodeServlet.class);

    private static Pattern patternDerivateID = Pattern.compile(".+_derivate_[0-9]+");

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRContentServlet#getContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public MCRContent getContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isParametersValid(request, response)) {
            return null;
        }
        String ownerID = getOwnerID(request);
        if (!MCRAccessManager.checkPermissionForReadingDerivate(ownerID)) {
            LOGGER.info("AccessForbidden to " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        String path = getPath(request);
        MCRPath mcrPath = MCRPath.getPath(ownerID, path);
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(mcrPath, BasicFileAttributes.class);
        } catch (NoSuchFileException e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "File or directory not found: " + mcrPath;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return null;
        }
        if (attr.isDirectory()) {
            try {
                return sendDirectory(request, response, mcrPath);
            } catch (TransformerException | SAXException e) {
                throw new IOException(e);
            }
        }
        if (attr.isRegularFile()) {
            return sendFile(request, response, mcrPath);
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not a file or directory: " + mcrPath);
        return null;
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
     *  Attention: derivateID is not always at the first position in path 
     *  @param request - the http request object
     */
    public static String getOwnerID(HttpServletRequest request) {
        String pI = request.getPathInfo();
        return Arrays.stream(pI.split("/"))
            .filter(fragment -> patternDerivateID.matcher(fragment).matches())
            .findFirst()
            .orElse("");
    }

    /**
     *  Retrieves the path of the file to display from request path.
     *  @param request - the http request object
     */
    protected String getPath(HttpServletRequest request) {
        String pI = request.getPathInfo();
        String ownerID = getOwnerID(request);
        int pos = pI.indexOf(ownerID) + ownerID.length() + 1;
        if (pos - 1 >= pI.length()) {
            return "/";
        }
        String path = pI.substring(pos);
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.length() == 0) {
            return "/";
        }
        return path.toString();
    }

    private MCRContent sendFile(HttpServletRequest request, HttpServletResponse response, MCRPath mcrPath) {
        // TODO: Does MCRFileNodeServlet really has to handle IFS1 AudioVideoExtender support? (last rev: 30037))
        return new MCRPathContent(mcrPath);
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     * @throws SAXException 
     * @throws TransformerException 
     */
    private MCRContent sendDirectory(HttpServletRequest request, HttpServletResponse response, MCRPath mcrPath)
        throws IOException, TransformerException, SAXException {
        Document directoryXML = MCRPathXML.getDirectoryXML(mcrPath);
        MCRJDOMContent source = new MCRJDOMContent(directoryXML);
        source.setLastModified(Files.getLastModifiedTime(mcrPath).toMillis());
        String fileName = mcrPath.getNameCount() == 0 ? mcrPath.getOwner() : mcrPath.getFileName().toString();
        source.setName(fileName);
        return getLayoutService().getTransformedContent(request, response, source);
    }

}
