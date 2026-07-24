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
        if (isMissingResult(result) && !defaultName.isEmpty()) {
            context = new MCRSourceContext(target, defaultName, "default " + description());
            result = getResult(context, configuration, configuration.fullProperties(), defaultName);
        }

        if (isMissingResult(result) && required()) {
            throw missingResultException(context);
        }

        return isMissingResult(result) ? missingResultReplacement() : result;

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

    protected abstract MCRConfigurationException missingResultException(MCRSourceContext context);

    protected abstract Result missingResultReplacement();

    protected final boolean rejectedBySentinel(MCRSentinel sentinel, MCRSourceContext context,
        Map<String, String> properties, String prefix) {

        if (sentinel != null) {
            boolean sentinelValue = sentinel.defaultValue();
            String configuredSentinelValue = properties.get(prefix + sentinel.name());
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
