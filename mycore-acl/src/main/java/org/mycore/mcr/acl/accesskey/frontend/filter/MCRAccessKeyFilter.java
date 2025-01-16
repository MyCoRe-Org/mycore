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

package org.mycore.mcr.acl.accesskey.frontend.filter;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.mcr.acl.accesskey.config.MCRAccessKeyConfig;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyServiceFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that extracts access key secret from query string for object and
 * activates access key by secret if valid as for current session.
 */
public class MCRAccessKeyFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void init(FilterConfig filterConfig) {
        if (LOGGER.isInfoEnabled() && !MCRAccessKeyConfig.getAllowedSessionPermissionTypes().isEmpty()) {
            LOGGER.info("MCRAccessKeyObjectServletFilter is enabled and the following permssions are allowed: {}",
                () -> String.join(", ", MCRAccessKeyConfig.getAllowedSessionPermissionTypes()));
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (!MCRAccessKeyConfig.getAllowedSessionPermissionTypes().isEmpty()) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            final MCRObjectID objectId = extractObjectId(httpServletRequest);
            if (objectId != null) {
                final String secret = httpServletRequest.getParameter("accesskey");
                if (secret != null) {
                    try {
                        MCRServlet.initializeMCRSession(httpServletRequest, getFilterName());
                        MCRFrontendUtil.configureSession(MCRSessionMgr.getCurrentSession(), httpServletRequest,
                            (HttpServletResponse) response);
                        MCRAccessKeyServiceFactory.getAccessKeySessionService()
                            .activateAccessKey(objectId.toString(), secret);
                    } catch (Exception e) {
                        LOGGER.error("Cannot set access key '{}' for {} to session", secret, objectId, e);
                        MCRTransactionManager.rollbackTransactions();
                    } finally {
                        MCRServlet.cleanupMCRSession(httpServletRequest, getFilterName());
                        httpServletRequest.removeAttribute(MCRServlet.CURRENT_THREAD_NAME_KEY);
                        httpServletRequest.removeAttribute(MCRServlet.INITIAL_SERVLET_NAME_KEY);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // not needed
    }

    private String getFilterName() {
        return this.getClass().getSimpleName();
    }

    private static MCRObjectID extractObjectId(HttpServletRequest request) {
        final String pathInfo = request.getPathInfo();
        final String id = pathInfo == null ? null : pathInfo.substring(1);
        if (id != null) {
            try {
                return MCRObjectID.getInstance(id);
            } catch (MCRException e) {
                LOGGER.error("Cannot convert {} to MCRObjectID", id);
            }
        }
        return null;
    }
}
