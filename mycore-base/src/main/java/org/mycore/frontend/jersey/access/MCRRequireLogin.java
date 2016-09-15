/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 2, 2013 $
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
