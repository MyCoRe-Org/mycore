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

package org.mycore.common.config.instantiator.source;

import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.emptyNameException;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.missingException;

import java.util.Map;

import org.mycore.common.config.MCRInstanceConfiguration;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPropertySource} is a {@link MCRSourceBase} that interprets a {@link MCRProperty}.
 */
final class MCRPropertySource extends MCRSourceBase {

    private final MCRProperty annotation;

    MCRPropertySource(MCRProperty annotation) {
        this.annotation = annotation;
    }

    @Override
    public MCRSourceType type() {
        return MCRSourceType.PROPERTY;
    }

    @Override
    public Class<MCRProperty> annotationClass() {
        return MCRProperty.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Class<?> valueClass() {
        return String.class;
    }

    @Override
    public String get(MCRInstanceConfiguration configuration, MCRTarget target) {

        String name = annotation.name();
        if (name.isEmpty()) {
            throw emptyNameException(target);
        }

        String property;
        String description;
        String propertyValue;
        if (annotation.absolute()) {
            property = annotation.name();
            description = "absolute property";
            propertyValue = getPropertyValue(property, annotation.name(),
                configuration.fullProperties(), description);
        } else {
            property = configuration.name().canonical() + "." + annotation.name();
            description = "property";
            propertyValue = getPropertyValue(property, annotation.name(),
                configuration.properties(), description);
        }

        String defaultName = annotation.defaultName();
        if (propertyValue == null && !defaultName.isEmpty()) {

            property = defaultName;
            description = "default property";
            propertyValue = getPropertyValue(defaultName, defaultName,
                configuration.fullProperties(), description);

            if (propertyValue == null) {
                throw missingException(property, target, description);
            }

        }

        if (propertyValue == null && annotation.required()) {
            throw missingException(property, target, description);
        }

        return propertyValue;

    }

    private String getPropertyValue(String property, String prefix,
        Map<String, String> properties, String description) {

        MCRSentinel sentinel = annotation.sentinel();

        if (sentinel.enabled()) {
            boolean sentinelValue = sentinel.defaultValue();
            String configuredSentinelValue = properties.get(prefix + "." + sentinel.name());
            if (configuredSentinelValue != null) {
                sentinelValue = Boolean.parseBoolean(configuredSentinelValue);
            }
            if (sentinelValue == sentinel.rejectionValue()) {
                if (logger.isInfoEnabled()) {
                    logger.info("[SENTINEL] Ignoring {} {} and all sup-properties", description, property);
                }
                return null;
            }

        }

        return properties.get(prefix);

    }

}
