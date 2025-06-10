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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.common.MCRResourceTracer;

/**
 * A {@link MCRNoOpResourceFilter} is a {@link MCRResourceFilter} that doesn't filter resource candidates.
 * <p>
 * No configuration options are available.
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.filter.MCRNoOpResourceFilter
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRNoOpResourceFilter.Factory.class)
public class MCRNoOpResourceFilter extends MCRResourceFilterBase {

    @Override
    protected Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
        return resourceUrls;
    }

    public static class Factory implements Supplier<MCRNoOpResourceFilter> {

        @Override
        public MCRNoOpResourceFilter get() {
            return new MCRNoOpResourceFilter();
        }

    }

}
