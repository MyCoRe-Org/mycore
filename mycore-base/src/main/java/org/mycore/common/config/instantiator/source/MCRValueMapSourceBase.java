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
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;

abstract class MCRValueMapSourceBase<Value> extends MCRSourceBase<Map<String, Value>> {

    private final MCRSentinel sentinel;

    private final MCRValueExtractor<Value> extractor;

    MCRValueMapSourceBase(MCRAnnotationProvider annotationProvider, MCRValueExtractor<Value> extractor) {
        this.sentinel = annotationProvider.get(MCRSentinel.class);
        this.extractor = extractor;
    }

    @Override
    protected final Map<String, Value> getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        Map<String, String> properties, String prefix) {

        Map<String, String> shortFormMap = Map.of();
        String shortFormProperty = properties.get(prefix);
        if (shortFormProperty != null) {
            shortFormMap = parseShortFormMap(shortFormProperty);
        }

        Map<String, String> mapProperties = new HashMap<>(shortFormMap);
        String keyPrefix = prefix.isEmpty() ? prefix : prefix + ".";
        int keyPrefixLength = keyPrefix.length();
        properties.forEach((key, value) -> {
            if (key.startsWith(keyPrefix) && !key.isEmpty()) {
                int index = key.indexOf('.', keyPrefixLength);
                if (index == -1) {
                    if (!value.isBlank()) {
                        mapProperties.put(key.substring(keyPrefixLength), value);
                    }
                }
            }
        });

        Map<String, Value> map = new HashMap<>();

        for (String key : mapProperties.keySet()) {
            MCRSourceContext nestedContext = context.nested(key, "property map entry");
            if (!rejectedBySentinel(sentinel, nestedContext, properties, keyPrefix + key + ".")) {
                map.put(key, extractor.toValue(nestedContext, mapProperties.get(key)));
            }
        }

        return map;

    }

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
