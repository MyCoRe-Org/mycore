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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * A {@link MCRCombinedDerivateDisplayFilter} is a {@link MCRDerivateDisplayFilter} that delegates to multiple
 * other {@link MCRDerivateDisplayFilter} instances, one after another, returning the first result that is not
 * <code>null</code>, or <code>null</code>, if no such result exists.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRCombinedDerivateDisplayFilter#FILTERS_KEY} can be used to
 * specify the list of filers to be used.
 * <li> For each filter, the property suffix {@link MCRSentinel#ENABLED_KEY} can be used to
 * excluded that filter from the configuration.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.xml.derivate.MCRCombinedDerivateDisplayFilter
 * [...].Filters.10.Class=foo.bar.FooFilter
 * [...].Filters.10.Enabled=true
 * [...].Filters.10.Key1=Value1
 * [...].Filters.10.Key2=Value2
 * [...].Filters.20.Class=foo.bar.BarFilter
 * [...].Filters.20.Enabled=false
 * [...].Filters.20.Key1=Value1
 * [...].Filters.20.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedDerivateDisplayFilter.Factory.class)
public class MCRCombinedDerivateDisplayFilter implements MCRDerivateDisplayFilter {

    public static final String FILTERS_KEY = "Filters";

    private final List<MCRDerivateDisplayFilter> filters;

    public MCRCombinedDerivateDisplayFilter(MCRDerivateDisplayFilter... filters) {
        this(Arrays.asList(Objects.requireNonNull(filters, "Filters must not be null")));
    }

    public MCRCombinedDerivateDisplayFilter(List<MCRDerivateDisplayFilter> filters) {
        this.filters = new ArrayList<>(Objects.requireNonNull(filters, "Filters must not be null"));
        this.filters.forEach(filter -> Objects.requireNonNull(filter, "Filter must not be null"));
    }

    @Override
    public Boolean isDisplayEnabled(MCRDerivate derivate, String intent) {
        for (MCRDerivateDisplayFilter filter : filters) {
            Boolean excludeDerivate = filter.isDisplayEnabled(derivate, intent);
            if (excludeDerivate != null) {
                return excludeDerivate;
            }
        }
        return null;
    }

    public static class Factory implements Supplier<MCRCombinedDerivateDisplayFilter> {

        @MCRInstanceList(name = FILTERS_KEY, valueClass = MCRDerivateDisplayFilter.class, required = false,
            sentinel = @MCRSentinel)
        public List<MCRDerivateDisplayFilter> filters;

        @Override
        public MCRCombinedDerivateDisplayFilter get() {
            return new MCRCombinedDerivateDisplayFilter(filters);
        }

    }

}
