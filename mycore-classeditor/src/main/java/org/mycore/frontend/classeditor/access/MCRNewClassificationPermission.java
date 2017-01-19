/*
 * $Id$
 * $Revision: 5697 $ $Date: Feb 19, 2013 $
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

package org.mycore.frontend.classeditor.access;

import java.text.MessageFormat;

import javax.ws.rs.container.ContainerRequestContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.classifications2.utils.MCRClassificationUtils;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRNewClassificationPermission implements MCRResourceAccessChecker {

    private static final Logger LOGGER = LogManager.getLogger(MCRNewClassificationPermission.class);

    /* (non-Javadoc)
     * @see org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker#isPermitted(com.sun.jersey.spi.container.ContainerRequest)
     */
    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        LOGGER.info(MessageFormat.format("{0} has permission {1}?", request.getUriInfo().getPath(),
            MCRClassificationUtils.CREATE_CLASS_PERMISSION));
        return MCRAccessManager.checkPermission(MCRClassificationUtils.CREATE_CLASS_PERMISSION);
    }

}
