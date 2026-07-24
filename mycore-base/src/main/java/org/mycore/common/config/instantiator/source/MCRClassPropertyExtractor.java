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

import org.mycore.common.MCRClassTools;

/**
 * A {@link MCRClassPropertyExtractor} is a {@link MCRPropertyExtractor} that uses
 * {@link MCRClassTools#forName(String)} to extract {@link Class} values.
 */
final class MCRClassPropertyExtractor implements MCRValueExtractor<Class<?>> {

    private final Class<?> superClass;

    MCRClassPropertyExtractor(Class<?> valueClass) {
        this.superClass = valueClass;
    }

    @Override
    public Class<?> toValue(MCRSourceContext context, Map<String, String> properties,
        Map<String, String> fullProperties) {

        String className = properties.get("");

        if (className == null) {
            return null;
        }

        try {
            Class<?> configuredClass = MCRClassTools.forName(className);
            if (!superClass.isAssignableFrom(configuredClass)) {
                throw context.classIncompatibilityException(superClass, configuredClass);
            }
            return configuredClass;
        } catch (ClassNotFoundException exception) {
            throw context.classLoadingException(className, exception);
        }

    }

}
