/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.frontend.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRUtils;

public class MCRFSMapperServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRFSMapperServlet.class);

    private static MCRConfiguration CONFIG = MCRConfiguration.instance();

    @Override
    protected void doGet(MCRServletJob job) throws Exception {
        File requestFile = getFile(job.getRequest());
        LOGGER.info("Requesting file: "+requestFile);
        final HttpServletResponse response = job.getResponse();
        if (requestFile == null) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "URI invalid: " + job.getRequest().getRequestURI());
            return;
        }
        if (!requestFile.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + requestFile.getAbsolutePath());
            return;
        }
        if (requestFile.isDirectory()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "File is directory: " + requestFile.getAbsolutePath());
            return;
        }
        
        if (requestFile.getName().endsWith(".xml")){
            //special handling for XML files
            MCRStaticXMLFileServlet.processFile(job.getRequest(), response, requestFile);
            return;
        }
        
        //process all non-xml files
        final long fileSize = requestFile.length();
        final String mimeType = getServletContext().getMimeType(requestFile.getName());
        response.setContentLength((int) fileSize);
        response.setContentType(mimeType);
        MCRUtils.copyStream(new FileInputStream(requestFile), response.getOutputStream());
        response.flushBuffer();
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        final File requestFile = getFile(request);
        /**
         * As XML files are processed by an Stylesheet the final output could
         * change even the lastModified() time is unchanged.
         */
        if (requestFile == null || requestFile.getName().endsWith(".xml")) {
            return -1L;
        }
        return requestFile.lastModified();
    }

    private final File getFile(HttpServletRequest request) {
        final String servletPath = request.getServletPath();
        final String relativePath;
        LOGGER.debug("getServletPath:" + servletPath);
        try {
            relativePath = URLDecoder.decode(request.getRequestURI(), "UTF-8").substring(servletPath.length() + 1);
            LOGGER.debug("relative Path:" + relativePath);
        } catch (UnsupportedEncodingException e) {
            // should never happen
            LOGGER.warn("Cannot decode URI: " + request.getRequestURI(), e);
            return null;
        }
        if (relativePath.contains("..")) {
            // don't allow outbreak to access system files;
            LOGGER.warn("Possible attempt to read system files! Requested file: "+relativePath);
            return null;
        }
        final String basePath = CONFIG.getString("MCR.FSMap." + servletPath, getServletContext().getRealPath(""));
        LOGGER.debug("basePath: " + basePath);
        File baseFile = new File(basePath);
        return new File(baseFile, relativePath);
    }

}
