package org.mycore.pi;

import static org.mycore.pi.MCRPIRegistrationService.GENERATOR_CONFIG_PREFIX;

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRPersistentIdentifierGenerator<T extends MCRPersistentIdentifier> {

    public MCRPersistentIdentifierGenerator(String generatorID) {
        this.generatorID = generatorID;
    }

    private String generatorID;

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(GENERATOR_CONFIG_PREFIX + generatorID + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().stream().forEach(key -> {
            String newKey = key.substring(GENERATOR_CONFIG_PREFIX.length() + generatorID.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    public abstract T generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException;

}
