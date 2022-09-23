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

package org.mycore.mcr.acl.accesskey.restapi.v2.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUtils;
import org.mycore.restapi.v2.MCRRestAuthorizationFilter;

@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class MCRAccessKeyFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        LOGGER.debug("Access key filter started.");
        if (!MCRAccessKeyUtils.isAccessKeyForSessionAllowed()) {
            LOGGER.debug("Access keys are not allowed for session. Skipping filter...");
            return;
        }
        if (!MCRSessionMgr.hasCurrentSession()) {
            LOGGER.debug("Session is not initialised. Skipping filter...");
            return;
        }
        final String secret = requestContext.getHeaderString("X-Access-Key");
        if (secret != null) {
            LOGGER.debug("Found X-Access-Key with value {}.", secret);
            final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
            final String objectIdString = pathParameters.getFirst(MCRRestAuthorizationFilter.PARAM_MCRID);
            if (objectIdString != null) {
                try {
                    final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
                    if (objectId != null) {
                        MCRAccessKeyUtils.addAccessKeySecretToCurrentSession(objectId, secret);
                    }
                } catch (MCRException e) {
                    LOGGER.debug("The access key could not be added to the current session: ", e);
                }
            }
        }
    }
}
