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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUtils;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * Strategy for {@link MCRAccessKey}.
 */
public class MCRAccessKeyStrategy implements MCRAccessCheckStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean checkPermission(final String objectIdString, final String permission) {
        if ((PERMISSION_READ.equals(permission) || PERMISSION_WRITE.equals(permission)
            || PERMISSION_VIEW.equals(permission) || PERMISSION_PREVIEW.equals(permission))
            && MCRObjectID.isValid(objectIdString)) {
            final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
            if ("derivate".equals(objectId.getTypeId())) {
                return checkDerivatePermission(objectId, permission);
            }
            return checkObjectPermission(objectId, permission);
        }
        return false;
    }

    /**
     * Fetches access key and checks object permission
     *
     * @param objectId the {@link MCRObjectID}
     * @param permission permission type
     * @return true if permitted, otherwise false
     */
    public boolean checkObjectPermission(final MCRObjectID objectId, final String permission) {
        LOGGER.debug("check object {} permission {}.", objectId.toString(), permission);
        if ((PERMISSION_READ.equals(permission) || PERMISSION_WRITE.equals(permission)
            || PERMISSION_VIEW.equals(permission) || PERMISSION_PREVIEW.equals(permission))
            && MCRAccessKeyUtils.isAccessKeyForObjectTypeAllowed(objectId.getTypeId())) {
            if (MCRAccessKeyUtils.isAccessKeyForSessionAllowed(permission)) {
                final MCRAccessKey accessKey = MCRAccessKeyUtils.getLinkedAccessKeyFromCurrentSession(objectId);
                if (accessKey != null && MCRAccessKeyStrategyHelper.verifyAccessKey(permission, accessKey)) {
                    return true;
                }
            }
            final MCRAccessKey accessKey = MCRAccessKeyUtils.getLinkedAccessKeyFromCurrentUser(objectId);
            if (accessKey != null && MCRAccessKeyStrategyHelper.verifyAccessKey(permission, accessKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches access key and checks derivate permission
     *
     * @param objectId the {@link MCRObjectID}
     * @param permission permission type
     * @return true if permitted, otherwise false
     */
    public boolean checkDerivatePermission(final MCRObjectID objectId, final String permission) {
        LOGGER.debug("check derivate {} permission {}.", objectId.toString(), permission);
        if ((PERMISSION_READ.equals(permission) || PERMISSION_WRITE.equals(permission)
            || PERMISSION_VIEW.equals(permission) || PERMISSION_PREVIEW.equals(permission))
            && MCRAccessKeyUtils.isAccessKeyForObjectTypeAllowed(objectId.getTypeId())) {
            if (checkObjectPermission(objectId, permission)) {
                return true;
            }
            final MCRObjectID parentObjectId = MCRMetadataManager.getObjectId(objectId, 10, TimeUnit.MINUTES);
            if (parentObjectId != null) {
                return checkObjectPermission(parentObjectId, permission);
            }
        }
        return false;
    }
}
