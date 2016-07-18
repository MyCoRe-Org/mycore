package org.mycore.pi;


import java.util.HashMap;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import static org.mycore.pi.MCRPIRegistrationService.INSCRIBER_CONFIG_PREFIX;

/**
 * Should be able to insert/remove DOI, URN or other identifiers to metadata and check if they already have a Identifier of type T
 * @param <T>
 */
public abstract class MCRPersistentIdentifierInscriber<T extends MCRPersistentIdentifier> {

    public MCRPersistentIdentifierInscriber(String inscriberID) {
        this.inscriberID = inscriberID;
    }

    private String inscriberID;


    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
                .getPropertiesMap(INSCRIBER_CONFIG_PREFIX + inscriberID + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().stream().forEach(key -> {
            String newKey = key.substring(INSCRIBER_CONFIG_PREFIX.length() + inscriberID.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    public abstract void insertIdentifier(T identifier, MCRBase obj, String additional) throws MCRPersistentIdentifierException;
    public abstract void removeIdentifier(T identifier, MCRBase obj, String additional);
    public abstract boolean hasIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException;
}
