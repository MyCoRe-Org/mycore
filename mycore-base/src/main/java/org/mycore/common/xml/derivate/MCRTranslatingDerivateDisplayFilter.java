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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * A {@link MCRTranslatingDerivateDisplayFilter} is a {@link MCRDerivateDisplayFilter} that translates intents
 * and delegates to another {@link MCRDerivateDisplayFilter} instance.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRTranslatingDerivateDisplayFilter#TRANSLATIONS_KEY} can be used to
 * specify the list of translations to be used.
 * <li> The property suffix {@link MCRTranslatingDerivateDisplayFilter#FILTER_KEY} can be used to
 * specify the filter to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.xml.derivate.MCRTranslatingDerivateDisplayFilter
 * [...].Translations.foo=bar
 * [...].Translations.bar=baz
 * [...].Filter.Class=foo.bar.BarFilter
 * [...].Filter.Key1=Value1
 * [...].Filter.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRTranslatingDerivateDisplayFilter.Factory.class)
public class MCRTranslatingDerivateDisplayFilter implements MCRDerivateDisplayFilter {

    public static final String TRANSLATIONS_KEY = "Translations";

    public static final String FILTER_KEY = "Filter";

    private final Map<String, String> translations;

    private final MCRDerivateDisplayFilter filter;

    public MCRTranslatingDerivateDisplayFilter(Map<String, String> translations, MCRDerivateDisplayFilter filter) {
        this.translations = new HashMap<>(Objects.requireNonNull(translations, "Translations must not be null"));
        this.filter = Objects.requireNonNull(filter, "Filter must not be null");
    }

    @Override
    public Boolean isDisplayEnabled(MCRDerivate derivate, String intent) {
        return filter.isDisplayEnabled(derivate, translations.getOrDefault(intent, intent));
    }

    public static class Factory implements Supplier<MCRTranslatingDerivateDisplayFilter> {

        @MCRPropertyMap(name = TRANSLATIONS_KEY, required = false)
        public Map<String, String> translations;

        @MCRInstance(name = FILTER_KEY, valueClass = MCRDerivateDisplayFilter.class)
        public MCRDerivateDisplayFilter filter;

        @Override
        public MCRTranslatingDerivateDisplayFilter get() {
            return new MCRTranslatingDerivateDisplayFilter(translations, filter);
        }

    }

}
