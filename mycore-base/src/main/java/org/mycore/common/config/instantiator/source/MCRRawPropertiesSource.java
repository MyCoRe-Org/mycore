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
import java.util.function.Function;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.instantiator.MCRProperTree;
import org.mycore.common.config.annotation.MCRRawProperties;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.MCRInstantiatorUtils;
import org.mycore.common.config.instantiator.target.MCRTarget;

import com.google.common.base.Functions;

/**
 * A {@link MCRRawPropertiesSource} is a {@link MCRRawProperties} that interprets a {@link MCRSource}.
 */
final class MCRRawPropertiesSource implements MCRSource {

    private final MCRRawProperties annotation;

    private final Function<MCRProperTree, MCRProperTree> treeFinder;

    MCRRawPropertiesSource(MCRRawProperties annotation) {
        this.annotation = annotation;
        String namePattern = annotation.namePattern();
        if (namePattern.equals("*")) {
            this.treeFinder = Functions.identity();
        } else if (namePattern.endsWith(".*")) {
            this.treeFinder = tree -> tree.deeplyNested(namePattern.substring(0, namePattern.length() - 2));
        } else {
            throw new MCRConfigurationException("Unsupported name pattern:" + annotation.namePattern());
        }
    }

    @Override
    public Type type() {
        return Type.RAW_PROPERTIES;
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
        Map<String, String> rawProperties = treeFinder.apply(properties).toProperties();
        rawProperties.remove(MCRInstanceConfiguration.CLASS_KEY);

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

}
