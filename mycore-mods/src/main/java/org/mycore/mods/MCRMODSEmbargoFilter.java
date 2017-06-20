/*
 * $Id$
 * $Revision: 5697 $ $Date: Jun 19, 2014 $
 *
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

package org.mycore.mods;

import static org.mycore.mods.MCRMODSEmbargoUtils.POOLPRIVILEGE_EMBARGO;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.services.i18n.MCRTranslation;

/**
 * Enforces embargo of mods documents to {@link MCRFileNodeServlet}.
 * @author Thomas Scheffler (yagee)
 * @deprecated You should implement your own {@link MCRAccessCheckStrategy}. For a example you can look at MIRStrategy.
 */
public class MCRMODSEmbargoFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger(MCRMODSEmbargoFilter.class);

    private static final long EXPIRE = 1;

    private static final TimeUnit EXPIRE_UNIT = TimeUnit.HOURS;

    public static boolean isReadAllowed(final MCRObjectID objectId) {
        if (objectId == null || !"mods".equals(objectId.getTypeId())) {
            return true;
        }

        return MCRAccessManager.checkPermission(objectId, MCRAccessManager.PERMISSION_READ)
            && (MCRAccessManager.checkPermission(POOLPRIVILEGE_EMBARGO)
                || MCRMODSEmbargoUtils.isCurrentUserCreator(objectId));
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        LOGGER.warn(MCRMODSEmbargoFilter.class.getName() + " is deprecated!");
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
        ServletException {
        boolean forward = true; //forward to servlet by default
        HttpServletRequest req = (HttpServletRequest) request;
        boolean newSession = req.getSession(false) == null;
        MCRSession session = MCRServlet.getSession(req);
        MCRSessionMgr.setCurrentSession(session);
        try {
            String ownerID = MCRFileNodeServlet.getOwnerID(req);
            MCRObjectID derivateID = MCRObjectID.getInstance(ownerID);
            //get session for DB access
            session.beginTransaction();
            try {
                MCRObjectID objectId = MCRMetadataManager.getObjectId(derivateID, EXPIRE, EXPIRE_UNIT);
                if (!newSession && isReadAllowed(objectId)) {
                    return; //user is allowed to read
                }
                final String embargo = MCRMODSEmbargoUtils.getEmbargo(objectId);
                if (embargo != null) {
                    LOGGER.warn(MessageFormat.format("Denied request {0} that is under embargo until {1}.",
                        req.getPathInfo(), embargo));
                    String embargoMsg = MCRTranslation.translate("component.mods.error.underEmbargo",
                        objectId.toString(), embargo);
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, embargoMsg);
                    forward = false;
                    return;
                }
            } finally {
                session.commitTransaction();
            }
        } finally {
            MCRSessionMgr.releaseCurrentSession();
            if (newSession) {
                HttpSession httpSession = req.getSession(false);
                if (httpSession != null) {
                    httpSession.invalidate();
                }
            }
            if (forward) {
                LOGGER.info("Request " + req.getPathInfo() + " verified by " + getClass().getSimpleName());
                filterChain.doFilter(request, response);
            }
        }
    }

}
