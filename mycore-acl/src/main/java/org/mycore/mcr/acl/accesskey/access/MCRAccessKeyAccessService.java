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

package org.mycore.mcr.acl.accesskey.access;

import org.mycore.access.MCRAccessException;

/**
 * This interfaces provides methods to manage access keys.
 */
public interface MCRAccessKeyAccessService {

    /**
     * Validates whether the current user is allowed to manage access key for reference and permission.
     *
     * @param reference the reference
     * @param permission the permission
     * @throws MCRAccessException if current user is allowed to manage access keys for reference and permission
     */
    void validateManagePermission(String reference, String permission) throws MCRAccessException;

    /**
     * Checks whether the current user is allowed to manage access keys for reference and permission.
     *
     * @param reference the reference
     * @param permission the permission
     * @return true if current user is allowed to manage access keys for reference and permission
     */
    boolean checkManagePermission(String reference, String permission);

}
