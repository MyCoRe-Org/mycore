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

/**
 * Helper methods to query information about the user of a {@link MCRSession}.
 * <p>
 * Methods named {@code isCurrentUser...} operate on the user of the
 * {@link MCRSessionMgr#getCurrentSession() current session}.
 */
public final class MCRSessionUtils {

    private MCRSessionUtils() {
    }

    /**
     * Returns the user information of the current session.
     *
     * @return the current user information
     */
    public static MCRUserInformation getCurrentUserInformation() {
        return MCRSessionMgr.getCurrentSession().getUserInformation();
    }

    /**
     * Checks if the given user is the guest user.
     *
     * @param userInformation the user to check
     * @return true if the user ID matches the one of {@link MCRSystemUserInformation#GUEST}
     */
    public static boolean isGuest(MCRUserInformation userInformation) {
        return MCRSystemUserInformation.GUEST.getUserID().equals(userInformation.getUserID());
    }

    /**
     * Checks if the given user is the super user.
     *
     * @param userInformation the user to check
     * @return true if the user ID matches the one of {@link MCRSystemUserInformation#SUPER_USER}
     */
    public static boolean isSuperUser(MCRUserInformation userInformation) {
        return MCRSystemUserInformation.SUPER_USER.getUserID().equals(userInformation.getUserID());
    }

    /**
     * Checks if the user of the current session is the guest user.
     *
     * @return true if the current user is the guest user
     */
    public static boolean isCurrentUserGuest() {
        return isGuest(getCurrentUserInformation());
    }

    /**
     * Checks if the user of the current session is the super user.
     *
     * @return true if the current user is the super user
     */
    public static boolean isCurrentUserSuperUser() {
        return isSuperUser(getCurrentUserInformation());
    }

    /**
     * Checks if the user of the current session is in a specific role.
     *
     * @param role a role name
     * @return true if the current user has this role
     */
    public static boolean isCurrentUserInRole(String role) {
        return getCurrentUserInformation().isUserInRole(role);
    }

    /**
     * Returns an attribute of the user of the current session.
     *
     * @param attribute the attribute name
     * @return the attribute value or null
     */
    public static String getCurrentUserAttribute(String attribute) {
        return getCurrentUserInformation().getUserAttribute(attribute);
    }

}
