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

import javax.xml.transform.TransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Selects a configured transformer factory ID and migrates legacy class-valued properties.
 */
public final class MCRTransformerFactorySelector {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEFAULT_FACTORY_PROPERTY = "MCR.LayoutService.TransformerFactory";

    private static final String LEGACY_DEFAULT_FACTORY_PROPERTY = DEFAULT_FACTORY_PROPERTY + "Class";

    private MCRTransformerFactorySelector() {
    }

    /**
     * Resolves the current default from {@code MCR.LayoutService.TransformerFactory} or the
     * legacy {@code MCR.LayoutService.TransformerFactoryClass} property.
     *
     * @return current default factory ID
     * @throws MCRConfigurationException if neither property is configured
     */
    public static String getDefaultFactoryId() {
        return getConfiguredFactoryId(DEFAULT_FACTORY_PROPERTY, LEGACY_DEFAULT_FACTORY_PROPERTY)
            .orElseThrow(() -> new MCRConfigurationException("Missing required configuration property '"
                + DEFAULT_FACTORY_PROPERTY + "'. Configure a transformer factory ID."));
    }

    /**
     * Returns the ID from the current property, or maps the legacy factory class to its configured ID.
     *
     * @param property current ID-valued property
     * @param legacyProperty legacy class-valued property
     * @param defaultId ID used when neither property is configured
     * @return selected configured factory ID
     */
    public static String getFactoryId(String property, String legacyProperty, String defaultId) {
        return getConfiguredFactoryId(property, legacyProperty).orElse(defaultId);
    }

    private static Optional<String> getConfiguredFactoryId(String property, String legacyProperty) {
        return MCRConfiguration2.getString(property).map(MCRTransformerFactorySelector::getFactoryId)
            .or(() -> MCRConfiguration2
                .<TransformerFactory>getClass(legacyProperty)
                .map(factoryClass -> migrateLegacyProperty(property, legacyProperty, factoryClass)));
    }

    public static String getFactoryId(String factoryIdOrClassName) {
        if (factoryIdOrClassName.indexOf('.') == -1) {
            return factoryIdOrClassName;
        }
        try {
            Class<?> factoryClass = MCRClassTools.forName(factoryIdOrClassName);
            if (TransformerFactory.class.isAssignableFrom(factoryClass)) {
                LOGGER.warn("Transformer factory class value '{}' is deprecated. Configure and reference a factory "
                    + "ID instead.", factoryIdOrClassName);
                return MCRLegacyTransformerFactorySupport
                    .getFactoryId(factoryClass.asSubclass(TransformerFactory.class));
            }
        } catch (ClassNotFoundException e) {
            // Not a class name, so handle it as a factory ID.
        }
        return factoryIdOrClassName;
    }

    /**
     * @deprecated configure and reference a factory ID instead of an implementation class
     */
    @Deprecated(forRemoval = true)
    public static String getFactoryId(Class<? extends TransformerFactory> factoryClass) {
        return MCRLegacyTransformerFactorySupport.getFactoryId(factoryClass);
    }

    private static String migrateLegacyProperty(String property, String legacyProperty,
        Class<? extends TransformerFactory> factoryClass) {
        String factoryId = MCRLegacyTransformerFactorySupport.getFactoryId(factoryClass);
        if (MCRLegacyTransformerFactorySupport.isLegacyFactoryId(factoryId)) {
            String factoryClassName = factoryClass.getName();
            LOGGER.warn("Configuration property '{}' is deprecated. Register {} below '{}' and replace the property "
                + "with a factory ID.", legacyProperty, factoryClassName,
                MCRTransformerFactoryRegistry.CONFIGURATION_PREFIX);
        } else {
            LOGGER.warn("Configuration property '{}' is deprecated. Replace it with '{}={}'.", legacyProperty,
                property, factoryId);
        }
        return factoryId;
    }

}
