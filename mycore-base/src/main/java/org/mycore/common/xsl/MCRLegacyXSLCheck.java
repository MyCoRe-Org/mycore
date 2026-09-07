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

import java.util.Map;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

import jakarta.servlet.ServletContext;

/**
 * Rejects legacy XSL factory properties that would be ignored in favor of a factory ID property.
 * <p>
 * This startup check is temporary compatibility support. Remove this class and its
 * {@code MCR.Startup.Class} registration when the deprecated class-based transformer APIs
 * and properties are removed.
 */
public class MCRLegacyXSLCheck implements AutoExecutable {

    private static final String LEGACY_SUFFIX = ".TransformerFactoryClass";

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
        Map<String, String> properties = MCRConfiguration2.getAllPropertiesMap();
        String conflicts = properties.keySet().stream()
            .filter(key -> key.endsWith(LEGACY_SUFFIX))
            .filter(key -> properties.containsKey(currentProperty(key)))
            .sorted()
            .map(key -> migrationMessage(key, properties))
            .collect(Collectors.joining("\n\n"));
        if (!conflicts.isEmpty()) {
            throw new MCRConfigurationException("Conflicting legacy XSL transformer factory properties. "
                + "Migrate the following settings before starting the application:\n\n" + conflicts
                + "\n\nFactory IDs are configured below '" + MCRTransformerFactoryRegistry.CONFIGURATION_PREFIX
                + ".<ID>.Class'. Choose the ID matching the intended implementation; "
                + "do not simply rename a class-valued property.");
        }
    }

    private static String currentProperty(String legacyProperty) {
        return legacyProperty.substring(0, legacyProperty.length() - "Class".length());
    }

    private static String migrationMessage(String legacyProperty, Map<String, String> properties) {
        String property = currentProperty(legacyProperty);
        return legacyProperty + "=" + properties.get(legacyProperty) + "\n"
            + property + "=" + properties.get(property) + "\n"
            + "Set '" + property + "' to the intended factory ID and remove '" + legacyProperty + "'.";
    }

}
