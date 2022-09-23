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

package org.mycore.mcr.acl.accesskey.strategy;

import static org.mycore.access.MCRAccessManager.PERMISSION_PREVIEW;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_VIEW;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

public class MCRAccessKeyStrategyHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * maps view and preview permission to read
     *
     * @param permission permission
     * @return (sanitized) permission
     */
    protected static String sanitizePermission(final String permission) {
        if (PERMISSION_VIEW.equals(permission) || PERMISSION_PREVIEW.equals(permission)) {
            LOGGER.debug("Mapped {} to read.", permission);
            return PERMISSION_READ;
        }
        return permission;
    }

    /**
     * verifies {@link MCRAccessKey} for permission.
     *
     * @param permission permission type
     * @param accessKey the {@link MCRAccessKey}
     * @return true if valid, otherwise false
     */
    public static boolean verifyAccessKey(final String permission, final MCRAccessKey accessKey) {
        final String sanitizedPermission = sanitizePermission(permission);
        if (PERMISSION_READ.equals(sanitizedPermission) || PERMISSION_WRITE.equals(sanitizedPermission)) {
            if (Boolean.FALSE.equals(accessKey.getIsActive())) {
                return false;
            }
            final Date expiration = accessKey.getExpiration();
            if (expiration != null && new Date().after(expiration)) {
                return false;
            }
            if ((sanitizedPermission.equals(PERMISSION_READ)
                && accessKey.getType().equals(PERMISSION_READ))
                || accessKey.getType().equals(PERMISSION_WRITE)) {
                LOGGER.debug("Access granted.");
                return true;
            }
        }
        return false;
    }
}
