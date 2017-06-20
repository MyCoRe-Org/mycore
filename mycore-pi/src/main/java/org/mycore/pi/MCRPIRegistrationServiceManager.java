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
        String className = MCRConfiguration.instance().getString(propertyName);

        try {
            Constructor<?> constructor = Class.forName(className).getConstructor(String.class);

            return (MCRPIRegistrationService<T>) constructor.newInstance(registrationServiceID);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException("The property : " + propertyName + " points to not existing class!", e);
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
