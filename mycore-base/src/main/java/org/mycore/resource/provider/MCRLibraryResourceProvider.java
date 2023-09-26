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

package org.mycore.resource.provider;

import java.util.function.Supplier;

import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.resource.filter.MCRLibraryResourceFilter;
import org.mycore.resource.filter.MCRResourceFilterMode;
import org.mycore.resource.locator.MCRClassLoaderResourceLocator;
import org.mycore.resource.selector.MCRCombinedResourceSelector;
import org.mycore.resource.selector.MCRFirstLibraryJarResourceSelector;
import org.mycore.resource.selector.MCRHighestComponentPriorityResourceSelector;

/**
 * A {@link MCRLibraryResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * in JAR files, prioritized by {@link MCRComponent#getPriority()} and the order in which the libraries
 * are present in the classpath.
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
                new MCRFirstLibraryJarResourceSelector()
            )
        );
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRLibraryResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.Library.Coverage")
        public String coverage;

        @Override
        public MCRLibraryResourceProvider get() {
            return new MCRLibraryResourceProvider(coverage);
        }

    }

}
