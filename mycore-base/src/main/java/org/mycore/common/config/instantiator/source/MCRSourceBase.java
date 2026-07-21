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
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.MCRInstantiatorUtils;
import org.mycore.common.config.instantiator.target.MCRTarget;

abstract class MCRSourceBase<Result> implements MCRSource {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    @SuppressWarnings("PMD.NPathComplexity")
    public final Result get(MCRInstanceConfiguration<?> configuration, MCRTarget target) {

        String name = name();
        if (name.isEmpty() && !allowsEmptyName()) {
            throw MCRInstantiatorUtils.emptyNameException(target);
        }

        MCRSourceContext context;
        Result result;

        if (absolute()) {
            context = new MCRSourceContext(target, name, "absolute " + description());
            result = getResult(context, configuration, configuration.fullProperties(), name);
        } else {
            if (name.isEmpty()) {
                context = new MCRSourceContext(target, configuration.name().canonical(), description());
                result = getResult(context, configuration, configuration.properties(), "");
            } else {
                context = new MCRSourceContext(target, configuration.name().canonical() + "." + name, description());
                result = getResult(context, configuration, configuration.properties(), name);
            }
        }

        String defaultName = defaultName();
        if (result == null && !defaultName.isEmpty()) {
            context = new MCRSourceContext(target, defaultName, "default " + description());
            result = getResult(context, configuration, configuration.fullProperties(), defaultName);
            if (result == null || (isMissingResult(result) && required())) {
                throw missingException(context);
            }
        }

        if ((result == null || isMissingResult(result)) && required()) {
            throw missingException(context);
        }

        return result == null ? nullResultReplacement() : result;

    }

    protected abstract String description();

    protected abstract String name();

    protected abstract boolean allowsEmptyName();

    protected abstract boolean absolute();

    protected abstract boolean required();

    protected abstract String defaultName();

    protected abstract Result getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        Map<String, String> properties, String prefix);

    protected abstract boolean isMissingResult(Result result);

    protected abstract MCRConfigurationException missingException(MCRSourceContext context);

    protected abstract Result nullResultReplacement();

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
            throw context.incompatibilityException(configuration.valueClass(), instance);
        }

        return instance;

    }

    protected final boolean rejectedBySentinel(MCRSentinel sentinel, MCRSourceContext context,
        Map<String, String> properties, String prefix) {

        if (sentinel != null) {
            boolean sentinelValue = sentinel.defaultValue();
            String configuredSentinelValue = properties.remove(prefix + sentinel.name());
            if (configuredSentinelValue != null) {
                sentinelValue = Boolean.parseBoolean(configuredSentinelValue);
            }
            if (sentinelValue == sentinel.rejectionValue()) {
                if (logger.isInfoEnabled()) {
                    logger.info("[SENTINEL] Ignoring {} {} and all sub-properties",
                        context.description(), context.property());
                }
                return true;
            }

        }

        return false;

    }

}
