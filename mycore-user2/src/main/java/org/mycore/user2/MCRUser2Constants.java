/*
 * $Id$
 * $Revision: 5697 $ $Date: 16.02.2012 $
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

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public final class MCRUser2Constants {

    /**
     * {@link MCRCategoryID} root ID for system roles.
     */
    public static final String ROLE_ROOT_ID = "mcr-roles";

    static final String USER_ADMIN_PERMISSION = "administrate-users";

    static final String USER_CREATE_PERMISSION = "create-users";

    /**
     * @return the userAdminPermission
     */
    public static String getUserAdminPermission() {
        return USER_ADMIN_PERMISSION;
    }

    /**
     * @return the userCreatePermission
     */
    public static String getUserCreatePermission() {
        return USER_CREATE_PERMISSION;
    }

    /**
     * @return the roleRootId
     */
    public static String getRoleRootId() {
        return ROLE_ROOT_ID;
    }

    static final MCRCategoryID ROLE_CLASSID = MCRCategoryID.rootID(ROLE_ROOT_ID);

    static final String CATEG_LINK_TYPE = "mcr-user";

    /**
     * {@link MCRConfiguration} prefix for all properties used by this MyCoRe component.
     */
    public static final String CONFIG_PREFIX = "MCR.user2.";

    private MCRUser2Constants() {
        //do not allow instantiation
    }
}
