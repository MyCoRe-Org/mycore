/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.Collection;
import java.util.List;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyJsonMapper;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyServiceFactory;

/**
 * Event handler for managing access keys in the context of MyCoRe objects and derivates.
 */
public class MCRAccessKeyEventHandler extends MCREventHandlerBase {

    /**
     * Service flags name for access keys.
     */
    public static final String ACCESS_KEY_FLAG_TYPE = "accesskeys";

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleBaseCreated(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleBaseUpdated(obj);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        handleBaseDeleted(obj);
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleBaseCreated(der);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleBaseUpdated(der);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleBaseDeleted(der);
    }

    private void handleBaseCreated(final MCRBase obj) {
        final MCRObjectService service = obj.getService();
        final List<MCRAccessKeyDto> accessKeyDtos = service.getFlags(ACCESS_KEY_FLAG_TYPE).stream()
            .map(MCRAccessKeyJsonMapper::jsonToAccessKeyDtos).flatMap(Collection::stream).toList();
        for (MCRAccessKeyDto accessKeyDto : accessKeyDtos) {
            accessKeyDto.setReference(obj.getId().toString());
            MCRAccessKeyServiceFactory.getAccessKeyService().importAccessKey(accessKeyDto);
        }
        service.removeFlags(ACCESS_KEY_FLAG_TYPE);
    }

    private void handleBaseUpdated(MCRBase obj) {
        obj.getService().removeFlags(ACCESS_KEY_FLAG_TYPE);
    }

    private void handleBaseDeleted(MCRBase obj) {
        MCRAccessKeyServiceFactory.getAccessKeyService().removeAccessKeysByReference(obj.getId().toString());
    }
}
