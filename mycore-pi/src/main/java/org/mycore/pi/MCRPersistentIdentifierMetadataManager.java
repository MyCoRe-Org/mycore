package org.mycore.pi;

import static org.mycore.pi.MCRPIRegistrationService.METADATA_MANAGER_CONFIG_PREFIX;
import static org.mycore.pi.MCRPIRegistrationService.METADATA_MANAGER_DEPRECATED_CONFIG_PREFIX;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Should be able to insert/remove DOI, URN or other identifiers to metadata and check if they already have a
 * Identifier of type T
 * @param <T>
 */
public abstract class MCRPersistentIdentifierMetadataManager<T extends MCRPersistentIdentifier> {

    private static final Logger LOGGER = LogManager.getLogger();

    private String metadataManagerID;

    public MCRPersistentIdentifierMetadataManager(String metadataManagerID) {
        this.metadataManagerID = metadataManagerID;
    }

    protected final Map<String, String> getProperties() {
        Map<String, String> deprecatedProperties = getPropertiesWithPrefix(METADATA_MANAGER_DEPRECATED_CONFIG_PREFIX);
        Map<String, String> properties = getPropertiesWithPrefix(METADATA_MANAGER_CONFIG_PREFIX);

        deprecatedProperties.forEach((k, v) -> {
            properties.computeIfAbsent(k, (k2) -> {
                LOGGER.warn("You should rename {}{}.{} to {}{}.{}", METADATA_MANAGER_DEPRECATED_CONFIG_PREFIX,
                    metadataManagerID, k, METADATA_MANAGER_CONFIG_PREFIX, metadataManagerID, k);
                return v;
            });
        });

        return properties;
    }

    protected final Map<String, String> getPropertiesWithPrefix(String configPrefix) {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(configPrefix + metadataManagerID + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().stream().forEach(key -> {
            String newKey = key.substring(configPrefix.length() + metadataManagerID.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    public abstract void insertIdentifier(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException;

    public abstract void removeIdentifier(T identifier, MCRBase obj, String additional);

    public abstract Optional<MCRPersistentIdentifier> getIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException;
}
