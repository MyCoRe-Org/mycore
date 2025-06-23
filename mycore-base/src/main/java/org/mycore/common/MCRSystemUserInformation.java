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

package org.mycore.common;

import org.mycore.common.config.MCRConfiguration2;

/**
 * A {@link MCRUserInformation} implementation with no roles attached.
 *
 * @author Thomas Scheffler (yagee)
 */
public enum MCRSystemUserInformation implements MCRUserInformation {

    GUEST(new UserIdResolver("guest", "MCR.Users.Guestuser.UserName"), false),

    JANITOR(new UserIdResolver("MCRJANITOR", null), true),

    SYSTEM_USER(new UserIdResolver("SYSTEM", null), false),

    SUPER_USER(new UserIdResolver("administrator", "MCR.Users.Superuser.UserName"), true);

    private final boolean roleReturn;

    private final String userID;

    MCRSystemUserInformation(UserIdResolver userIdResolver, boolean roleReturn) {
        this.userID = userIdResolver.userId();
        this.roleReturn = roleReturn;
    }

    @Override
    public String getUserID() {
        return userID;
    }

    @Override
    public boolean isUserInRole(String role) {
        return roleReturn;
    }

    @Override
    public String getUserAttribute(String attribute) {
        return null;
    }

    private record UserIdResolver(String userId, String property) {

        @Override
        public String userId() {
            return property != null ? MCRConfiguration2.getString(property).orElse(userId) : userId;
        }

    }

}
