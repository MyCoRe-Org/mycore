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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

public class MCRPIServiceManager {

    public static final String REGISTRATION_SERVICE_CONFIG_PREFIX = "MCR.PI.Service.";

    private final Map<String, MCRPIService> serviceCache = new ConcurrentHashMap<>();

    public static MCRPIServiceManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public List<String> getServiceIDList() {
        return MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(p -> p.getKey().startsWith(REGISTRATION_SERVICE_CONFIG_PREFIX))
            .map(Map.Entry::getKey)
            .map(s -> s.substring(REGISTRATION_SERVICE_CONFIG_PREFIX.length()))
            .filter(s -> !s.contains("."))
            .collect(Collectors.toList());
    }

    public List<MCRPIService> getServiceList() {
        return getServiceIDList()
            .stream()
            .map(this::getRegistrationService)
            .collect(Collectors.toList());
    }

    public List<MCRPIJobService> getAutoCreationList() {
        return getServiceList()
            .stream()
            .filter(service -> service instanceof MCRPIJobService)
            .map(MCRPIJobService.class::cast)
            .filter(service -> MCRConfiguration2
                .getString(REGISTRATION_SERVICE_CONFIG_PREFIX + service.getServiceID() + "." +
                    CREATION_PREDICATE)
                .isPresent())
            .collect(Collectors.toList());
    }

    public <T extends MCRPersistentIdentifier> MCRPIService<T> getRegistrationService(
        String id) {

        final MCRPIService mcrpiService = serviceCache.computeIfAbsent(id, (registrationServiceID) -> {
            String propertyName = REGISTRATION_SERVICE_CONFIG_PREFIX + registrationServiceID;
            Class<? extends MCRPIService<T>> piClass = MCRConfiguration2.<MCRPIService<T>>getClass(propertyName)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(propertyName));

            try {
                Constructor<? extends MCRPIService<T>> constructor = piClass.getConstructor(String.class);
                return constructor.newInstance(registrationServiceID);
            } catch (NoSuchMethodException e) {
                throw new MCRConfigurationException("The property : " + propertyName
                    + " points to existing class, but without string constructor(serviceid)!", e);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new MCRException("Cant initialize class the class defined in: " + propertyName, e);
            }
        });
        return mcrpiService;
    }

    private static class InstanceHolder {
        private static final MCRPIServiceManager INSTANCE = new MCRPIServiceManager();
    }

}
