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

package org.mycore.frontend.classeditor.access;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.classifications2.utils.MCRClassificationUtils;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRNewClassificationPermission implements MCRResourceAccessChecker {

    private static final Logger LOGGER = LogManager.getLogger();

    /* (non-Javadoc)
     * @see org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker
     *  #isPermitted(com.sun.jersey.spi.container.ContainerRequest)
     */
    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        LOGGER.info("{} has permission {}?", () -> request.getUriInfo().getPath(),
            () -> MCRClassificationUtils.CREATE_CLASS_PERMISSION);
        return MCRAccessManager.checkPermission(MCRClassificationUtils.CREATE_CLASS_PERMISSION);
    }

}
