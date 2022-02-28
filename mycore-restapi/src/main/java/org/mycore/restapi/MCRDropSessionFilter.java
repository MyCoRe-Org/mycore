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

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

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
            if (MCRTransactionHelper.isTransactionActive()) {
                MCRTransactionHelper.commitTransaction();
            }
            MCRSessionMgr.releaseCurrentSession();
            currentSession.close();
        }

    }
}
