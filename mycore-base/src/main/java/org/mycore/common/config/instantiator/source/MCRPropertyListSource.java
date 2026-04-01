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
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.orderedKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.MCRInstanceName;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPropertyListSource} is a {@link MCRSourceBase} that interprets a {@link MCRPropertyList}.
 */
final class MCRPropertyListSource extends MCRSourceBase {

    private final MCRPropertyList annotation;

    MCRPropertyListSource(MCRPropertyList annotation) {
        this.annotation = annotation;
    }

    @Override
    public MCRSourceType type() {
        return MCRSourceType.PROPERTY_LIST;
    }

    @Override
    public Class<MCRPropertyList> annotationClass() {
        return MCRPropertyList.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Class<?> valueClass() {
        return List.class;
    }

    @Override
    public List<String> get(MCRInstanceConfiguration<?> configuration, MCRTarget target) {

        Map<String, String> fullProperties = configuration.fullProperties();

        String property;
        String description;
        List<String> propertyList;
        if (annotation.absolute()) {
            property = annotation.name();
            description = "absolute property list";
            Map<String, String> properties = fullProperties;
            propertyList = getPropertyList(property, annotation.name(), ".", target, properties, description);
        } else {
            if (annotation.name().isEmpty()) {
                property = configuration.name().canonical();
                description = "property list";
                Map<String, String> properties = configuration.properties();
                propertyList = getPropertyList(property, "", "", target, properties, description);
            } else {
                property = configuration.name().canonical() + "." + annotation.name();
                description = "property list";
                Map<String, String> properties = configuration.properties();
                propertyList = getPropertyList(property, annotation.name(), ".", target, properties, description);
            }
        }

        String defaultName = annotation.defaultName();
        if (propertyList == null && !defaultName.isEmpty()) {

            property = defaultName;
            description = "default property list";
            propertyList = getPropertyList(defaultName, defaultName, ".", target, fullProperties, description);

            if (propertyList == null || (propertyList.isEmpty() && annotation.required())) {
                throw emptyException(property, target, description);
            }
        }

        if ((propertyList == null || propertyList.isEmpty()) && annotation.required()) {
            throw emptyException(property, target, description);
        }

        return propertyList == null ? new ArrayList<>() : propertyList;

    }

    private List<String> getPropertyList(String property, String prefix, String delimiter,
        MCRTarget target, Map<String, String> properties, String description) {

        AtomicBoolean hasRelevantProperty = new AtomicBoolean(false);
        MCRSentinel sentinel = annotation.sentinel();

        Map<String, String> rawPropertyMap = new HashMap<>();
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

        List<String> headPropertyList = new ArrayList<>(0);
        List<String> tailPropertyList = new ArrayList<>(rawPropertyMap.size());

        List<String> keyList = orderedKeys(property, target, rawPropertyMap, description);
        for (String key : keyList) {
            String value = rawPropertyMap.get(key);
            if (sentinel.enabled()) {
                boolean sentinelValue = sentinel.defaultValue();
                String configuredSentinelValue = properties.get(keyPrefix + key + "." + sentinel.name());
                if (configuredSentinelValue != null) {
                    sentinelValue = Boolean.parseBoolean(configuredSentinelValue);
                }
                if (sentinelValue == sentinel.rejectionValue()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("[SENTINEL] Ignoring {} element {}.{} and all sup-properties",
                            description, property, key);
                    }
                    continue;
                }
            }

            if (key.charAt(0) == '-') {
                headPropertyList.add(value);
            } else {
                tailPropertyList.add(value);
            }

        }

        List<String> shortFormList = List.of();
        String shortFormProperty = properties.get(prefix);
        if (shortFormProperty != null) {
            hasRelevantProperty.set(true);
            shortFormList = parseShortFormList(shortFormProperty);
        }

        int totalSize = headPropertyList.size() + shortFormList.size() + tailPropertyList.size();
        List<String> fullPropertyList = new ArrayList<>(totalSize);
        fullPropertyList.addAll(headPropertyList);
        fullPropertyList.addAll(shortFormList);
        fullPropertyList.addAll(tailPropertyList);

        return hasRelevantProperty.get() ? fullPropertyList : null;

    }

    private List<String> parseShortFormList(String value) {
        return MCRConfiguration2.splitValue(value).toList();
    }

}
