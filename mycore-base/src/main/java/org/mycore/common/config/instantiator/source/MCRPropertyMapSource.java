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
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPropertyMapSource} is a {@link MCRSource} that interprets a {@link MCRPropertyMap}.
 */
final class MCRPropertyMapSource extends MCRSourceBase<Map<String, String>> {

    private final MCRPropertyMap annotation;

    private final MCRSentinel sentinel;

    MCRPropertyMapSource(MCRPropertyMap annotation, MCRAnnotationProvider annotationProvider) {
        this.annotation = annotation;
        this.sentinel = annotationProvider.get(MCRSentinel.class);
    }

    @Override
    public Type type() {
        return Type.PROPERTY_MAP;
    }

    @Override
    public Class<MCRPropertyMap> annotationClass() {
        return MCRPropertyMap.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Set<MCRTarget.Type> allowedTargetTypes() {
        return MCRTarget.Types.ALL;
    }

    @Override
    public Class<?> valueClass() {
        return Map.class;
    }

    @Override
    protected String description() {
        return "property map";
    }

    @Override
    protected String name() {
        return annotation.name();
    }

    @Override
    protected boolean allowsEmptyName() {
        return true;
    }

    @Override
    protected boolean absolute() {
        return annotation.absolute();
    }

    @Override
    protected boolean required() {
        return annotation.required();
    }

    @Override
    protected String defaultName() {
        return annotation.defaultName();
    }

    @Override
    protected Map<String, String> getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
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
                    if (!value.isEmpty()) {
                        mapProperties.put(key.substring(keyPrefixLength), value);
                    }
                }
            }
        });

        Map<String, String> propertyMap = new HashMap<>();

        for (String key : mapProperties.keySet()) {
            MCRSourceContext nestedContext = context.nested(key, "property map entry");
            if (!rejectedBySentinel(sentinel, nestedContext, properties, keyPrefix + key + ".")) {
                propertyMap.put(key, mapProperties.get(key));
            }
        }

        return propertyMap;

    }

    private Map<String, String> parseShortFormMap(String value) {
        return MCRConfiguration2.splitValue(value)
            .map(s -> s.split(":", 2))
            .filter(parts -> parts.length != 1)
            .filter(parts -> !parts[1].isBlank())
            .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()));
    }

    @Override
    protected boolean isMissingResult(Map<String, String> result) {
        return result.isEmpty();
    }

    @Override
    protected MCRConfigurationException missingResultException(MCRSourceContext context) {
        return context.emptyException();
    }

    @Override
    protected Map<String, String> missingResultReplacement() {
        return new HashMap<>();
    }

}
