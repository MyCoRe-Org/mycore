/*
 *
 * $Revision$ $Date$
 *
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 *
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
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
 * A {@link MCRCombinedResourceFilter} is a {@link MCRResourceFilter} that delegates to multiple other
 * {@link MCRResourceFilter}, one after another.
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedResourceFilter.Factory.class)
public final class MCRCombinedResourceFilter extends MCRResourceFilterBase {

    private final List<MCRResourceFilter> filters;

    public MCRCombinedResourceFilter(MCRResourceFilter... filters) {
        this(Arrays.asList(filters));
    }

    public MCRCombinedResourceFilter(List<MCRResourceFilter> filters) {
        this.filters = new ArrayList<>(Objects.requireNonNull(filters));
        this.filters.forEach(Objects::requireNonNull);
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

        @MCRInstanceList(name = "Filters", valueClass = MCRResourceFilter.class)
        public List<MCRResourceFilter> filters;

        @Override
        public MCRCombinedResourceFilter get() {
            return new MCRCombinedResourceFilter(filters);
        }

    }

}
