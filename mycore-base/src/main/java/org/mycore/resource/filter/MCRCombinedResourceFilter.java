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

package org.mycore.resource.filter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

/**
 * {@link MCRCombinedResourceFilter} is an implementation of {@link MCRResourceFilter} that delegates to multiple
 * other {@link MCRResourceFilter} instances, one after another.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> Filters are configured as a list using the property suffix {@link MCRCombinedResourceFilter#FILTERS_KEY}.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.filter.MCRCombinedResourceFilter
 * [...].Filters.10.Class=MUST_MATCH
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedResourceFilter.Factory.class)
public final class MCRCombinedResourceFilter extends MCRResourceFilterBase {

    public static final String FILTERS_KEY = "Filters";

    private final List<MCRResourceFilter> filters;

    public MCRCombinedResourceFilter(MCRResourceFilter... filters) {
        this(Arrays.asList(filters));
    }

    public MCRCombinedResourceFilter(List<MCRResourceFilter> filters) {
        this.filters = new ArrayList<>(Objects.requireNonNull(filters, "Filters must not be null"));
        this.filters.forEach(filter -> Objects.requireNonNull(filter, "Filter must not be null"));
    }

    @Override
    protected Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints) {
        Stream<URL> filteredResourceUrls = resourceUrls;
        for (MCRResourceFilter filter : filters) {
            filteredResourceUrls = filter.filter(filteredResourceUrls, hints);
        }
        return filteredResourceUrls;
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        filters.forEach(filter -> description.add("Filter", filter.compileDescription(level)));
        return description;
    }

    public static class Factory implements Supplier<MCRCombinedResourceFilter> {

        @MCRInstanceList(name = FILTERS_KEY, valueClass = MCRResourceFilter.class)
        public List<MCRResourceFilter> filters;

        @Override
        public MCRCombinedResourceFilter get() {
            return new MCRCombinedResourceFilter(filters);
        }

    }

}
