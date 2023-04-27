/*
 *
 * $Revision$ $Date$
 *
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 *
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.common.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.content.util.MCRServletContentHelper;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A {@link HttpServlet} that delivers web resources identified by the {@link HttpServletRequest#getPathInfo()}
 * of the incoming request, using {@link MCRResourceResolver#resolveWebResource(String)}.
 */
public final class MCRResourceServlet extends HttpServlet {

    private static final Logger LOGGER = LogManager.getLogger();

    private MCRServletContentHelper.Config config;

    @Override
    public void init() throws ServletException {
        this.config = MCRServletContentHelper.buildConfig(getServletConfig());
    }

    @Override
    public void doOptions(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Allow", "GET, HEAD, OPTIONS");
    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doServe(request, response, false);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doServe(request, response, true);
    }

    private void doServe(HttpServletRequest request, HttpServletResponse response, boolean serveContent)
        throws IOException {
        String path = request.getPathInfo();
        if (path != null) {
            doServe(request, response, path, serveContent);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No resource path given");
        }
    }

    private void doServe(HttpServletRequest request, HttpServletResponse response, String path, boolean serveContent)
        throws IOException {
        Optional<MCRContent> content = getContent(path);
        if (content.isPresent()) {
            try {
                ServletContext context = getServletContext();
                MCRServletContentHelper.serveContent(content.get(), request, response, context, config, serveContent);
            } catch (NoSuchFileException | FileNotFoundException e) {
                LOGGER.warn("Caught exception while serving content", e);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource " + path + " not found");
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource " + path + " not found");
        }
    }

    public Optional<MCRContent> getContent(String path) throws IOException {
        return toUrl(path).map(this::toContent);
    }

    private Optional<URL> toUrl(String path) {
        LOGGER.debug("Delivering resource for path {}", path);
        return MCRResourceResolver.instance().resolveWebResource(path);
    }

    private MCRURLContent toContent(URL url) {
        LOGGER.debug("Delivering content for url {}", url);
        MCRURLContent content = new MCRURLContent(url);
        content.setMimeType(getMimeType(url));
        return content;
    }

    private String getMimeType(URL url) {
        return getServletContext().getMimeType(url.toString());
    }

}
