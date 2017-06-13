/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.oai.classmapping;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration;
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

    private static MCRConfiguration config = MCRConfiguration.instance();

    private static String PROP_SUFFIX = "MapSetToClassification.";

    /**
     * maps a classification name to an OAI set name
     * @param prefix - the properties prefix of the OAIAdapter
     * @param classid - the classification name
     */
    public static String mapClassificationToSet(String prefix, String classid) {
        String propPrefix = prefix + PROP_SETS_PREFIX;
        Map<String, String> props = config.getPropertiesMap(propPrefix);
        return props.entrySet().stream()
            .filter(e -> e.getKey().endsWith(PROP_CLASS_SUFFIX))
            .filter(e -> e.getValue().equals(classid))
            .findFirst()
            .map(entry -> entry.getKey().substring(propPrefix.length(),
                entry.getKey().length() - PROP_CLASS_SUFFIX.length()))
            .orElseGet(() -> {
                Map<String, String> propsLegacy = config.getPropertiesMap(prefix + PROP_SUFFIX);
                return propsLegacy.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(classid))
                    .peek(e -> LogManager.getLogger()
                        .warn("Please rename deprecated property '{}' and use '{}' suffix.", e.getKey(),
                            PROP_CLASS_SUFFIX))
                    .findFirst()
                    .map(entry -> entry.getKey().substring(entry.getKey().lastIndexOf(".") + 1))
                    .orElse(classid);
            });
    }

    /**
     * maps an OAI set name to a classification name
     * @param prefix - the property prefix for the OAIAdapter
     * @param setid - the set name
     */
    public static String mapSetToClassification(String prefix, String setid) {
        String classProperty = prefix + PROP_SETS_PREFIX + setid + PROP_CLASS_SUFFIX;
        try {
            return config.getString(classProperty);
        } catch (MCRConfigurationException mce) {
            try {
                String legacyProperty = prefix + PROP_SUFFIX + setid;
                String legacy = config.getString(legacyProperty);
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
