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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPropertyListSource} is a {@link MCRSource} that interprets a {@link MCRPropertyList}.
 */
final class MCRPropertyListSource extends MCRSourceBase<List<String>> {

    private final MCRPropertyList annotation;

    private final MCRSentinel sentinel;

    MCRPropertyListSource(MCRPropertyList annotation, MCRAnnotationProvider annotationProvider) {
        this.annotation = annotation;
        this.sentinel = annotationProvider.get(MCRSentinel.class);
    }

    @Override
    public Type type() {
        return Type.PROPERTY_LIST;
    }

    @Override
    public Class<MCRPropertyList> annotationClass() {
        return MCRPropertyList.class;
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
        return List.class;
    }

    @Override
    protected String description() {
        return "property list";
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
    protected List<String> getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        Map<String, String> properties, String prefix) {

        Map<String, String> listProperties = new HashMap<>();
        String keyPrefix = prefix.isEmpty() ? prefix : prefix + ".";
        int keyPrefixLength = keyPrefix.length();
        properties.forEach((key, value) -> {
            if (key.startsWith(keyPrefix) && !key.isEmpty()) {
                int index = key.indexOf('.', keyPrefixLength);
                if (index == -1) {
                    if (!value.isEmpty()) {
                        listProperties.put(key.substring(keyPrefixLength), value);
                    }
                }
            }
        });

        List<String> headPropertyList = new ArrayList<>(0);
        List<String> tailPropertyList = new ArrayList<>(listProperties.size());

        List<String> keyList = context.orderedKeys(listProperties);
        for (String key : keyList) {
            MCRSourceContext nestedContext = context.nested(key, "property list element");
            if (!rejectedBySentinel(sentinel, nestedContext, properties, keyPrefix + key + ".")) {
                if (key.charAt(0) == '-') {
                    headPropertyList.add(listProperties.get(key));
                } else {
                    tailPropertyList.add(listProperties.get(key));
                }
            }
        }

        List<String> shortFormList = List.of();
        String shortFormProperty = properties.get(prefix);
        if (shortFormProperty != null) {
            shortFormList = parseShortFormList(shortFormProperty);
        }

        int totalSize = headPropertyList.size() + shortFormList.size() + tailPropertyList.size();
        List<String> propertyList = new ArrayList<>(totalSize);
        propertyList.addAll(headPropertyList);
        propertyList.addAll(shortFormList);
        propertyList.addAll(tailPropertyList);

        return propertyList;

    }

    private List<String> parseShortFormList(String value) {
        return MCRConfiguration2.splitValue(value).toList();
    }

    @Override
    protected boolean isMissingResult(List<String> result) {
        return result.isEmpty();
    }

    @Override
    protected MCRConfigurationException missingResultException(MCRSourceContext context) {
        return context.emptyException();
    }

    @Override
    protected List<String> missingResultReplacement() {
        return new ArrayList<>();
    }

}
