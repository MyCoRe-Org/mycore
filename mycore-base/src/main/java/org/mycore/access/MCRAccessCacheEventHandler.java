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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Updates the access cache when a object is updated or deleted
 */
public class MCRAccessCacheEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        clearPermissionCache(obj);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        LOGGER.info("Invalidate permission cache for derivate {}", der.getId());
        MCRAccessManager.invalidAllPermissionCachesById(der.getId().toString());
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleDerivateUpdated(evt, der);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        clearPermissionCache(obj);
    }

    /**
     * removes all cached permissions for the object and all direct children and all direct derivates
     * @param obj the object
     */
    private void clearPermissionCache(MCRObject obj) {
        LOGGER.info("Invalidate permission cache for obj {}", obj.getId());
        MCRAccessManager.invalidAllPermissionCachesById(obj.getId().toString());
        final String[] ids = Stream
            .of(obj.getStructure().getChildren().stream(), obj.getStructure().getDerivates().stream())
            .flatMap(s -> s)
            .map(MCRMetaLinkID::getXLinkHrefID)
            .map(MCRObjectID::toString)
            .toArray(String[]::new);

        MCRAccessManager.invalidAllPermissionCachesById(ids);
    }
}
