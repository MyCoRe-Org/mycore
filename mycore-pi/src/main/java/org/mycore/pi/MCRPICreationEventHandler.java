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
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPICreationEventHandler extends MCREventHandlerBase {

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        processPIServices(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        processPIServices(obj);
    }

    private void processPIServices(MCRObject obj) {
        List<MCRPIRegistrationInfo> registered = MCRPIManager.getInstance().getRegistered(obj);

        final List<String> services = registered.stream().map(MCRPIRegistrationInfo::getService)
            .collect(Collectors.toList());

        //collect all services, which want to create new PIs and are allowed to do so
        //this avoids cases  where one service does modifications 
        //which could change the result of the filter method for other services
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<MCRPIJobService> listOfServicesWithCreatablePIs = MCRPIServiceManager.getInstance()
            .getAutoCreationList().stream()
            .filter(Predicate.not(s -> services.contains(s.getServiceID())))
            .filter(s -> s.getCreationPredicate().test(obj))
            .collect(Collectors.toList());

        listOfServicesWithCreatablePIs
            .forEach((serviceToRegister) -> {
                try {
                    serviceToRegister.register(obj, "", false);
                } catch (MCRAccessException | MCRActiveLinkException | MCRPersistentIdentifierException
                    | ExecutionException | InterruptedException e) {
                    throw new MCRException("Error while register pi for object " + obj.getId().toString(), e);
                }
            });
    }
}
