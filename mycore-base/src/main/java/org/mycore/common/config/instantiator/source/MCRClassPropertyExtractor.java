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

import org.mycore.common.MCRClassTools;

/**
 * A {@link MCRClassPropertyExtractor} is a {@link MCRPropertyExtractor} that uses
 * {@link MCRClassTools#forName(String)} to extract {@link Class} values.
 */
class MCRClassPropertyExtractor implements MCRValueExtractor<Class<?>> {

    private final Class<?> superClass;

    MCRClassPropertyExtractor(Class<?> superClass) {
        this.superClass = superClass;
    }

    @Override
    public Class<?> toValue(MCRSourceContext context, String value) {
        Class<?> configuredClass = loadConfiguredClass(context, value);
        if (!superClass.isAssignableFrom(configuredClass)) {
            throw context.incompatibilityException(superClass, configuredClass);
        }
        return configuredClass;
    }

    private Class<?> loadConfiguredClass(MCRSourceContext context, String value) {
        try {
            return MCRClassTools.forName(value);
        } catch (ClassNotFoundException | LinkageError cause) {
            throw context.configurationException("has a class (" + value + ") that could not be loaded", cause);
        }
    }

}
