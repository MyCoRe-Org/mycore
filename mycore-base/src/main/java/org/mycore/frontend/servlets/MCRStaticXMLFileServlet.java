/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.TransformerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.resource.MCRResourceHelper;
import org.xml.sax.SAXException;

/**
 * This servlet displays static *.xml files stored in the web application by sending them to MCRLayoutService.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRStaticXMLFileServlet extends MCRServlet {

    private static final String READ_WEBPAGE_PERMISSION = MCRLayoutUtilities.getPermission2ReadWebpage();

    private static final long serialVersionUID = -9213353868244605750L;

    private static final String REDIRECT_GUESTS_PROPERTY = "MCR.StaticXMLFileServlet.NoAccess.RedirectGuestsToLogin";

    private static final boolean REDIRECT_GUESTS = MCRConfiguration2
        .getBoolean(REDIRECT_GUESTS_PROPERTY)
        .orElse(false);

    private static final String REDIRECT_GUESTS_XSL_STATUS_MESSAGE = MCRConfiguration2
        .getString(REDIRECT_GUESTS_PROPERTY + ".XSLStatusMessage")
        .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
        .orElse("");

    private static final String REDIRECT_GUESTS_XSL_STATUS_STYLE = MCRConfiguration2
        .getString(REDIRECT_GUESTS_PROPERTY + ".XSLStatusStyle")
        .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
        .orElse("");

    protected static final Logger LOGGER = LogManager.getLogger(MCRStaticXMLFileServlet.class);

    @Override
    public void doGetPost(MCRServletJob job) throws IOException, MCRException, SAXException, JDOMException,
        TransformerException {
        String webpageID = getWebpageId(job.getRequest());
        boolean hasAccess = MCRLayoutUtilities.webpageAccess(READ_WEBPAGE_PERMISSION, webpageID, true);
        if (!hasAccess) {
            HttpServletResponse response = job.getResponse();
            MCRUserInformation currentUser = MCRSessionMgr.getCurrentSession().getUserInformation();
            if (REDIRECT_GUESTS && currentUser.equals(MCRSystemUserInformation.getGuestInstance())) {
                String contextPath = job.getRequest().getContextPath();
                String encodedURL = URLEncoder.encode(contextPath + webpageID, StandardCharsets.UTF_8);
                StringBuilder redirectTarget = new StringBuilder();
                redirectTarget.append(contextPath).append("/servlets/MCRLoginServlet");
                redirectTarget.append("?url=").append(encodedURL);
                if (!REDIRECT_GUESTS_XSL_STATUS_MESSAGE.isEmpty() && !REDIRECT_GUESTS_XSL_STATUS_STYLE.isEmpty()) {
                    redirectTarget.append("&XSL.Status.Message=").append(REDIRECT_GUESTS_XSL_STATUS_MESSAGE);
                    redirectTarget.append("&XSL.Status.Style=").append(REDIRECT_GUESTS_XSL_STATUS_STYLE);
                }
                String redirectUrl = response.encodeRedirectURL(redirectTarget.toString());
                response.setStatus(403);
                response.sendRedirect(redirectUrl);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } else {
            URL resource = resolveResource(job);
            if (resource != null) {
                HttpServletRequest request = job.getRequest();
                HttpServletResponse response = job.getResponse();
                setXSLParameters(resource, request);
                MCRContent content = getResourceContent(request, response, resource);
                getLayoutService().doLayout(request, response, content);
            }
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

    private void setXSLParameters(URL resource, HttpServletRequest request) {
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

        URL resource = MCRResourceHelper.getWebResourceUrl(requestedPath);
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
