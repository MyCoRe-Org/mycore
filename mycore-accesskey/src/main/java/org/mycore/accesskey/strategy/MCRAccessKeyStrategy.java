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
        if (MCRAccessManager.PERMISSION_WRITE.equals(permission) 
            || MCRAccessManager.PERMISSION_READ.equals(permission)) {
            if (MCRObjectID.isValid(id)) {
                final MCRObjectID objectId = MCRObjectID.getInstance(id);
                if (objectId.getTypeId().equals("derivate")) {
                    MCRObjectID objId = MCRMetadataManager.getObjectId(objectId, 10, TimeUnit.MINUTES);
                    LOGGER.debug("check derivate {}, object {} permission {}.", objectId, objId, permission);
                    return Stream.of(objectId, objId)
                        .filter(Objects::nonNull)
                        .filter(_id -> MCRAccessKeyManager.getAccessKeys(_id).size() > 0)
                        .findFirst()
                        .map(_id -> userHasValidAccessKey(_id, permission))
                        .orElse(Boolean.FALSE);
                }
                LOGGER.debug("check object {} permission {}.", objectId, permission);
                return userHasValidAccessKey(objectId, permission);
            }
        }
        return false;
    }

    /**
     * Checks if a user has a valid {@link MCRAccessKey} for given {@link MCRObjectID} and permission.
     * If there is an invalid {@link MCRAccessKey} attribute, the attribute will be deleted.
     *
     * @param objectId the {@link MCRObjectID}
     * @param permission permission type
     * @return if {@MCRAccessKey} is valid or not for permssion.
    */
    private static boolean userHasValidAccessKey(MCRObjectID objectId, String permission) {
        final String userKey = MCRAccessKeyUserUtils.getAccessKey(objectId);
        if (userKey != null) {
            MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyByValue(objectId, userKey);
            if (accessKey != null) {
                if ((permission.equals(MCRAccessManager.PERMISSION_READ) 
                    && accessKey.getType().equals(MCRAccessManager.PERMISSION_READ)) 
                    || accessKey.getType().equals(MCRAccessManager.PERMISSION_WRITE)) {
                    LOGGER.debug("Access granted. User has a key to access the resource {}.", objectId);
                    return true;
                }
            } else {
                MCRAccessKeyUserUtils.deleteAccessKey(objectId);
            }
        }
        return false;
    }
}
