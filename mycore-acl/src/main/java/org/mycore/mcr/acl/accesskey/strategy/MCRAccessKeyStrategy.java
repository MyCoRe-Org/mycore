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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyConfig;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.MCRAccessKeySessionService;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUserService;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * Strategy for {@link MCRAccessKey}.
 */
public class MCRAccessKeyStrategy implements MCRAccessCheckStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRAccessKeyUserService userService;

    private final MCRAccessKeySessionService sessionService;

    /**
     * Constructs new {@link MCRAccessKeyStrategy} with default user and session service
     */
    public MCRAccessKeyStrategy() {
        userService = MCRAccessKeyServiceFactory.getUserService();
        sessionService = MCRAccessKeyServiceFactory.getSessionService();
    }

    /**
     * Constructs new {@link MCRAccessKeyStrategy} with given user and session service.
     *
     * @param userService the user service
     * @param sessionService the session
     */
    protected MCRAccessKeyStrategy(MCRAccessKeyUserService userService, MCRAccessKeySessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @Override
    public boolean checkPermission(String reference, String permission) {
        if ((MCRAccessManager.PERMISSION_READ.equals(permission) || MCRAccessManager.PERMISSION_WRITE.equals(permission)
            || MCRAccessManager.PERMISSION_VIEW.equals(permission)
            || MCRAccessManager.PERMISSION_PREVIEW.equals(permission))
            && MCRObjectID.isValid(reference)) {
            final MCRObjectID objectId = MCRObjectID.getInstance(reference);
            if ("derivate".equals(objectId.getTypeId())) {
                return checkDerivatePermission(objectId, permission);
            }
            return checkObjectPermission(objectId, permission);
        }
        return false;
    }

    /**
     * Fetches access key and checks derivate permission.
     *
     * @param objectId the {@link MCRObjectID}
     * @param permission permission type
     * @return true if permitted, otherwise false
     */
    public boolean checkDerivatePermission(MCRObjectID objectId, String permission) {
        LOGGER.debug("check derivate {} permission {}.", objectId.toString(), permission);
        if ((MCRAccessManager.PERMISSION_READ.equals(permission) || MCRAccessManager.PERMISSION_WRITE.equals(permission)
            || MCRAccessManager.PERMISSION_VIEW.equals(permission)
            || MCRAccessManager.PERMISSION_PREVIEW.equals(permission))
            && MCRAccessKeyConfig.getAllowedObjectTypes().contains(objectId.getTypeId())) {
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

    /**
     * Fetches access key and checks object permission.
     *
     * @param objectId the {@link MCRObjectID}
     * @param permission permission type
     * @return true if permitted, otherwise false
     */
    public boolean checkObjectPermission(MCRObjectID objectId, String permission) {
        LOGGER.debug("check object {} permission {}.", objectId.toString(), permission);
        if ((MCRAccessManager.PERMISSION_READ.equals(permission) || MCRAccessManager.PERMISSION_WRITE.equals(permission)
            || MCRAccessManager.PERMISSION_VIEW.equals(permission)
            || MCRAccessManager.PERMISSION_PREVIEW.equals(permission))
            && MCRAccessKeyConfig.getAllowedObjectTypes().contains(objectId.getTypeId())) {
            if (MCRAccessKeyConfig.getAllowedSessionPermissionTypes().contains(permission)) {
                final MCRAccessKeyDto accessKeyDto = sessionService
                    .getActivatedAccessKeyForReference(objectId.toString());
                if (accessKeyDto != null && MCRAccessKeyStrategyHelper.verifyAccessKey(permission, accessKeyDto)) {
                    return true;
                }
            }
            final MCRAccessKeyDto accessKeyDto = userService.getActivatedAccessKeyForReference(objectId.toString());
            return accessKeyDto != null && MCRAccessKeyStrategyHelper.verifyAccessKey(permission, accessKeyDto);
        }
        return false;
    }

}
