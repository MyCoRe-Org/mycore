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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

public class MCRPIRegistrationServiceManager {

    public static final String REGISTRATION_SERVICE_CONFIG_PREFIX = "MCR.PI.Registration.";

    public static MCRPIRegistrationServiceManager getInstance() {
        return InstanceHolder.instance;
    }

    public List<String> getServiceIDList() {
        return MCRConfiguration.instance()
            .getPropertiesMap(REGISTRATION_SERVICE_CONFIG_PREFIX)
            .keySet()
            .stream()
            .map(s -> s.substring(REGISTRATION_SERVICE_CONFIG_PREFIX.length()))
            .collect(Collectors.toList());
    }

    public List<MCRPIRegistrationService> getServiceList() {
        return getServiceIDList()
            .stream()
            .map(this::getRegistrationService)
            .collect(Collectors.toList());
    }

    public <T extends MCRPersistentIdentifier> MCRPIRegistrationService<T> getRegistrationService(
        String registrationServiceID) {
        String propertyName = REGISTRATION_SERVICE_CONFIG_PREFIX + registrationServiceID;
        Class<? extends MCRPIRegistrationService<T>> piClass = MCRConfiguration.instance().getClass(propertyName);

        try {
            Constructor<? extends MCRPIRegistrationService<T>> constructor = piClass.getConstructor(String.class);

            return constructor.newInstance(registrationServiceID);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException("The property : " + propertyName
                + " points to existing class, but without string constructor(serviceid)!", e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException("Cant initialize class the class defined in: " + propertyName, e);
        }
    }

    private static class InstanceHolder {
        private static final MCRPIRegistrationServiceManager instance = new MCRPIRegistrationServiceManager();
    }

}
