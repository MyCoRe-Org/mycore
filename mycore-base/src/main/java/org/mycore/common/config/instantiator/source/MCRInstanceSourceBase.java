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

import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;

abstract class MCRInstanceSourceBase<Result> extends MCRSourceBase<Result> {

    protected final Object createInstance(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        MCRSentinel sentinel) {

        if (rejectedBySentinel(sentinel, context, configuration.properties(), "")) {
            return null;
        }

        if (!configuration.instantiatable()) {
            if (logger.isInfoEnabled()) {
                logger.info("[CLEAN-UP] Ignoring {} {} and all sub-properties (no or empty class name)",
                    context.description(), context.property());
            }
            return null;
        }

        Object instance = configuration.instantiate();

        if (!configuration.valueClass().isAssignableFrom(instance.getClass())) {
            throw context.incompatibilityException(configuration.valueClass(), instance.getClass());
        }

        return instance;

    }

}
