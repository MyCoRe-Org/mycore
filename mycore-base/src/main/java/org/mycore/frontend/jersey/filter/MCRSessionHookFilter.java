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

package org.mycore.frontend.jersey.filter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

@Priority(Priorities.AUTHENTICATION)
public class MCRSessionHookFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private HttpServletResponse httpResponse;

    private final static String ATTR = MCRSessionHookFilter.class.getName() + ".session";

    private static final Logger LOGGER = LogManager.getLogger(MCRSessionHookFilter.class);

    @Override
    public void filter(ContainerRequestContext request) {
        MCRSessionMgr.unlock();
        MCRSession session = MCRServlet.getSession(httpRequest);
        request.setProperty(ATTR, session);
        MCRSessionMgr.setCurrentSession(session);
        LOGGER.info("{} ip={} mcr={} user={}", request.getUriInfo().getPath(),
            MCRFrontendUtil.getRemoteAddr(httpRequest), session.getID(), session.getUserInformation().getUserID());
        MCRFrontendUtil.configureSession(session, httpRequest, httpResponse);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MCRSessionMgr.unlock();
        MCRSession requestSession = (MCRSession) requestContext.getProperty(ATTR);
        if (responseContext.hasEntity()) {
            responseContext.setEntityStream(new ProxyOutputStream(responseContext.getEntityStream()) {
                @Override
                public void close() throws IOException {
                    LOGGER.debug("Closing EntityStream");
                    try {
                        super.close();
                    } finally {
                        releaseSessionIfNeeded(requestSession);
                        LOGGER.debug("Closing EntityStream done");
                    }
                }
            });
        } else {
            LOGGER.debug("No Entity in response, closing MCRSession");
            releaseSessionIfNeeded(requestSession);
        }
    }

    private static void releaseSessionIfNeeded(MCRSession requestSession) {
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            try {
                if (currentSession.isTransactionActive()) {
                    LOGGER.debug("Active MCRSession and JPA-Transaction found. Clearing up");
                    if (currentSession.transactionRequiresRollback()) {
                        currentSession.rollbackTransaction();
                    } else {
                        currentSession.commitTransaction();
                    }
                } else {
                    LOGGER.debug("Active MCRSession found. Clearing up");
                }
            } finally {
                MCRSessionMgr.releaseCurrentSession();
                if (!currentSession.equals(requestSession)) {
                    LOGGER.warn("Found orphaned MCRSession. Closing {} ", currentSession);
                    currentSession.close(); //is not bound to HttpSession
                }
                MCRSessionMgr.lock();
                LOGGER.debug("Session released.");
            }
        }
    }

}
