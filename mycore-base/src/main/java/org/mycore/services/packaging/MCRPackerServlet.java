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

package org.mycore.services.packaging;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRUsageException;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.queuedjob.MCRJob;

/**
 * <p>Servlet for {@link MCRPackerManager}.</p>
 * <p>
 * <p>You can pass a <code>redirect</code> parameter to the servlet!</p>
 * <p>The rights you need to start a Packer depends on the implementation!</p>
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRPackerServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void doGetPost(MCRServletJob job) throws IOException {
        String packer = job.getRequest().getParameter("packer");
        if (packer == null || packer.isEmpty()) {
            try {
                job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "No or invalid 'packer' parameter!");
            } catch (IOException e) {
                LOGGER.error("Error while sending request error to client!", e);
                return;
            }
        }

        Map<String, String> jobParameters = resolveJobParameters(job);

        try {
            MCRJob mcrJob = MCRPackerManager.startPacking(jobParameters);
            if (mcrJob == null) {
                job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "No packer parameter!");
            }
        } catch (MCRAccessException e) {
            job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (MCRUsageException e) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Parameters: " + e.getMessage());
        }

        if (jobParameters.containsKey("redirect")) {
            String redirect = jobParameters.get("redirect");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(redirect));
        }
    }

    private Map<String, String> resolveJobParameters(MCRServletJob job) {
        return job.getRequest().getParameterMap()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue().length >= 1) ? e.getValue()[0] : ""));
    }
}
