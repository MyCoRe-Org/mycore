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

package org.mycore.user2;

import org.mycore.common.config.MCRConfiguration2;
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
    /**
     * {@link MCRConfiguration2} prefix for all properties used by this MyCoRe component.
     */
    public static final String CONFIG_PREFIX = "MCR.user2.";
    static final String USER_ADMIN_PERMISSION = "administrate-users";
    static final String USER_CREATE_PERMISSION = "create-users";
    static final MCRCategoryID ROLE_CLASSID = new MCRCategoryID(ROLE_ROOT_ID);
    static final String CATEG_LINK_TYPE = "mcr-user";

    private MCRUser2Constants() {
        //do not allow instantiation
    }

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
}
