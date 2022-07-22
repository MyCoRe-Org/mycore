/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.servlets;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRDeveloperTools;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.frontend.MCRLayoutUtilities;
import org.xml.sax.SAXException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This servlet displays static *.xml files stored in the web application by sending them to MCRLayoutService.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRStaticXMLFileServlet extends MCRServlet {

    private static final String READ_WEBPAGE_PERMISSION = MCRLayoutUtilities.getPermission2ReadWebpage();

    private static final long serialVersionUID = -9213353868244605750L;

    protected static final Logger LOGGER = LogManager.getLogger(MCRStaticXMLFileServlet.class);

    @Override
    public void doGetPost(MCRServletJob job) throws java.io.IOException, MCRException, SAXException, JDOMException,
        URISyntaxException, TransformerException {
        String webpageID = getWebpageId(job.getRequest());
        boolean hasAccess = MCRLayoutUtilities.webpageAccess(READ_WEBPAGE_PERMISSION, webpageID, true);
        if (!hasAccess) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        URL resource = resolveResource(job);
        if (resource != null) {
            HttpServletRequest request = job.getRequest();
            HttpServletResponse response = job.getResponse();
            setXSLParameters(resource, request);
            MCRContent content = getResourceContent(request, response, resource);
            getLayoutService().doLayout(request, response, content);
        }
    }

    private String getWebpageId(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String queryString = request.getQueryString();
        StringBuilder builder = new StringBuilder(servletPath);
        if (queryString != null && !queryString.isEmpty()) {
            builder.append('?').append(queryString);
        }
        return builder.toString();
    }

    private void setXSLParameters(URL resource, HttpServletRequest request)
        throws MalformedURLException, URISyntaxException {
        String path = resource.getProtocol().equals("file") ? resource.getPath() : resource.toExternalForm();
        int lastPathElement = path.lastIndexOf('/') + 1;
        String fileName = path.substring(lastPathElement);
        String parent = path.substring(0, lastPathElement);
        request.setAttribute("XSL.StaticFilePath", request.getServletPath().substring(1));
        request.setAttribute("XSL.DocumentBaseURL", parent);
        request.setAttribute("XSL.FileName", fileName);
        request.setAttribute("XSL.FilePath", path);
    }

    private URL resolveResource(MCRServletJob job) throws IOException {
        String requestedPath = job.getRequest().getServletPath();
        LOGGER.info("MCRStaticXMLFileServlet {}", requestedPath);

        if (MCRDeveloperTools.overrideActive()) {
            final Optional<Path> overriddenFilePath = MCRDeveloperTools
                .getOverriddenFilePath(requestedPath, true);
            if (overriddenFilePath.isPresent()) {
                return overriddenFilePath.get().toUri().toURL();
            }
        }

        URL resource = getServletContext().getResource(requestedPath);
        if (resource != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Resolved to {}", resource);
            }
            return resource;
        }

        String msg = "Could not find file " + requestedPath;
        job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, msg);
        return null;
    }

    protected MCRContent getResourceContent(HttpServletRequest request, HttpServletResponse response, URL resource)
        throws IOException, JDOMException, SAXException {
        return new MCRURLContent(resource);
    }
}
