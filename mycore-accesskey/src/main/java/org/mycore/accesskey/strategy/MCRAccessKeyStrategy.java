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

package org.mycore.accesskey.strategy;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mycore.access.MCRAccessManager;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.accesskey.MCRAccessKeyManager;
import org.mycore.accesskey.MCRAccessKeyUserUtils;
import org.mycore.accesskey.backend.MCRAccessKey;

/**
 * Strategy for {@link MCRAccessKey}.
 */
public class MCRAccessKeyStrategy implements MCRAccessCheckStrategy {

    private static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public boolean checkPermission(String id, String permission) {
        final MCRObjectID objectId = MCRObjectID.getInstance(id);
        if (objectId.getTypeId().equals("derivate")) {
            MCRObjectID objId = MCRMetadataManager.getObjectId(objectId, 10, TimeUnit.MINUTES);
            return checkDerivatePermission(objectId, objId, permission);
        }
        return checkObjectPermission(objectId, permission);
    }

    /**
     * Checks access for an object.
     *
     * @param objectId the {@link MCRObjectID}
     * @param permission permission type
     * @return true if the access is permitted or false if not
     */
    private boolean checkObjectPermission(MCRObjectID objectId, String permission) {
        LOGGER.debug("check object {} permission {}.", objectId, permission);
        boolean isWritePermission = MCRAccessManager.PERMISSION_WRITE.equals(permission);
        boolean isReadPermission = MCRAccessManager.PERMISSION_READ.equals(permission);
        return (isWritePermission || isReadPermission) && userHasValidAccessKey(objectId, isReadPermission);
    }

    /**
     * Checks if a user has a valid {@link MCRAccessKey} for given {@link MCRObjectID} and permission.
     * If there is an invalid {@link MCRAccessKey} attribute, the attribute will be deleted.
     *
     * @param objectId the {@link MCRObjectID}
     * @param isReadPermission permission type
     * @return if {@MCRAccessKey} is valid or not for permssion.
    */
    private static boolean userHasValidAccessKey(MCRObjectID objectId, boolean isReadPermission) {
        final String userKey = MCRAccessKeyUserUtils.getAccessKey(objectId);
        if (userKey != null) {
            MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyByValue(objectId, userKey);
            if (accessKey != null) {
                if (isReadPermission && accessKey.getType().equals(MCRAccessManager.PERMISSION_READ) || 
                        accessKey.getType().equals(MCRAccessManager.PERMISSION_WRITE)) {
                    LOGGER.debug("Access granted. User has a key to access the resource {}.", objectId);
                    return true;
                }
            } else {
                MCRAccessKeyUserUtils.deleteAccessKey(objectId);
            }
        }
        return false;
    }

    /**
     * Checks access for a derivate. 
     * If no access key exists for the derivate, the access key for the parent object is checked.
     *
     * @param derivateId the {@link MCRObjectID} of the derivate
     * @param objectId the {@link MCRObjectID} of the parent
     * @param permission permission type
     * @return true if the access is permitted or false if not
     */
    private boolean checkDerivatePermission(MCRObjectID derivateId, MCRObjectID objectId, String permission) {
        LOGGER.debug("check derivate {}, object {} permission {}.", derivateId, objectId, permission);
        boolean isWritePermission = MCRAccessManager.PERMISSION_WRITE.equals(permission);
        boolean isReadPermission = MCRAccessManager.PERMISSION_READ.equals(permission);
        if ((isWritePermission || isReadPermission)) {
            return Stream.of(derivateId, objectId)
                .filter(Objects::nonNull)
                .filter(id -> MCRAccessKeyManager.getAccessKeys(id).size() > 0)
                .findFirst()
                .map(id -> userHasValidAccessKey(id, isReadPermission))
                .orElse(Boolean.FALSE);
        }
        return false;
    }
}
