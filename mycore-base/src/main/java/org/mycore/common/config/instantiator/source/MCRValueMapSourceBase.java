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
import java.util.Map;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRSentinel;

/**
 * A {@link MCRValueMapSourceBase} is a base implementation of {@link MCRSource} that
 * obtains a {@link Map} of values for annotation based injection from properties.
 * It provides support for {@link MCRSentinel} for each map entry and
 * uses a {@link MCRValueExtractor} to obtain each map entry value from the corresponding nested properties.
 * 
 * @param <Value> the type of injected map values.
 */
abstract class MCRValueMapSourceBase<Value> extends MCRSourceBase<Map<String, Value>> {

    private final MCRSentinel sentinel;

    private final MCRValueExtractor<Value> extractor;

    MCRValueMapSourceBase(MCRAnnotationProvider annotationProvider, MCRValueExtractor<Value> extractor) {
        this.sentinel = annotationProvider.get(MCRSentinel.class);
        this.extractor = extractor;
    }

    @Override
    protected final Map<String, Value> getResult(MCRSourceContext context, Map<String, String> properties,
        Map<String, String> fullProperties) {

        Map<String, Value> map = new HashMap<>();

        String shortFormProperty = properties.get("");
        if (supportsShortForm() && shortFormProperty != null) {
            parseShortFormMap(shortFormProperty).forEach((key, shortFormValue) -> {
                Value value = extractor.toValue(context, Map.of("", shortFormValue), fullProperties);
                if (value != null) {
                    map.put(key, value);
                }
            });
        }

        String entryDescription = context.description() + " entry";
        for (String key : nextNestedKeys(properties)) {
            MCRSourceContext nestedContext = context.nested(key, entryDescription);
            Map<String, String> nestesProperties = reduceProperties(properties, key);
            if (!rejectedBySentinel(sentinel, nestedContext, nestesProperties)) {
                Value value = extractor.toValue(nestedContext, nestesProperties, fullProperties);
                if (value != null) {
                    map.put(key, value);
                }
            }
        }

        return map;

    }

    protected abstract boolean supportsShortForm();

    private Map<String, String> parseShortFormMap(String value) {
        return MCRConfiguration2.splitValue(value)
            .map(s -> s.split(":", 2))
            .filter(parts -> parts.length != 1)
            .filter(parts -> !parts[1].isBlank())
            .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()));
    }

    @Override
    protected final boolean isMissingResult(Map<String, Value> result) {
        return result.isEmpty();
    }

    @Override
    protected final MCRConfigurationException missingResultException(MCRSourceContext context) {
        return context.emptyException();
    }

    @Override
    protected final Map<String, Value> missingResultReplacement() {
        return new HashMap<>();
    }

}
