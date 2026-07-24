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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.MCRInstanceName;

/**
 * A {@link MCRClassPropertyExtractor} is a {@link MCRPropertyExtractor} that uses
 * {@link MCRInstanceConfiguration#instantiate()} to extract nested configured instances.
 */
final class MCRInstanceExtractor implements MCRValueExtractor<Object> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Class<?> superClass;

    MCRInstanceExtractor(Class<?> superClass) {
        this.superClass = superClass;
    }

    @Override
    public Object toValue(MCRSourceContext context, Map<String, String> properties,
        Map<String, String> fullProperties) {

        MCRInstanceConfiguration<?> configuration = MCRInstanceConfiguration.ofComponents(
            superClass, MCRInstanceName.of(context.property()), properties, fullProperties);

        if (!configuration.instantiatable()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[CLEAN-UP] Ignoring {}, configured in {} (and sub-properties thereof), " +
                    "because {}.Class is missing or empty)", context.description(), context.property(),
                    context.property());
            }
            return null;
        }

        if (!superClass.isAssignableFrom(configuration.valueClass())) {
            throw context.classIncompatibilityException(superClass, configuration.valueClass());
        }

        return configuration.instantiate();

    }

}
