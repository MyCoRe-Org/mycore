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

package org.mycore.common;

import org.mycore.common.config.MCRConfiguration;

/**
 * A {@link MCRUserInformation} implementation with no roles attached.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSystemUserInformation implements MCRUserInformation {

    private static MCRSystemUserInformation systemInstance = new MCRSystemUserInformation(new UserIdResolver("SYSTEM",
        null), false);

    private static MCRSystemUserInformation guestInstance = new MCRSystemUserInformation(new UserIdResolver("guest",
        "MCR.Users.Guestuser.UserName"), false);

    private static MCRSystemUserInformation superUserInstance = new MCRSystemUserInformation(new UserIdResolver(
        "administrator", "MCR.Users.Superuser.UserName"), true);

    private boolean roleReturn;

    private UserIdResolver userID;

    private MCRSystemUserInformation(UserIdResolver userID, boolean roleReturn) {
        this.userID = userID;
        this.roleReturn = roleReturn;
    }

    /**
     * Always returns "SYSTEM" 
     */
    @Override
    public String getUserID() {
        return userID.getUserId();
    }

    /**
     * Always returns <em>false</em>
     */
    @Override
    public boolean isUserInRole(String role) {
        return roleReturn;
    }

    /**
     * @return the systemInstance
     */
    public static MCRSystemUserInformation getSystemUserInstance() {
        return systemInstance;
    }

    @Override
    public String getUserAttribute(String attribute) {
        return null;
    }

    /**
     * @return the guestInstance
     */
    public static MCRSystemUserInformation getGuestInstance() {
        return guestInstance;
    }

    /**
     * @return the superUserInstance
     */
    public static MCRSystemUserInformation getSuperUserInstance() {
        return superUserInstance;
    }

    private static class UserIdResolver {
        private String userId;

        private String property;

        public UserIdResolver(String userId, String property) {
            this.userId = userId;
            this.property = property;
        }

        public String getUserId() {
            if (property == null) {
                return userId;
            }
            return MCRConfiguration.instance().getString(property, userId);
        }
    }

}
