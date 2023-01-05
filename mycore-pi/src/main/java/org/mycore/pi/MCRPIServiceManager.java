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

import static org.mycore.pi.MCRPIJobService.CREATION_PREDICATE;

import java.util.List;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;

public class MCRPIServiceManager {

    public static final String REGISTRATION_SERVICE_CONFIG_PREFIX = "MCR.PI.Service.";

    public static MCRPIServiceManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public List<String> getServiceIDList() {
        return MCRConfiguration2.getInstantiatablePropertyKeys(REGISTRATION_SERVICE_CONFIG_PREFIX)
            .map(s -> s.substring(MCRPIServiceManager.REGISTRATION_SERVICE_CONFIG_PREFIX.length()))
            .collect(Collectors.toList());
    }

    public List<MCRPIService<MCRPersistentIdentifier>> getServiceList() {
        return getServiceIDList()
            .stream()
            .map(this::getRegistrationService)
            .collect(Collectors.toList());
    }

    public List<MCRPIService<MCRPersistentIdentifier>> getAutoCreationList() {
        return getServiceList()
            .stream()
            .filter(service -> MCRConfiguration2
                .getString(REGISTRATION_SERVICE_CONFIG_PREFIX + service.getServiceID() + "." +
                    CREATION_PREDICATE)
                .isPresent())
            .collect(Collectors.toList());
    }

    public <T extends MCRPersistentIdentifier> MCRPIService<T> getRegistrationService(String id) {
        return MCRConfiguration2
            .<MCRPIService<T>>getSingleInstanceOf(MCRPIServiceManager.REGISTRATION_SERVICE_CONFIG_PREFIX + id).get();
    }

    private static class InstanceHolder {
        private static final MCRPIServiceManager INSTANCE = new MCRPIServiceManager();
    }

}
