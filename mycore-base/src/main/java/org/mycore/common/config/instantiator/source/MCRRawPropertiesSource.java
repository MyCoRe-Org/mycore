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

import java.util.Map;
import java.util.Set;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRRawProperties;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.MCRInstantiatorUtils;
import org.mycore.common.config.instantiator.MCRProperTree;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRRawPropertiesSource} is a {@link MCRRawProperties} that interprets a {@link MCRSource}.
 */
final class MCRRawPropertiesSource implements MCRSource {

    private final MCRRawProperties annotation;

    private final PatternHandler patternHandler;

    MCRRawPropertiesSource(MCRRawProperties annotation) {
        this.annotation = annotation;
        this.patternHandler = toPatternHandler(annotation.namePattern());
    }

    private static PatternHandler toPatternHandler(String namePattern) {
        if (namePattern.equals("*")) {
            return new RootPatternHandler();
        } else if (namePattern.endsWith(".*")) {
            return new NestedPatternHandler(namePattern);
        } else {
            throw new MCRConfigurationException("Unsupported name pattern:" + namePattern);
        }
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
    public Set<MCRTarget.Type> allowedTargetTypes() {
        return MCRTarget.Types.ALL;
    }

    @Override
    public Class<?> valueClass() {
        return Map.class;
    }

    @Override
    public Map<String, String> get(MCRInstanceConfiguration<?> configuration, MCRTarget target) {

        MCRProperTree properties = annotation.absolute() ? configuration.fullProperties() : configuration.properties();
        Map<String, String> rawProperties = patternHandler.findTree(properties).toProperties();
        patternHandler.prepareRawProperties(rawProperties);

        if (rawProperties.isEmpty() && annotation.required()) {
            String property;
            String description;
            if (annotation.absolute()) {
                property = annotation.namePattern();
                description = "absolute raw property map";
            } else {
                property = configuration.name().canonical() + "." + annotation.namePattern();
                description = "raw property map";
            }
            throw MCRInstantiatorUtils.emptyRawException(property, target, description);
        }

        return rawProperties;

    }

    private sealed interface PatternHandler permits RootPatternHandler, NestedPatternHandler {

        MCRProperTree findTree(MCRProperTree properties);

        void prepareRawProperties(Map<String, String> rawProperties);

    }

    private static final class RootPatternHandler implements PatternHandler {

        @Override
        public MCRProperTree findTree(MCRProperTree properties) {
            return properties;
        }

        @Override
        public void prepareRawProperties(Map<String, String> rawProperties) {
            rawProperties.remove(MCRInstanceConfiguration.CLASS_KEY);
        }

    }

    private static final class NestedPatternHandler implements PatternHandler {

        private final String key;

        private NestedPatternHandler(String namePattern) {
            this.key = namePattern.substring(0, namePattern.length() - 2);
        }

        @Override
        public MCRProperTree findTree(MCRProperTree properties) {
            return properties.deeplyNested(key);
        }

        @Override
        public void prepareRawProperties(Map<String, String> rawProperties) {
        }

    }

}
