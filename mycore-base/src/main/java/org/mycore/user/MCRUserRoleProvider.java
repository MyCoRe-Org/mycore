/*
 * $Id$
 * $Revision: 5697 $ $Date: 29.11.2010 $
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

package org.mycore.user;

import org.mycore.common.MCRUserInformation;

/**
 * A {@link MCRUserInformation} implementation based upon {@link MCRUser}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUserRoleProvider implements MCRUserInformation {

    private MCRUser user;

    public MCRUserRoleProvider(MCRUser user) {
        this.user = user;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRUserInformation#getCurrentUserID()
     */
    @Override
    public String getCurrentUserID() {
        return user.getID();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRUserInformation#isUserInRole(java.lang.String)
     */
    @Override
    public boolean isUserInRole(String role) {
        return user.getGroupIDs().contains(role);
    }

}
