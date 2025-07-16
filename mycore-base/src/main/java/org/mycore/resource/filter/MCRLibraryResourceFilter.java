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

import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;

/**
 * {@link MCRLibraryResourceFilter} is an implementation of {@link MCRResourceFilter} that checks if a resource
 * candidate is a resource from a JAR file. To decide weather such resources are retained or ignored, a
 * {@link MCRResourceFilterMode} value is used.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The mode is configured using the property suffix {@link MCRLibraryResourceFilter#MODE_KEY}.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.filter.MCRLibraryResourceFilter
 * [...].Mode=MUST_MATCH
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRLibraryResourceFilter.Factory.class)
public class MCRLibraryResourceFilter extends MCRUrlPrefixResourceFilterBase {

    public static final String MODE_KEY = "Mode";

    public MCRLibraryResourceFilter(MCRResourceFilterMode mode) {
        super(mode);
    }

    @Override
    protected Optional<String> getPrefix(MCRHints hints) {
        return Optional.of("jar:");
    }

    public static class Factory implements Supplier<MCRLibraryResourceFilter> {

        @MCRProperty(name = MODE_KEY, defaultName = "MCR.Resource.Filter.Default.Library.Mode")
        public String mode;

        @Override
        public MCRLibraryResourceFilter get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRLibraryResourceFilter(mode);
        }

    }

}
