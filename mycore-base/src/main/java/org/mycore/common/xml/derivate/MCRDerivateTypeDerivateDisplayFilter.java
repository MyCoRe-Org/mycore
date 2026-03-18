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

package org.mycore.common.xml.derivate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;

/**
 * A {@link MCRDerivateTypeDerivateDisplayFilter} is a {@link MCRDerivateDisplayFilter} that looks up
 * explicitly configured mappings for combinations of intent and derivate type.
 * <p>
 * If a derivate has more than one derivate type, the last derivate type is used.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRDerivateTypeDerivateDisplayFilter#MAPPINGS_KEY} can be used to
 * specify the mappings to be used as a map of <code><em>{intent}</em>.<em>{derivate-type}</em></code> keys to
 * boolean values.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.xml.derivate.MCRDerivateTypeDerivateDisplayFilter
 * [...].Mappings.foo-intent.foo-derivate-type=true
 * [...].Mappings.foo-intent.bar-derivate-type=true
 * [...].Mappings.bar-intent.baz-derivate-type=false
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRDerivateTypeDerivateDisplayFilter.Factory.class)
public class MCRDerivateTypeDerivateDisplayFilter implements MCRDerivateDisplayFilter {

    public static final String MAPPINGS_KEY = "Mappings";

    private final Map<String, Mapping> mappings;

    public MCRDerivateTypeDerivateDisplayFilter(Map<String, Mapping> mappings) {
        this.mappings = new HashMap<>(Objects.requireNonNull(mappings, "Mappings must not be null"));
    }

    @Override
    public Boolean isDisplayEnabled(MCRDerivate derivate, String intent) {

        Mapping mapping = mappings.get(intent);
        if (mapping == null) {
            return null;
        }

        Boolean isEnabled = mapping.map(getDerivateType(derivate));
        if (isEnabled == null) {
            return null;
        }

        return isEnabled;

    }

    private String getDerivateType(MCRDerivate derivate) {

        List<String> derivateTypes = new ArrayList<>();
        for (MCRMetaClassification classification : derivate.getDerivate().getClassifications()) {
            if (classification.getClassId().equals("derivate_types")) {
                derivateTypes.add(classification.getCategId());
            }
        }

        return derivateTypes.isEmpty() ? null : derivateTypes.getLast();

    }

    @MCRConfigurationProxy(proxyClass = Mapping.Factory.class)
    public static final class Mapping {

        private final Map<String, Boolean> mappings;

        public Mapping(Map<String, Boolean> mappings) {
            this.mappings = new HashMap<>(Objects.requireNonNull(mappings, "Mappings set must not be null"));
        }

        public Boolean map(String derivateType) {
            return mappings.get(derivateType);
        }

        @Override
        public String toString() {
            return mappings.toString();
        }

        public static final class Factory implements Supplier<Mapping> {

            @MCRPropertyMap(required = false)
            public Map<String, String> mappings;

            @Override
            public Mapping get() {
                return new Mapping(getMappings());
            }

            private Map<String, Boolean> getMappings() {
                return mappings.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Boolean.valueOf(e.getValue())));
            }

        }

    }

    public static class Factory implements Supplier<MCRDerivateTypeDerivateDisplayFilter> {

        @MCRInstanceMap(name = MAPPINGS_KEY, valueClass = Mapping.class, required = false)
        public Map<String, Mapping> mappings;

        @Override
        public MCRDerivateTypeDerivateDisplayFilter get() {
            return new MCRDerivateTypeDerivateDisplayFilter(mappings);
        }

    }

}
