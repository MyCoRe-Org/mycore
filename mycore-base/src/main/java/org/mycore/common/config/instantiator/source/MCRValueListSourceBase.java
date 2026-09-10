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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRProperTree;

/**
 * A {@link MCRValueListSourceBase} is a base implementation of {@link MCRSource} that
 * obtains a {@link List} of values for annotation based injection from properties.
 * It provides support for {@link MCRSentinel} for each list element and
 * uses a {@link MCRValueExtractor} to obtain each list element value from the corresponding nested properties.
 *
 * @param <Value> the type of injected map values.
 */
abstract sealed class MCRValueListSourceBase<Value> extends MCRSourceBase<List<Value>> permits
    MCRClassPropertyListSource, MCRInstanceListSource, MCRPropertyListSource {

    private final MCRSentinel sentinel;

    private final MCRValueExtractor<Value> extractor;

    MCRValueListSourceBase(MCRAnnotationProvider annotationProvider, MCRValueExtractor<Value> extractor) {
        this.sentinel = annotationProvider.get(MCRSentinel.class);
        this.extractor = extractor;
    }

    @Override
    protected final List<Value> getResult(MCRSourceContext context, MCRProperTree properties,
        MCRProperTree fullProperties) {

        List<Value> list = new ArrayList<>();

        int negativeKeyCount = 0;
        String elementDescription = context.description() + " element";
        for (String key : orderedKeys(context, properties.keys())) {
            if (key.charAt(0) == '-') {
                negativeKeyCount++;
            }
            MCRSourceContext nestedContext = context.nested(key, elementDescription);
            MCRProperTree nestesProperties = properties.nested(key);
            if (!rejectedBySentinel(sentinel, nestedContext, nestesProperties)) {
                Value value = extractor.toValue(nestedContext, nestesProperties, fullProperties);
                if (value != null) {
                    list.add(value);
                }
            }
        }

        String shortFormProperty = properties.value();
        if (supportsShortForm() && shortFormProperty != null) {
            List<Value> shortFormList = new ArrayList<>();
            for (String shortFormValue : parseShortFormList(shortFormProperty)) {
                Value value = extractor.toValue(context, MCRProperTree.of(shortFormValue), fullProperties);
                if (value != null) {
                    shortFormList.add(value);
                }
            }
            list.addAll(negativeKeyCount, shortFormList);
        }

        return list;

    }

    public List<String> orderedKeys(MCRSourceContext context, Stream<String> keys) {

        SortedMap<Integer, String> keyMap = new TreeMap<>();
        keys.forEach(key -> {
            try {
                Integer integerValue = Integer.parseInt(key);
                String alreadyMappedKey = keyMap.put(integerValue, key);
                if (alreadyMappedKey != null && !alreadyMappedKey.equals(key)) {
                    throw context.inconsistentIntegerKeysException(key, alreadyMappedKey);
                }
            } catch (NumberFormatException exception) {
                throw context.nonIntegerKeyException(key, exception);
            }
        });

        return new ArrayList<>(keyMap.values());

    }

    protected abstract boolean supportsShortForm();

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
