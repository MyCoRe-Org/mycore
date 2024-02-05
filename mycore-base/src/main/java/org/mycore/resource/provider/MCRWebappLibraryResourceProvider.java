/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import org.apache.logging.log4j.Level;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.filter.MCRCombinedResourceFilter;
import org.mycore.resource.filter.MCRLibraryResourceFilter;
import org.mycore.resource.filter.MCRResourceFilterMode;
import org.mycore.resource.filter.MCRWebappLibraryResourceFilter;
import org.mycore.resource.locator.MCRClassLoaderResourceLocator;
import org.mycore.resource.selector.MCRCombinedResourceSelector;
import org.mycore.resource.selector.MCRFirstLibraryJarResourceSelector;
import org.mycore.resource.selector.MCRHighestComponentPriorityResourceSelector;

/**
 * A {@link MCRLibraryResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * in JAR files placed in the WAR file, prioritized by {@link MCRComponent#getPriority()} and the order
 * in which the libraries are present in the classpath.
 */
@MCRConfigurationProxy(proxyClass = MCRWebappLibraryResourceProvider.Factory.class)
public final class MCRWebappLibraryResourceProvider extends MCRLFSResourceProvider {

    private final MCRResourceFilterMode mode;

    public MCRWebappLibraryResourceProvider(String coverage, MCRResourceFilterMode mode) {
        super(
            coverage,
            new MCRClassLoaderResourceLocator(),
            new MCRCombinedResourceFilter(
                new MCRLibraryResourceFilter(MCRResourceFilterMode.MUST_MATCH),
                new MCRWebappLibraryResourceFilter(mode)
            ),
            new MCRCombinedResourceSelector(
                new MCRHighestComponentPriorityResourceSelector(),
                new MCRFirstLibraryJarResourceSelector()
            )
        );
        this.mode = mode;
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        description.add("Mode", mode.toString());
        return description;
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRWebappLibraryResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.WebappLibrary.Coverage")
        public String coverage;

        @MCRProperty(name = "Mode", defaultName = "MCR.Resource.Provider.Default.WebappLibrary.Mode")
        public String mode;

        @Override
        public MCRWebappLibraryResourceProvider get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRWebappLibraryResourceProvider(coverage, mode);
        }

    }

}
