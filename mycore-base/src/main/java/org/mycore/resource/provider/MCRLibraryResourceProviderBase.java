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

package org.mycore.resource.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.mycore.common.config.MCRComponent;
import org.mycore.resource.filter.MCRCombinedResourceFilter;
import org.mycore.resource.filter.MCRLibraryResourceFilter;
import org.mycore.resource.filter.MCRResourceFilter;
import org.mycore.resource.filter.MCRResourceFilterMode;
import org.mycore.resource.locator.MCRClassLoaderResourceLocator;
import org.mycore.resource.selector.MCRCombinedResourceSelector;
import org.mycore.resource.selector.MCRFirstServletLibraryResourceSelector;
import org.mycore.resource.selector.MCRHighestComponentPriorityResourceSelector;

/**
 * {@link MCRLibraryResourceProviderBase} is a base implementation of a {@link MCRResourceProvider} that looks up
 * resources in JAR files, prioritized by {@link MCRComponent#getPriority()} and the order in which the libraries
 * are present in the classpath.
 */
public abstract class MCRLibraryResourceProviderBase extends MCRLFSResourceProvider {

    public MCRLibraryResourceProviderBase(String coverage, MCRResourceFilter... filters) {
        this(coverage, Arrays.asList(Objects.requireNonNull(filters, "Filters must not be null")));
    }

    public MCRLibraryResourceProviderBase(String coverage, List<MCRResourceFilter> filters) {
        super(
            coverage,
            new MCRClassLoaderResourceLocator(),
            combineWithLibraryFilter(filters),
            new MCRCombinedResourceSelector(
                new MCRHighestComponentPriorityResourceSelector(),
                new MCRFirstServletLibraryResourceSelector()));
    }

    private static MCRResourceFilter combineWithLibraryFilter(List<MCRResourceFilter> filters) {
        MCRLibraryResourceFilter libraryFilter = new MCRLibraryResourceFilter(MCRResourceFilterMode.MUST_MATCH);
        List<MCRResourceFilter> combinedFilters = Stream.concat(Stream.of(libraryFilter), filters.stream()).toList();
        return new MCRCombinedResourceFilter(combinedFilters);
    }

}
