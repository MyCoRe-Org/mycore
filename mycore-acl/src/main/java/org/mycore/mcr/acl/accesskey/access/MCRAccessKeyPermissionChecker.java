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

import org.mycore.access.MCRAccessManager;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;

/**
 * The {@code MCRAccessKeyPermissionChecker} class provides methods to check
 * whether a user has permission to manage an access key.
 * It uses permission checking based on a combination of reference and permission.
 */
public class MCRAccessKeyPermissionChecker {

    /**
     * Prefix appended to the permission to check if a user has management
     * rights for an access key.
     */
    private static final String MANAGE_PERMISSION_PREFIX = "manage-access-key-";

    /**
     * Checks if the current user has permission to manage the given access key
     * based on the information provided in the {@code MCRAccessKeyDto}.
     *
     * @param accessKeyDto the {@code MCRAccessKeyDto} of the access key
     * @return {@code true} if the user has permission to manage the access key; otherwise {@code false}
     */
    public boolean canManageAccessKey(MCRAccessKeyDto accessKeyDto) {
        return canManageAccessKey(accessKeyDto.getReference(), accessKeyDto.getPermission());
    }

    /**
     * Checks if the current user has permission to manage the access key
     * based on the given reference and permission.
     *
     * @param reference  the reference identifying the access key
     * @param permission the permission to check for
     * @return {@code true} if the user has permission to manage the access key; otherwise {@code false}.
     */
    public boolean canManageAccessKey(String reference, String permission) {
        return MCRAccessManager.checkPermission(reference, MANAGE_PERMISSION_PREFIX + permission);
    }
}
