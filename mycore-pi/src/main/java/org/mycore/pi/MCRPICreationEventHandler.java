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

package org.mycore.pi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPICreationEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        processPIServices(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        processPIServices(obj);
    }

    private void processPIServices(MCRObject obj) {

        // list of service IDs for PI service for which an PI is already registered
        Set<String> servicesWithRegisteredIdentifier = MCRPIManager
            .getInstance()
            .getRegistered(obj)
            .stream()
            .map(MCRPIRegistrationInfo::getService)
            .collect(Collectors.toSet());

        // map of auto-creating PI services for which no PI is registered yet
        Map<String, MCRPIService<MCRPersistentIdentifier>> autoCreatingServices = MCRPIServiceManager
            .getInstance()
            .getAutoCreationList()
            .stream()
            .filter(service -> !servicesWithRegisteredIdentifier.contains(service.getServiceID()))
            .filter(service -> !MCRPIService.hasFlag(obj, "", service))
            .collect(Collectors.toMap(MCRPIService::getServiceID, Function.identity()));

        boolean mayRegisterAdditionalIdentifiers = true;
        while (mayRegisterAdditionalIdentifiers && !autoCreatingServices.isEmpty()) {

            // list of auto-creating PI services that will register a PI in this iteration
            List<MCRPIService<MCRPersistentIdentifier>> matchingAutoCreatingServices = autoCreatingServices
                .values()
                .stream()
                .filter(service -> service.getCreationPredicate().test(obj))
                .toList();

            // additional PIs will be registered in this iteration, so the conditions will change
            // and additional predicates may evaluate to true in the next iteration
            mayRegisterAdditionalIdentifiers = !matchingAutoCreatingServices.isEmpty();

            for (MCRPIService<MCRPersistentIdentifier> service : matchingAutoCreatingServices) {
                try {

                    // register new PI, succeeds or throws exception
                    MCRPersistentIdentifier identifier = service.register(obj, "", false);

                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Created new identifier {} for object  {} using auto-creating PI service {}",
                            identifier, obj.getId(), service.getServiceID());
                    }

                    // each auto-creating PI service ony gets one shot at registering a PI
                    autoCreatingServices.remove(service.getServiceID());

                } catch (MCRAccessException | MCRPersistentIdentifierException | ExecutionException
                    | InterruptedException e) {
                    throw new MCRException("Error while register pi for object " + obj.getId().toString(), e);
                }
            }

        }

    }

}
