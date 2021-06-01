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

package org.mycore.access;

import org.mycore.common.MCRUserInformation;

public interface MCRAccessInterface {
    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     *
     * All information regarding the current user is capsulated by a
     * <code>MCRSession</code> instance which can be retrieved by
     *
     * <pre>
     * MCRSession currentSession = MCRSessionMgr.getCurrentSession();
     * </pre>
     *
     * This method is used for checking "a priori permissions" like "create-document"
     *     where a String ID does not exist yet
     *
     * @param permission
     *            the permission/action to be granted, e.g. "create-document"
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    boolean checkPermission(String permission);

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     */
    boolean checkPermission(String id, String permission);

    /**
     * determines whether a given user has the permission to perform a
     * certain action. no session data will be checked here.
     *
     * This method is used for checking "a priori permissions" like "create-document"
     *     where a String ID does not exist yet
     *
     * @param permission
     *            the permission/action to be granted, e.g. "create-document"
     * @param userInfo
     *            the MCRUser, whose permissions are checked
     * @return true if the permission is granted, else false
     */
    // TODO: maybe rename to checkPermission or rename checkPermission to checkPermissionForUser
    boolean checkPermissionForUser(String permission, MCRUserInformation userInfo);

    /**
     * determines whether a given user has the permission to perform a
     * certain action. no session data will be checked here.
     *
     *
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     *
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the permission/action to be granted, e.g. "read"
     * @param userInfo
     *            the MCRUser, whose permissions are checked
     * @return true if the permission is granted, else false
     */
    boolean checkPermission(String id, String permission, MCRUserInformation userInfo);

}
