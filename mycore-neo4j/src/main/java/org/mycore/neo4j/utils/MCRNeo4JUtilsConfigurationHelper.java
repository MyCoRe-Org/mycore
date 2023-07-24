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

package org.mycore.neo4j.utils;

import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CONFIG_PREFIX;

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration2;

/**
 * The MCRNeo4JUtilsConfigurationHelper class provides utility methods for managing configuration settings related to
 * Neo4j in MyCoRe. It maintains a map of attribute paths for different MCR object types.
 * <p>
 * Note: This class assumes the usage of MyCoRe and its specific configuration conventions. The attributePaths map
 * follows the format: Map<MCRObjectType, Map<property, path_to_element>>
 * <p>
 * Example usage: Map<String, String> configuration = MCRNeo4JUtilsConfigurationHelper.getConfiguration("ObjectType");
 * String idPath = configuration.get("id"); String descriptorPath = configuration.get("descriptor"); // Use the
 * configuration for further processing
 * <p>
 * Note: This class utilizes the MCRConfiguration2 class for retrieving configuration properties.
 *
 * @author Andreas Kluge
 * @author Jens Kupferschmidt
 */
public class MCRNeo4JUtilsConfigurationHelper {

    private MCRNeo4JUtilsConfigurationHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
    * A map of MyCoRe properties starts with MCR.Neo4J... Map<MCRObjectType, Map<property, path_to_element>>
    */
    static Map<String, Map<String, String>> attributePaths = new HashMap<>();

    /**
    * Retrieves the configuration for the specified MCR object type. Configured in the Style of
    * MCR.Neo4J.NodeAttribute.<MCRObjectType>.propertyName=Path/to/value
    *
    * @param type the MCR object type
    * @return a map of attribute paths for the specified object type
    */
    public static Map<String, String> getConfiguration(String type) {
        if (attributePaths.containsKey(type)) {
            return attributePaths.get(type);
        }

        Map<String, String> properties = MCRConfiguration2.getSubPropertiesMap(NEO4J_CONFIG_PREFIX
            + "NodeAttribute." + type + ".");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("id", "/mycoreobject/@ID");
        attributes.put("type", type);
        String desc = properties.get("descriptor");
        if (desc != null && desc.length() > 1) {
            attributes.put("descriptor", desc);
        } else {
            attributes.put("descriptor", "/mycoreobject/@label");
        }

        properties.forEach((k, v) -> {
            if (!k.startsWith("descriptor")) {
                attributes.put(k, v);
            }
        });
        attributePaths.put(type, attributes);
        return attributes;
    }

}
