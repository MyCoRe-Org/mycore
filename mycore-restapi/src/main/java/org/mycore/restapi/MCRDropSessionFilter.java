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

package org.mycore.restapi;

import java.io.IOException;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.mycore.backend.jpa.MCRJPAUtil;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

@Priority(Priorities.USER + 1)
/**
 * Drops and closes MCRSession if Resource produces Server-Sent Events.
 */
public class MCRDropSessionFilter implements ContainerRequestFilter {
    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Produces produces = resourceInfo.getResourceMethod().getAnnotation(Produces.class);
        if (produces == null || Stream.of(produces.value()).noneMatch(MediaType.SERVER_SENT_EVENTS::equals)) {
            return;
        }
        LogManager.getLogger().info("Has Session? {}", MCRSessionMgr.hasCurrentSession());
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            if (MCRJPAUtil.isTransactionActive()) {
                MCRJPAUtil.commitTransaction();
            }
            MCRSessionMgr.releaseCurrentSession();
            currentSession.close();
        }

    }
}
