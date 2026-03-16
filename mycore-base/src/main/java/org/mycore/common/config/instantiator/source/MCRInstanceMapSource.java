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
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.property;

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.config.MCRInstanceConfiguration;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRInstanceMapSource} is a {@link MCRSourceBase} that interprets a {@link MCRInstanceMap}.
 */
final class MCRInstanceMapSource extends MCRSourceBase {

    private final MCRInstanceMap annotation;

    MCRInstanceMapSource(MCRInstanceMap annotation) {
        this.annotation = annotation;
    }

    @Override
    public MCRSourceType type() {
        return MCRSourceType.INSTANCE_MAP;
    }

    @Override
    public Class<MCRInstanceMap> annotationClass() {
        return MCRInstanceMap.class;
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
    public Map<String, Object> get(MCRInstanceConfiguration configuration, MCRTarget target) {

        Map<String, Object> instanceMap = getInstanceMap(target, configuration);

        if (instanceMap.isEmpty() && annotation.required()) {
            throw emptyException(property(configuration, annotation.name()), target, "instance map");
        }

        return instanceMap;

    }

    private Map<String, Object> getInstanceMap(MCRTarget target, MCRInstanceConfiguration configuration) {

        Map<String, MCRInstanceConfiguration> nestedConfigurationMap =
            configuration.nestedConfigurationMap(annotation.name());

        Class<?> valueClass = annotation.valueClass();
        MCRSentinel sentinel = annotation.sentinel();

        Map<String, Object> instanceMap = new HashMap<>();
        for (String key : nestedConfigurationMap.keySet()) {

            MCRInstanceConfiguration nestedConfiguration = nestedConfigurationMap.get(key);
            String property = nestedConfiguration.name().canonical();

            Object instance = getInstance(property, target, valueClass, nestedConfiguration,
                sentinel, "instance map entry");

            if (instance != null) {
                instanceMap.put(key, instance);
            }

        }

        return instanceMap;

    }

}
