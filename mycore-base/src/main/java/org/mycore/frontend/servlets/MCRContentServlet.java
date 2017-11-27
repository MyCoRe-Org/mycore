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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.util.MCRServletContentHelper;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRContentServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LogManager.getLogger(MCRContentServlet.class);

    private MCRServletContentHelper.Config config;

    /**
     * Returns MCRContent matching current request.
     */
    public abstract MCRContent getContent(HttpServletRequest req, HttpServletResponse resp) throws IOException;

    @Override
    public void init() throws ServletException {
        super.init();
        this.config = MCRServletContentHelper.buildConfig(getServletConfig());
    }

    /**
     * Handles a HEAD request for the specified content.
     */
    @Override
    protected void doHead(final HttpServletRequest request, final HttpServletResponse response) throws IOException,
        ServletException {

        request.setAttribute(MCRServletContentHelper.ATT_SERVE_CONTENT, Boolean.FALSE);
        super.doGet(request, response);
    }

    @Override
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
        IOException {
        resp.setHeader("Allow", "GET, HEAD, POST, OPTIONS");
    }

    @Override
    protected void render(final MCRServletJob job, final Exception ex) throws Exception {
        if (ex != null) {
            throw ex;
        }
        final HttpServletRequest request = job.getRequest();
        final HttpServletResponse response = job.getResponse();
        final MCRContent content = getContent(request, response);
        boolean serveContent = MCRServletContentHelper.isServeContent(request);
        try {
            MCRServletContentHelper.serveContent(content, request, response, getServletContext(), getConfig(),
                serveContent);
        } catch (NoSuchFileException | FileNotFoundException e) {
            LOGGER.info("Catched {}:", e.getClass().getSimpleName(), e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        }
        LOGGER.info("Finished serving resource.");
    }

    public MCRServletContentHelper.Config getConfig() {
        return config;
    }

}
