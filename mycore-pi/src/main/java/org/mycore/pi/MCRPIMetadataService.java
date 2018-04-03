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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import static org.mycore.pi.MCRPIService.METADATA_SERVICE_CONFIG_PREFIX;

/**
 * Should be able to insert/remove DOI, URN or other identifiers to metadata and check if they already have a
 * Identifier of type T
 *
 * @param <T>
 */
public abstract class MCRPIMetadataService<T extends MCRPersistentIdentifier> {

    private static final Logger LOGGER = LogManager.getLogger();

    private String metadataManagerID;

    public MCRPIMetadataService(String metadataManagerID) {
        this.metadataManagerID = metadataManagerID;
    }

    protected final Map<String, String> getProperties() {
        return getPropertiesWithPrefix(METADATA_SERVICE_CONFIG_PREFIX);
    }

    protected final Map<String, String> getPropertiesWithPrefix(String configPrefix) {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(configPrefix + metadataManagerID + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().forEach(key -> {
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
