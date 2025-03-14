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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.NoSuchFileException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.util.MCRServletContentHelper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRContentServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

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
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp) {
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
            LOGGER.info("Caught exception while serving content", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        }

        final Supplier<Object> getResourceName = () -> {
            String id = "";
            if (Objects.nonNull(content.getSystemId())) {
                id = content.getSystemId();
            } else if (Objects.nonNull(content.getName())) {
                id = content.getName();
            }
            return String.format(Locale.ROOT, "Finished serving resource:%s", id);
        };
        LOGGER.debug(getResourceName);
    }

    public MCRServletContentHelper.Config getConfig() {
        return config;
    }

}
