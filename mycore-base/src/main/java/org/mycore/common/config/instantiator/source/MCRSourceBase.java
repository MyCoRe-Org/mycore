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

import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.incompatibilityException;

import java.lang.reflect.Modifier;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRInstanceConfiguration;
import org.mycore.common.config.MCRInstanceConfiguration.Option;
import org.mycore.common.config.MCRInstanceConfiguration.Options;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstantiator;
import org.mycore.common.config.instantiator.target.MCRTarget;
import org.mycore.common.config.instantiator.target.MCRTargetType;

/**
 * {@link MCRSourceBase} is a base implementation of {@link MCRSource}.
 */
@SuppressWarnings({ "PMD.MCR.Singleton.ClassModifiers", "PMD.MCR.Singleton.PrivateConstructor",
    "PMD.MCR.Singleton.NonPrivateConstructors", "PMD.MCR.Singleton.MethodModifiers",
    "PMD.MCR.Singleton.MethodReturnType", "PMD.SingletonClassReturningNewInstance" })
abstract class MCRSourceBase implements MCRSource {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    public Set<MCRTargetType> allowedTargetTypes() {
        return MCRTargetType.ALL;
    }

    protected Object getInstance(String property, MCRTarget target, Class<?> valueClass,
        MCRInstanceConfiguration nestedConfiguration, MCRSentinel sentinel, String description) {

        Set<Option> options = options(valueClass);
        boolean implicitValueClass = Modifier.isFinal(valueClass.getModifiers());

        if (sentinel.enabled()) {
            boolean sentinelValue = sentinel.defaultValue();
            String configuredSentinelValue = nestedConfiguration.properties().remove(sentinel.name());
            if (configuredSentinelValue != null) {
                sentinelValue = Boolean.parseBoolean(configuredSentinelValue);
            }
            if (sentinelValue == sentinel.rejectionValue()) {
                if (logger.isInfoEnabled()) {
                    logger.info("[SENTINEL] Ignoring {} {} and all sup-properties", description, property);
                }
                return null;
            }
        }

        String className = nestedConfiguration.className();
        if (className == null && !implicitValueClass) {
            if (logger.isInfoEnabled()) {
                logger.info("[CLEAN-UP] Ignoring {} {} and all sup-properties (no class name)",
                    description, property);
            }
            return null;
        } else if (className != null && className.isBlank()) {
            if (logger.isInfoEnabled()) {
                logger.info("[CLEAN-UP] Ignoring {} {} and all sup-properties (empty class name)",
                    description, property);
            }
            return null;
        }

        Object instance = MCRInstantiator.getInstance(valueClass, nestedConfiguration, options);
        if (!valueClass.isAssignableFrom(instance.getClass())) {
            throw incompatibilityException(property, target, valueClass, instance);
        }

        return instance;

    }

    protected final Set<Option> options(Class<?> valueClass) {
        if (Modifier.isFinal(valueClass.getModifiers())) {
            return Options.IMPLICIT;
        } else {
            return Options.NONE;
        }
    }

}
