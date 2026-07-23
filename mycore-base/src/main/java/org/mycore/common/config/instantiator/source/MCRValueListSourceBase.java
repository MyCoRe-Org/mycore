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

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;

abstract class MCRValueListSourceBase<Value> extends MCRSourceBase<List<Value>> {

    private final MCRSentinel sentinel;

    private final MCRValueExtractor<Value> extractor;

    MCRValueListSourceBase(MCRAnnotationProvider annotationProvider, MCRValueExtractor<Value> extractor) {
        this.sentinel = annotationProvider.get(MCRSentinel.class);
        this.extractor = extractor;
    }

    @Override
    protected final List<Value> getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        Map<String, String> properties, String prefix) {

        Map<String, String> listProperties = new HashMap<>();
        String keyPrefix = prefix.isEmpty() ? prefix : prefix + ".";
        int keyPrefixLength = keyPrefix.length();
        properties.forEach((key, value) -> {
            if (key.startsWith(keyPrefix) && !key.isEmpty()) {
                int index = key.indexOf('.', keyPrefixLength);
                if (index == -1) {
                    if (!value.isBlank()) {
                        listProperties.put(key.substring(keyPrefixLength), value);
                    }
                }
            }
        });

        List<Value> headPropertyList = new ArrayList<>(0);
        List<Value> tailPropertyList = new ArrayList<>(listProperties.size());

        List<String> keyList = context.orderedKeys(listProperties);
        for (String key : keyList) {
            MCRSourceContext nestedContext = context.nested(key, "property list element");
            if (!rejectedBySentinel(sentinel, nestedContext, properties, keyPrefix + key + ".")) {
                if (key.charAt(0) == '-') {
                    headPropertyList.add(extractor.toValue(nestedContext, listProperties.get(key)));
                } else {
                    tailPropertyList.add(extractor.toValue(nestedContext, listProperties.get(key)));
                }
            }
        }

        List<Value> shortFormList = new ArrayList<>();
        String shortFormProperty = properties.get(prefix);
        if (shortFormProperty != null) {
            for (String shortFormValue : parseShortFormList(shortFormProperty)) {
                shortFormList.add(extractor.toValue(context, shortFormValue));
            }
        }

        int totalSize = headPropertyList.size() + shortFormList.size() + tailPropertyList.size();
        List<Value> list = new ArrayList<>(totalSize);
        list.addAll(headPropertyList);
        list.addAll(shortFormList);
        list.addAll(tailPropertyList);

        return list;

    }

    private List<String> parseShortFormList(String value) {
        return MCRConfiguration2.splitValue(value).toList();
    }

    @Override
    protected final boolean isMissingResult(List<Value> result) {
        return result.isEmpty();
    }

    @Override
    protected final MCRConfigurationException missingResultException(MCRSourceContext context) {
        return context.emptyException();
    }

    @Override
    protected final List<Value> missingResultReplacement() {
        return new ArrayList<>();
    }

}
