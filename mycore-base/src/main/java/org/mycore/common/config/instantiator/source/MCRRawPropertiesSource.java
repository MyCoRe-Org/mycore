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

import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.emptyRawException;

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRRawProperties;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRRawPropertiesSource} is a {@link MCRSourceBase} that interprets a {@link MCRSourceBase}.
 */
final class MCRRawPropertiesSource extends MCRSourceBase {

    private final MCRRawProperties annotation;

    private final String prefix;

    MCRRawPropertiesSource(MCRRawProperties annotation, MCRAnnotationProvider annotationProvider) {
        this.annotation = annotation;
        String namePattern = annotation.namePattern();
        if (namePattern.equals("*")) {
            this.prefix = "";
        } else if (namePattern.endsWith(".*")) {
            this.prefix = namePattern.substring(0, namePattern.length() - 1);
        } else {
            throw new MCRConfigurationException("Unsupported name pattern:" + annotation.namePattern());
        }
    }

    @Override
    public MCRSourceType type() {
        return MCRSourceType.RAW_PROPERTIES;
    }

    @Override
    public Class<MCRRawProperties> annotationClass() {
        return MCRRawProperties.class;
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

        Map<String, String> properties =
            annotation.absolute() ? configuration.fullProperties() : configuration.properties();

        Map<String, String> filteredProperties = new HashMap<>();
        properties.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                filteredProperties.put(key.substring(prefix.length()), value);
            }
        });

        if (filteredProperties.isEmpty() && annotation.required()) {
            String property;
            String description;
            if (annotation.absolute()) {
                property = annotation.namePattern();
                description = "absolute raw property map";
            } else {
                property = configuration.name().canonical() + "." + annotation.namePattern();
                description = "raw property map";
            }
            throw emptyRawException(property, target, description);
        }

        return filteredProperties;

    }

}
