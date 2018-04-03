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

package org.mycore.pi;

import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import com.google.gson.Gson;

public class MCRPersistentIdentifierEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @SuppressWarnings("unchecked")
    public static void updateObject(MCRObject obj) {
        detectServices(obj, (service, registrationInfo) -> {
            try {
                service.onUpdate(getIdentifier(registrationInfo), obj, registrationInfo.getAdditional());
            } catch (MCRPersistentIdentifierException e) {
                throw new MCRException(e);
            }
        });
    }

    private static void detectServices(MCRObject obj, BiConsumer<MCRPIService, MCRPIRegistrationInfo> r) {
        MCRPIServiceManager serviceManager = MCRPIServiceManager.getInstance();

        List<MCRPIRegistrationInfo> registered = MCRPIManager.getInstance().getRegistered(obj);
        List<String> serviceList = serviceManager.getServiceIDList();

        for (MCRPIRegistrationInfo pi : registered) {
            String serviceName = pi.getService();
            if (serviceList.contains(serviceName)) {
                getIdentifier(pi);
                MCRPIService<MCRPersistentIdentifier> registrationService = serviceManager
                    .getRegistrationService(serviceName);
                r.accept(registrationService, pi);
            } else {
                LOGGER
                    .warn(() -> "The service " + serviceName + " was removed from properties, so the update function!");
            }
        }
    }

    private static MCRPersistentIdentifier getIdentifier(MCRPIRegistrationInfo pi) {
        MCRPIManager identifierManager = MCRPIManager.getInstance();
        MCRPIParser<?> parser = identifierManager.getParserForType(pi.getType());

        return parser.parse(pi.getIdentifier())
            .orElseThrow(() -> new MCRException("Cannot parse a previous inserted identifier"));
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        /* Add PIs to DB if they are not there */
        MCRPIManager.getInstance().getRegistered(obj)
            .forEach(pi -> MCRPIManager.getInstance().delete(pi.getMycoreID(), pi.getAdditional(),
                pi.getType(),
                pi.getService()));

        Gson gson = new Gson();
        obj.getService().getFlags(MCRPIService.PI_FLAG).stream()
            .map(piFlag -> gson.fromJson(piFlag, MCRPI.class))
            .filter(entry -> !MCRPIManager.getInstance().exist(entry))
            .forEach(entry -> {
                //TODO: disabled for MCR-1393
                //                    entry.setMcrRevision(MCRCoreVersion.getRevision());
                entry.setMcrVersion(MCRCoreVersion.getVersion());
                entry.setMycoreID(obj.getId().toString());
                LOGGER.info("Add PI : {} with service {} to database!", entry.getIdentifier(), entry.getService());
                MCRHIBConnection.instance().getSession().save(entry);
            });

        handleObjectUpdated(evt, obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        updateObject(obj);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        detectServices(obj, (service, registrationInfo) -> {
            try {
                service.onDelete(getIdentifier(registrationInfo), obj, registrationInfo.getAdditional());
            } catch (MCRPersistentIdentifierException e) {
                throw new MCRException(e);
            }
        });
    }

}
