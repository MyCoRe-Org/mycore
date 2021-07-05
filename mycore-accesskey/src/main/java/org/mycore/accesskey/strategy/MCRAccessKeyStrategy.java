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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mycore.access.MCRAccessManager;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.accesskey.MCRAccessKeyUserUtils;
import org.mycore.accesskey.backend.MCRAccessKey;

/**
 * Strategy for {@link MCRAccessKey}.
 */
public class MCRAccessKeyStrategy implements MCRAccessCheckStrategy {

    private static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public boolean checkPermission(String id, String permission) {
        if ((MCRAccessManager.PERMISSION_WRITE.equals(permission) 
            || MCRAccessManager.PERMISSION_READ.equals(permission)) && MCRObjectID.isValid(id)) {
            MCRObjectID objectId = MCRObjectID.getInstance(id);
            if (objectId.getTypeId().equals("derivate")) {
                LOGGER.debug("check derivate {} permission {}.", objectId.toString(), permission);
                MCRAccessKey accessKey = MCRAccessKeyUserUtils.getAccessKey(objectId);
                if (accessKey != null) {
                    return checkPermission(permission, accessKey);
                }
                objectId = MCRMetadataManager.getObjectId(objectId, 10, TimeUnit.MINUTES);
            }
            LOGGER.debug("check object {} permission {}.", objectId.toString(), permission);
            final MCRAccessKey accessKey = MCRAccessKeyUserUtils.getAccessKey(objectId);
            if (accessKey != null) {
                return checkPermission(permission, accessKey);
            }
        }
        return false;
    }

    private boolean checkPermission(String permission, MCRAccessKey accessKey) {
        if ((permission.equals(MCRAccessManager.PERMISSION_READ) 
            && accessKey.getType().equals(MCRAccessManager.PERMISSION_READ)) 
            || accessKey.getType().equals(MCRAccessManager.PERMISSION_WRITE)) {
            LOGGER.debug("Access granted. User has a key to access the resource {}.",
                accessKey.getObjectId().toString());
            return true;
        }
        return false;
    }

    public boolean checkPermission(String id, String permission, MCRAccessKey accessKey) {
        LOGGER.debug("check object {} permission {}.", id, permission);
        if (MCRObjectID.isValid(id) && id.equals(accessKey.getObjectId().toString())) {
            return checkPermission(permission, accessKey);
        }
        return false;
    }
}
