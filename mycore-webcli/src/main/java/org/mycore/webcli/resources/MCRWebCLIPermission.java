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

package org.mycore.webcli.resources;

import javax.ws.rs.container.ContainerRequestContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class MCRWebCLIPermission implements MCRResourceAccessChecker {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        if (!MCRAccessManager.getAccessImpl().checkPermission("use-webcli")) {
            LOGGER.info("Permission denied on MCRWebCLI");
            return false;
        }
        return true;
    }

}
