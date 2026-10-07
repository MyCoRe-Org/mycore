/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.common.xsl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

import jakarta.servlet.ServletContext;

/**
 * Reports legacy class-valued properties that override an XSLT processor ID property.
 * <p>
 * A legacy class-valued property takes precedence over the corresponding ID-valued property, because MyCoRe
 * itself configures only ID-valued properties. This check lists all such overrides at startup, so they can be
 * migrated before the legacy properties are removed.
 * <p>
 * This startup check is temporary compatibility support. Remove this class and its
 * {@code MCR.Startup.Class} registration when the deprecated class-based transformer APIs
 * and properties are removed.
 */
public class MCRLegacyXSLCheck implements AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<String> LEGACY_SUFFIXES = List.of(".TransformerFactoryClass",
        ".TransformerFactory.Class");

    @Override
    public String getName() {
        return "Legacy XSL configuration check";
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        String overrides = getOverrides(MCRConfiguration2.getAllPropertiesMap());
        if (!overrides.isEmpty()) {
            LOGGER.warn("Deprecated class-valued properties override XSLT processor ID properties. "
                + "Migrate the following settings:\n\n{}\n\nXSLT processor IDs are configured under '{}<ID>.{}.Class'. "
                + "Choose the ID matching the intended implementation; do not simply rename a class-valued property.",
                overrides, MCRXSLTProcessorRegistry.ENTRIES_PROPERTY_PREFIX,
                MCRXSLTProcessorRegistry.FACTORY_KEY);
        }
    }

    static String getOverrides(Map<String, String> properties) {
        return properties.keySet().stream()
            .filter(key -> currentProperty(key) != null)
            .filter(key -> isSet(properties, key) && isSet(properties, currentProperty(key)))
            .sorted()
            .map(key -> migrationMessage(key, properties))
            .collect(Collectors.joining("\n\n"));
    }

    private static boolean isSet(Map<String, String> properties, String key) {
        String value = properties.get(key);
        return value != null && !value.isBlank();
    }

    private static String currentProperty(String legacyProperty) {
        return LEGACY_SUFFIXES.stream()
            .filter(legacyProperty::endsWith)
            .map(suffix -> legacyProperty.substring(0, legacyProperty.length() - suffix.length())
                + ".XSLTProcessor")
            .findFirst()
            .orElse(null);
    }

    private static String migrationMessage(String legacyProperty, Map<String, String> properties) {
        String property = currentProperty(legacyProperty);
        return legacyProperty + "=" + properties.get(legacyProperty) + "\n"
            + "  overrides " + property + "=" + properties.get(property) + "\n"
            + "  Set '" + property + "' to the intended XSLT processor ID and remove '" + legacyProperty + "'.";
    }

}
