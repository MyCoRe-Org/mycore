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

package org.mycore.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
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

    @Serial
    private static final long serialVersionUID = 1L;

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
        return MCRResourceResolver.obtainInstance().resolveWebResource(path);
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
