/*
 * $Id$
 * $Revision: 5697 $ $Date: Aug 19, 2013 $
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

package org.mycore.user2;

import java.util.Date;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTransientUser extends MCRUser {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRTransientUser.class);

    private MCRUserInformation userInfo;

    public MCRTransientUser(MCRUserInformation userInfo) {
        super();
        this.userInfo = userInfo;
        setLocked(true);
        super.setUserName(userInfo.getUserID());
        super.setLastLogin(new Date(MCRSessionMgr.getCurrentSession().getLoginTime()));
        super.setRealName(getUserAttribute(MCRUserInformation.ATT_REAL_NAME));
        super.setRealmID(getUserAttribute(MCRRealm.USER_INFORMATION_ATTR));
        for (MCRRole role : MCRRoleManager.listSystemRoles()) {
            LOGGER.debug("Test is in role: " + role.getName());
            if (userInfo.isUserInRole(role.getName())) {
                assignRole(role.getName());
            }
        }
    }

    @Override
    public String getUserAttribute(String attribute) {
        return this.userInfo.getUserAttribute(attribute);
    }

    MCRUserInformation getUserInformation() {
        return userInfo;
    }

}
