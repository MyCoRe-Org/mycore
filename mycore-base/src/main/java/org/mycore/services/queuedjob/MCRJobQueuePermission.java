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
package org.mycore.services.queuedjob;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRJobQueuePermission implements MCRResourceAccessChecker {

    public static final String PERMISSION_LIST = "list-jobqueues";

    private static Logger LOGGER = LogManager.getLogger(MCRJobQueuePermission.class);

    /* (non-Javadoc)
     * @see org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker#isPermitted(com.sun.jersey.spi.container.ContainerRequest)
     */
    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        String queueName = request.getUriInfo().getPathSegments().size() > 1
            ? request.getUriInfo().getPathSegments().get(1).getPath()
            : null;
        try {
            if (queueName == null || queueName.isEmpty()) {
                return MCRAccessManager.checkPermission(PERMISSION_LIST);
            }

            String permissionType = request.getMethod().matches("(?i)(POST|PUT|DELETE)") ? PERMISSION_WRITE
                : PERMISSION_READ;
            if (!MCRAccessManager.checkPermission(queueName, permissionType)) {
                LOGGER.info("Permission \"{}\" denied for queue \"{}\".", permissionType, queueName);
                return false;
            }

            return true;
        } catch (Exception exc) {
            throw new WebApplicationException(exc,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Unable to check permission for request " + request.getUriInfo().getRequestUri()
                        + " containing entity value " + queueName)
                    .build());
        }
    }

}
