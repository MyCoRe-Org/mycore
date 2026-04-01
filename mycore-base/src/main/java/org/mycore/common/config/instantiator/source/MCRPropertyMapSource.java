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

import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.emptyException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPropertyMapSource} is a {@link MCRSourceBase} that interprets a {@link MCRPropertyMap}.
 */
final class MCRPropertyMapSource extends MCRSourceBase {

    private final MCRPropertyMap annotation;

    MCRPropertyMapSource(MCRPropertyMap annotation) {
        this.annotation = annotation;
    }

    @Override
    public MCRSourceType type() {
        return MCRSourceType.PROPERTY_MAP;
    }

    @Override
    public Class<MCRPropertyMap> annotationClass() {
        return MCRPropertyMap.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Class<?> valueClass() {
        return Map.class;
    }

    @Override
    public Map<String, String> get(MCRInstanceConfiguration<?> configuration, MCRTarget target) {

        Map<String, String> fullProperties = configuration.fullProperties();

        String property;
        String description;
        Map<String, String> propertyMap;
        if (annotation.absolute()) {
            property = annotation.name();
            description = "absolute property map";
            Map<String, String> properties = fullProperties;
            propertyMap = getPropertyMap(property, annotation.name(), ".", properties, description);
        } else {
            if (annotation.name().isEmpty()) {
                property = configuration.name().canonical();
                description = "property map";
                Map<String, String> properties = configuration.properties();
                propertyMap = getPropertyMap(property, "", "", properties, description);
            } else {
                property = configuration.name().canonical() + "." + annotation.name();
                description = "property map";
                Map<String, String> properties = configuration.properties();
                propertyMap = getPropertyMap(property, annotation.name(), ".", properties, description);
            }
        }

        String defaultName = annotation.defaultName();
        if (propertyMap == null && !defaultName.isEmpty()) {

            property = defaultName;
            description = "default property map";
            propertyMap = getPropertyMap(defaultName, defaultName, ".", fullProperties, description);

            if (propertyMap == null || (propertyMap.isEmpty() && annotation.required())) {
                throw emptyException(property, target, description);
            }

        }

        if ((propertyMap == null || propertyMap.isEmpty()) && annotation.required()) {
            throw emptyException(property, target, description);
        }

        return propertyMap == null ? new HashMap<>() : propertyMap;

    }

    private Map<String, String> getPropertyMap(String property, String prefix, String delimiter,
        Map<String, String> properties, String description) {

        AtomicBoolean hasRelevantProperty = new AtomicBoolean(false);
        MCRSentinel sentinel = annotation.sentinel();

        Map<String, String> shortFormMap = Map.of();
        String shortFormProperty = properties.get(prefix);
        if (shortFormProperty != null) {
            hasRelevantProperty.set(true);
            shortFormMap = parseShortFormMap(shortFormProperty);
        }

        Map<String, String> rawPropertyMap = new HashMap<>(shortFormMap);
        String keyPrefix = prefix + delimiter;
        int keyPrefixLength = keyPrefix.length();
        properties.forEach((key, value) -> {
            if (key.startsWith(keyPrefix) && !key.isEmpty()) {
                int index = key.indexOf('.', keyPrefixLength);
                if (index == -1) {
                    if (!value.isEmpty()) {
                        hasRelevantProperty.set(true);
                        rawPropertyMap.put(key.substring(keyPrefixLength), value);
                    }
                }
            }
        });

        Map<String, String> propertyMap = new HashMap<>();

        for (String key : rawPropertyMap.keySet()) {
            String value = rawPropertyMap.get(key);
            if (sentinel.enabled()) {
                boolean sentinelValue = sentinel.defaultValue();
                String configuredSentinelValue = properties.get(keyPrefix + key + "." + sentinel.name());
                if (configuredSentinelValue != null) {
                    sentinelValue = Boolean.parseBoolean(configuredSentinelValue);
                }
                if (sentinelValue == sentinel.rejectionValue()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("[SENTINEL] Ignoring {} entry {}.{} and all sub-properties",
                            description, property, key);
                    }
                    continue;
                }
            }

            propertyMap.put(key, value);

        }

        return hasRelevantProperty.get() ? propertyMap : null;

    }

    private Map<String, String> parseShortFormMap(String value) {
        return MCRConfiguration2.splitValue(value)
            .map(s -> s.split(":", 2))
            .filter(parts -> parts.length != 1)
            .filter(parts -> !parts[1].isBlank())
            .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()));
    }

}
