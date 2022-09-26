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

package org.mycore.oai.classmapping;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

/**
 * This class maps MyCoRe classification names to set names and vice versa
 * in OAI implementation.
 * 
 * The mapping itself is stored in properties
 * e.g. MCR.OAIDataProvider.OAI2.MapSetToClassification.doc-type=diniPublType
 * 
 * @author Robert Stephan
 * 
 * @version $Revision$ $Date$
 */
public class MCRClassificationAndSetMapper {
    private static final String PROP_CLASS_SUFFIX = ".Classification";

    private static final String PROP_SETS_PREFIX = "Sets.";

    private static String PROP_SUFFIX = "MapSetToClassification.";

    /**
     * maps a classification name to an OAI set name
     * @param prefix - the properties prefix of the OAIAdapter
     * @param classid - the classification name
     */
    public static String mapClassificationToSet(String prefix, String classid) {
        String propPrefix = prefix + PROP_SETS_PREFIX;
        return MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(p -> p.getKey().startsWith(propPrefix))
            .filter(e -> e.getKey().endsWith(PROP_CLASS_SUFFIX))
            .filter(e -> e.getValue().equals(classid))
            .findFirst()
            .map(Map.Entry::getKey)
            .map(key -> key.substring(propPrefix.length(), key.length() - PROP_CLASS_SUFFIX.length()))
            .orElseGet(() -> getSetNameFromDeprecatedProperty(prefix, classid));
    }

    private static String getSetNameFromDeprecatedProperty(String prefix, String classid) {
        return MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(p -> p.getKey().startsWith(prefix + PROP_SUFFIX))
            .filter(entry -> entry.getValue().equals(classid))
            .peek(e -> LogManager.getLogger()
                .warn("Please rename deprecated property '{}' and use '{}' suffix.", e.getKey(),
                    PROP_CLASS_SUFFIX))
            .findFirst()
            .map(Map.Entry::getKey)
            .map(key -> key.substring(key.lastIndexOf(".") + 1))
            .orElse(classid);
    }

    /**
     * maps an OAI set name to a classification name
     * @param prefix - the property prefix for the OAIAdapter
     * @param setid - the set name
     */
    public static String mapSetToClassification(String prefix, String setid) {
        String classProperty = prefix + PROP_SETS_PREFIX + setid + PROP_CLASS_SUFFIX;
        try {
            return MCRConfiguration2.getStringOrThrow(classProperty);
        } catch (MCRConfigurationException mce) {
            try {
                String legacyProperty = prefix + PROP_SUFFIX + setid;
                String legacy = MCRConfiguration2.getStringOrThrow(legacyProperty);
                LogManager.getLogger().warn("Please rename deprecated property '{}' to '{}'.", legacyProperty,
                    classProperty);
                return legacy;
            } catch (MCRConfigurationException e) {
                // use the given value of setid
                return setid;
            }
        }
    }
}
