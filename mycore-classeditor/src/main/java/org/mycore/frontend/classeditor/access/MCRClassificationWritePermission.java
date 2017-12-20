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

package org.mycore.frontend.classeditor.access;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.frontend.classeditor.utils.MCRCategUtils;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRClassificationWritePermission implements MCRResourceAccessChecker {
    public static final String PERMISSION_CREATE = "create-class";

    private static Logger LOGGER = LogManager.getLogger(MCRClassificationWritePermission.class);

    /* (non-Javadoc)
     * @see org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker
     *  #isPermitted(com.sun.jersey.spi.container.ContainerRequest)
     */
    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        String value = convertStreamToString(request.getEntityStream());
        try {
            //        Set<MCRCategoryID> categories = MCRCategUtils.getRootCategoryIDs(value);
            HashMap<MCRCategoryID, String> categories = MCRCategUtils.getCategoryIDMap(value);
            if (categories == null) {
                LOGGER.error("Could not parse {}", value);
                return false;
            }
            for (Map.Entry<MCRCategoryID, String> categoryEntry : categories.entrySet()) {
                MCRCategoryID category = categoryEntry.getKey();
                String state = categoryEntry.getValue();

                if (!hasPermission(category, state)) {
                    LOGGER.info("Permission {} denied on classification {}", category.getRootID(), category);
                    return false;
                }
            }
            return true;
        } catch (Exception exc) {
            throw new WebApplicationException(exc,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Unable to check permission for request " + request.getUriInfo().getRequestUri()
                        + " containing entity value " + value)
                    .build());
        }
    }

    private boolean hasPermission(MCRCategoryID category, String state) {
        if ("new".equals(state)) {
            return MCRAccessManager.checkPermission(PERMISSION_CREATE);
        }
        return MCRAccessManager.checkPermission(category.getRootID(), PERMISSION_WRITE);
    }

}
