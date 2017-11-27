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

package org.mycore.frontend.jersey.access;

import javax.ws.rs.container.ContainerRequestContext;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRequireLogin implements MCRResourceAccessChecker {

    /**
     * Returns true if and only if the current user is logged in.
     */
    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        if (MCRSessionMgr.hasCurrentSession()) {
            return !MCRSessionMgr.getCurrentSession().getUserInformation().getUserID().equals(
                MCRSystemUserInformation.getGuestInstance().getUserID());
        }
        return false;
    }

}
