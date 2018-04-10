package org.mycore.pi;

import static org.mycore.pi.MCRPIRegistrationService.GENERATOR_CONFIG_PREFIX;

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRPersistentIdentifierGenerator<T extends MCRPersistentIdentifier> {

    public MCRPersistentIdentifierGenerator(String generatorID) {
        this.generatorID = generatorID;
    }

    private String generatorID;

    public String getGeneratorID() {
        return generatorID;
    }

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(GENERATOR_CONFIG_PREFIX + generatorID + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().forEach(key -> {
            String newKey = key.substring(GENERATOR_CONFIG_PREFIX.length() + generatorID.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    public abstract T generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException;

    /**
     * checks if the property exists and throws a exception if not.
     * @param propertyName to check
     * @throws MCRConfigurationException if property does not exist
     */
    protected void checkPropertyExists(final String propertyName) throws MCRConfigurationException {
        if(!getProperties().containsKey(propertyName)){
            throw new MCRConfigurationException("Missing property " + GENERATOR_CONFIG_PREFIX + getGeneratorID() + "." + propertyName);
        }
    }
}
