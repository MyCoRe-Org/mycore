/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mcr.acl.accesskey.frontend.filter;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.Base64;

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
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUtils;

/**
 * Servlet filter that extracts access key value from query string and includes value if valid as
 * an attribute into the session
 */
public class MCRAccessKeyFilter implements Filter {
   
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ALLOWED_PERMISSION_TYPES = MCRConfiguration2
        .getString("MCR.AccessKey.Session.AllowedPermissionTypes")
        .orElse(null);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (ALLOWED_PERMISSION_TYPES != null && ALLOWED_PERMISSION_TYPES.length() > 0) {
            LOGGER.info("MCRAccessKeyFilter is enabled and the following permssions are allowed: {}", 
                ALLOWED_PERMISSION_TYPES);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (ALLOWED_PERMISSION_TYPES != null && ALLOWED_PERMISSION_TYPES.length() > 0) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            final MCRObjectID objectId = extractObjectId(httpServletRequest);
            if (objectId != null) {
                String value = httpServletRequest.getParameter("accesskey");
                if (value != null) {
                    try {
                        MCRServlet.initializeMCRSession(httpServletRequest, getFilterName());
                        MCRFrontendUtil.configureSession(MCRSessionMgr.getCurrentSession(), httpServletRequest, 
                            (HttpServletResponse) response);
                        value = new String(Base64.getUrlDecoder().decode(value.getBytes(UTF_8)), UTF_8);
                        MCRAccessKeyUtils.addAccessKeyToCurrentSession(objectId, value);
                    } catch (Exception e) {
                        LOGGER.debug("Cannot set access key to session", e);
                        MCRTransactionHelper.rollbackTransaction();
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
        //not needed
    }

    private String getFilterName() {
        return this.getClass().getSimpleName();
    }

    private static MCRObjectID extractObjectId(final HttpServletRequest request) {
        final String pathInfo = request.getPathInfo();
        final String id = pathInfo == null ? null : pathInfo.substring(1);
        if (id != null) {
            try {
                return MCRObjectID.getInstance(id);
            } catch (final MCRException e) {
                LOGGER.debug("Cannot convert {} to MCRObjectID", id);
            }
        }
        return null;
    }
}
