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

import java.util.function.Supplier;

import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.resource.filter.MCRLibraryResourceFilter;
import org.mycore.resource.filter.MCRResourceFilterMode;
import org.mycore.resource.locator.MCRClassLoaderResourceLocator;
import org.mycore.resource.selector.MCRCombinedResourceSelector;
import org.mycore.resource.selector.MCRFirstServletLibraryResourceSelector;
import org.mycore.resource.selector.MCRHighestComponentPriorityResourceSelector;

/**
 * A {@link MCRLibraryResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * in JAR files, prioritized by {@link MCRComponent#getPriority()} and the order in which the libraries
 * are present in the classpath.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRLibraryResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRLibraryResourceProvider.Factory.class)
public final class MCRLibraryResourceProvider extends MCRLFSResourceProvider {

    public MCRLibraryResourceProvider(String coverage) {
        super(
            coverage,
            new MCRClassLoaderResourceLocator(),
            new MCRLibraryResourceFilter(MCRResourceFilterMode.MUST_MATCH),
            new MCRCombinedResourceSelector(
                new MCRHighestComponentPriorityResourceSelector(),
                new MCRFirstServletLibraryResourceSelector()));
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRLibraryResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.Library.Coverage")
        public String coverage;

        @Override
        public MCRLibraryResourceProvider get() {
            return new MCRLibraryResourceProvider(coverage);
        }

    }

}
