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

package org.mycore.mcr.acl.accesskey.restapi.v2.filter;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

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
                    //
                }
            }
        }
    }
}
