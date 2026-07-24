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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.MCRInstantiatorUtils;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRSourceBase} is a base implementation of {@link MCRSource} that
 * handles basic aspects for obtaining a value for annotation based injection from properties, i.e.
 * <ul>
 *   <li>whether annotation name are allowed</li>
 *   <li>whether the annotation name is absolute or not,</li>
 *   <li>whether falling back to a default value is necessary, if no value is configured</li>
 *   <li>whether an exception is thrown id no value and no default value is configured.</li>
 * </ul>
 *
 * @param <Result> the type of injected value.
 */
abstract class MCRSourceBase<Result> implements MCRSource {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    @SuppressWarnings("PMD.NPathComplexity")
    public final Result get(MCRInstanceConfiguration<?> configuration, MCRTarget target) {

        String name = name();
        Map<String, String> fullProperties = configuration.fullProperties();

        MCRSourceContext context;
        Result result;

        if (supportsAbsoluteName()) {
            if (name.isEmpty()) {
                throw MCRInstantiatorUtils.emptyNameException(target);
            }
            context = new MCRSourceContext(target, name, "absolute " + description());
            result = getResult(context, reduceProperties(fullProperties, name), fullProperties);
        } else {
            if (name.isEmpty() && !supportsEmptyName()) {
                throw MCRInstantiatorUtils.emptyNameException(target);
            }
            if (name.isEmpty()) {
                context = new MCRSourceContext(target, configuration.name().canonical(), description());
                result = getResult(context, configuration.properties(), fullProperties);
            } else {
                context = new MCRSourceContext(target, configuration.name().canonical() + "." + name, description());
                result = getResult(context, reduceProperties(configuration.properties(), name), fullProperties);
            }
        }

        String defaultName = defaultName();
        if (isMissingResult(result) && !defaultName.isEmpty()) {
            context = new MCRSourceContext(target, defaultName, "default " + description());
            result = getResult(context, reduceProperties(fullProperties, defaultName), fullProperties);
        }

        if (isMissingResult(result) && required()) {
            throw missingResultException(context);
        }

        return isMissingResult(result) ? missingResultReplacement() : result;

    }

    protected static Map<String, String> reduceProperties(Map<String, String> properties, String prefix) {

        final String prefixWithDelimiter = prefix + '.';
        final int prefixWithDelimiterLength = prefixWithDelimiter.length();

        Map<String, String> reducedProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefixWithDelimiter)) {
                continue;
            }
            String reducedKey = key.substring(prefixWithDelimiterLength);
            reducedProperties.put(reducedKey, entry.getValue());
        }
        String directProperty = properties.get(prefix);
        if (directProperty != null) {
            reducedProperties.put("", directProperty);
        }
        return reducedProperties;
    }

    protected static Set<String> nextNestedKeys(Map<String, String> properties) {
        Set<String> keys = new HashSet<>();
        properties.keySet().forEach((key) -> {
            if (!key.isEmpty()) {
                int index = key.indexOf('.');
                if (index == -1) {
                    keys.add(key);
                } else {
                    keys.add(key.substring(0, index));
                }
            }
        });
        return keys;
    }

    protected abstract String description();

    protected abstract String name();

    protected abstract String defaultName();

    protected abstract boolean supportsEmptyName();

    protected abstract boolean supportsAbsoluteName();

    protected abstract Result getResult(MCRSourceContext context, Map<String, String> properties,
        Map<String, String> fullProperties);

    protected abstract boolean required();

    protected abstract boolean isMissingResult(Result result);

    protected abstract MCRConfigurationException missingResultException(MCRSourceContext context);

    protected abstract Result missingResultReplacement();

    protected final boolean rejectedBySentinel(MCRSentinel sentinel, MCRSourceContext context,
        Map<String, String> properties) {

        if (sentinel != null) {
            boolean sentinelValue = sentinel.defaultValue();
            String configuredSentinelValue = properties.get(sentinel.name());
            if (configuredSentinelValue != null) {
                sentinelValue = Boolean.parseBoolean(configuredSentinelValue);
            }
            if (sentinelValue == sentinel.rejectionValue()) {
                if (logger.isInfoEnabled()) {
                    logger.info("[SENTINEL] Ignoring {}, configured in {} (and sub-properties thereof), " +
                        "because {}.{} has value {}", context.description(), context.property(),
                        context.property(), sentinel.name(), sentinel.rejectionValue());
                }
                return true;
            }

        }

        return false;

    }

}
