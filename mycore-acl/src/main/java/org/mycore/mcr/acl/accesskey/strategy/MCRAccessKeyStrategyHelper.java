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
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * Provies helper for {@link MCRAccessKeyStrategy}.
 */
public class MCRAccessKeyStrategyHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Maps view and preview permission to read.
     * 
     * @param permission permission
     * @return (sanitized) permission
     */
    protected static String sanitizePermission(String permission) {
        if (permission.equals(PERMISSION_VIEW) || permission.equals(PERMISSION_PREVIEW)) {
            LOGGER.debug("Mapped {} to read.", permission);
            return PERMISSION_READ;
        }
        return permission;
    }

    /**
     * Verifies {@link MCRAccessKey} for permission.
     * 
     * @param permission permission type
     * @param accessKey the MCRAccessKey
     * @return true if valid
     */
    public static boolean verifyAccessKey(String permission, MCRAccessKey accessKey) {
        final String sanitizedPermission = sanitizePermission(permission);
        if (sanitizedPermission.equals(PERMISSION_READ) || sanitizedPermission.equals(PERMISSION_WRITE)) {
            if (Boolean.FALSE.equals(accessKey.getIsActive())) {
                return false;
            }
            final Date expiration = accessKey.getExpiration();
            if (expiration != null && new Date().after(expiration)) {
                return false;
            }
            if ((Objects.equals(sanitizedPermission, PERMISSION_READ)
                && Objects.equals(accessKey.getType(), PERMISSION_READ))
                || Objects.equals(accessKey.getType(), PERMISSION_WRITE)) {
                LOGGER.debug("Access granted.");
                return true;
            }
        }
        return false;
    }
}
