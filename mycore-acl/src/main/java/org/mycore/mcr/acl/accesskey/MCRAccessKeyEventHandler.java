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

package org.mycore.mcr.acl.accesskey;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyJsonMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * This class contains EventHandler methods to manage access keys of
 * MCRObjects and MCRDerivates.
 *
 */
public class MCRAccessKeyEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String ACCESS_KEY_FLAG_TYPE = "accesskeys";

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectCreated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleBaseCreated(obj);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectUpdated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleBaseUpdated(obj);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectDeleted(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        handleBaseDeleted(obj);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleDerivateCreated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRDerivate)
     */
    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleBaseCreated(der);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleDerivateUpdated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRDerivate)
     */
    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleBaseUpdated(der);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleDerivateDeleted(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRDerivate)
     */
    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleBaseDeleted(der);
    }

    /**
     * Add all {@link MCRAccessKey} from servflags to object.
     *
     * Then the servlag will be deleted.
     * @param obj the {@link MCRBase}
     */
    private void handleBaseCreated(final MCRBase obj) {
        final MCRObjectService service = obj.getService();
        final List<MCRAccessKeyDto> accessKeyDtos = service.getFlags(ACCESS_KEY_FLAG_TYPE).stream()
            .map(MCRAccessKeyJsonMapper::jsonToAccessKeyDtos).flatMap(s -> s.stream()).toList();
        try {
            for (MCRAccessKeyDto accessKeyDto : accessKeyDtos) {
                accessKeyDto.setReference(obj.getId().toString());
                MCRAccessKeyServiceFactory.getService().importAccessKey(accessKeyDto);
            }
        } catch (MCRAccessException e) {
            LOGGER.error("User is not allowed to import access keys to reference: " + obj.getId());
        } finally {
            service.removeFlags(ACCESS_KEY_FLAG_TYPE);
        }
    }

    /**
     * Removes {@link MCRAccessKey} string for servflags.
     *
     * {@link MCRAccessKey} string will not handled
     * @param obj the {@link MCRBase}
     */
    private void handleBaseUpdated(MCRBase obj) {
        obj.getService().removeFlags(ACCESS_KEY_FLAG_TYPE);
    }

    /**
     * Deletes all access keys for given object by its ID as reference.
     *
     * @param obj the object
     */
    private void handleBaseDeleted(MCRBase obj) {
        MCRAccessKeyServiceFactory.getService().deleteAccessKeysByReference(obj.getId().toString());
    }
}
