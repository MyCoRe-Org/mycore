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

package org.mycore.frontend.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDirSetup;
import org.mycore.frontend.MCRFrontendUtil;

/**
 * @author Robert Stephan
 *
 */
public class MCRSessionInitializationFilter implements Filter {

    private static Logger LOGGER = LogManager.getLogger();

    public static final String SESSION_KEY = "mcr.authenticateRequest";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        // Try to set encoding of form values
        String ReqCharEncoding = req.getCharacterEncoding();

        if (ReqCharEncoding == null) {
            // Set default to UTF-8
            ReqCharEncoding = MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8");
            req.setCharacterEncoding(ReqCharEncoding);
            LOGGER.debug("Setting ReqCharEncoding to: {}", ReqCharEncoding);
        }

        if ("true".equals(req.getParameter("reload.properties"))) {
            MCRConfigurationDirSetup setup = new MCRConfigurationDirSetup();
            setup.startUp(req.getServletContext());
        }

        MCRSession session = MCRFrontendUtil.getMCRSessionFromRequest(req);
        MCRFrontendUtil.configureSession(session, (HttpServletRequest) request, (HttpServletResponse) response);
        MCRFrontendUtil.bindSessionToThread(req, session);

        chain.doFilter(request, response);

        // Release current MCRSession from current Thread,

        MCRSessionMgr.releaseCurrentSession();
        MCRSessionMgr.lock();
    }

    @Override
    public void destroy() {
    }

}
