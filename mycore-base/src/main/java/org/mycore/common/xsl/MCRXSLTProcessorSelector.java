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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.TransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Selects a configured XSLT processor ID and migrates legacy class-valued properties.
 * <p>
 * A legacy class-valued property takes precedence over the corresponding ID-valued property. MyCoRe itself
 * configures only ID-valued properties, so a legacy property always comes from an application layer that is
 * meant to override the MyCoRe default.
 */
public final class MCRXSLTProcessorSelector {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEFAULT_PROCESSOR_PROPERTY = "MCR.LayoutService.XSLTProcessor";

    private static final String LEGACY_DEFAULT_PROCESSOR_PROPERTY = "MCR.LayoutService.TransformerFactoryClass";

    private static final String FACTORY_CLASS_PROPERTY_PATTERN =
        MCRXSLTProcessorRegistry.ENTRIES_PROPERTY_PREFIX + "<ID>." + MCRXSLTProcessorRegistry.FACTORY_KEY
            + ".Class";

    private static final Set<String> WARNED_LEGACY_PROPERTIES = ConcurrentHashMap.newKeySet();

    private MCRXSLTProcessorSelector() {
    }

    /**
     * Resolves the current default from the legacy {@code MCR.LayoutService.TransformerFactoryClass} property,
     * if configured, or from {@code MCR.LayoutService.XSLTProcessor}.
     *
     * @return current default XSLT processor ID
     * @throws MCRConfigurationException if neither property is configured
     */
    public static String getDefaultProcessorId() {
        return getConfiguredProcessorId(DEFAULT_PROCESSOR_PROPERTY, LEGACY_DEFAULT_PROCESSOR_PROPERTY)
            .orElseThrow(() -> new MCRConfigurationException("Missing required configuration property '"
                + DEFAULT_PROCESSOR_PROPERTY + "'. Configure an XSLT processor ID."));
    }

    /**
     * Maps the legacy factory class to its configured ID, if configured, or returns the ID from the current
     * property.
     *
     * @param property current ID-valued property
     * @param legacyProperty legacy class-valued property
     * @param defaultId ID used when neither property is configured
     * @return selected configured XSLT processor ID
     */
    public static String getProcessorId(String property, String legacyProperty, String defaultId) {
        return getConfiguredProcessorId(property, legacyProperty).orElse(defaultId);
    }

    private static Optional<String> getConfiguredProcessorId(String property, String legacyProperty) {
        return MCRConfiguration2.<TransformerFactory>getClass(legacyProperty)
            .map(factoryClass -> migrateLegacyProperty(property, legacyProperty, factoryClass))
            .or(() -> MCRConfiguration2.getString(property).map(MCRXSLTProcessorSelector::getProcessorId));
    }

    public static String getProcessorId(String processorIdOrClassName) {
        if (processorIdOrClassName.indexOf('.') == -1) {
            return processorIdOrClassName;
        }
        try {
            Class<?> factoryClass = MCRClassTools.forName(processorIdOrClassName);
            if (TransformerFactory.class.isAssignableFrom(factoryClass)) {
                LOGGER.warn("Transformer factory class value '{}' is deprecated. Configure and reference an XSLT "
                    + "processor ID instead.", processorIdOrClassName);
                return MCRLegacyXSLTProcessorProvider
                    .getProcessorId(factoryClass.asSubclass(TransformerFactory.class));
            }
        } catch (ClassNotFoundException e) {
            // Not a class name, so handle it as an XSLT processor ID.
        }
        return processorIdOrClassName;
    }

    /**
     * @deprecated configure and reference an XSLT processor ID instead of an implementation class
     */
    @Deprecated(forRemoval = true)
    public static String getProcessorId(Class<? extends TransformerFactory> factoryClass) {
        return MCRLegacyXSLTProcessorProvider.getProcessorId(factoryClass);
    }

    /**
     * Allows deprecation warnings for legacy properties to be logged again, e.g. after the configuration was
     * reloaded. Each legacy property is otherwise reported only once, because the default factory is resolved on
     * every call.
     */
    public static void resetLegacyWarnings() {
        WARNED_LEGACY_PROPERTIES.clear();
    }

    private static String migrateLegacyProperty(String property, String legacyProperty,
        Class<? extends TransformerFactory> factoryClass) {
        String processorId = MCRLegacyXSLTProcessorProvider.getProcessorId(factoryClass);
        if (!WARNED_LEGACY_PROPERTIES.add(legacyProperty)) {
            return processorId;
        }
        MCRConfiguration2.getString(property).ifPresent(ignoredId -> LOGGER.warn(
            "Configuration property '{}' overrides '{}={}'.", legacyProperty, property, ignoredId));
        if (MCRLegacyXSLTProcessorProvider.isLegacyProcessorId(processorId)) {
            String factoryClassName = factoryClass.getName();
            LOGGER.warn("Configuration property '{}' is deprecated. Register {} under '{}' and replace the property "
                + "with an XSLT processor ID.", legacyProperty, factoryClassName,
                FACTORY_CLASS_PROPERTY_PATTERN);
        } else {
            LOGGER.warn("Configuration property '{}' is deprecated. Replace it with '{}={}'.", legacyProperty,
                property, processorId);
        }
        return processorId;
    }

}
