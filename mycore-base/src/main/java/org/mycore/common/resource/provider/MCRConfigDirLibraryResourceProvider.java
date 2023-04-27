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

package org.mycore.common.resource.provider;

import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.common.resource.filter.MCRCombinedResourceFilter;
import org.mycore.common.resource.filter.MCRConfigDirLibraryResourceFilter;
import org.mycore.common.resource.filter.MCRLibraryResourceFilter;
import org.mycore.common.resource.filter.MCRResourceFilterMode;
import org.mycore.common.resource.hint.MCRResourceHintKeys;
import org.mycore.common.resource.locator.MCRClassLoaderResourceLocator;
import org.mycore.common.resource.selector.MCRCombinedResourceSelector;
import org.mycore.common.resource.selector.MCRFirstLibraryJarResourceSelector;
import org.mycore.common.resource.selector.MCRHighestComponentPriorityResourceSelector;

/**
 * A {@link MCRConfigDirLibraryResourceProvider} is a {@link MCRResourceProvider} that looks up web resources
 * in JAR files placed in the <code>/lib</code> directory in the config directory, prioritized by
 * {@link MCRComponent#getPriority()} and the order in which the libraries are present in the classpath.
 * <p>
 * It uses the config directory hinted at by {@link MCRResourceHintKeys#CONFIG_DIR}, if present.
 */
@MCRConfigurationProxy(proxyClass = MCRConfigDirLibraryResourceProvider.Factory.class)
public final class MCRConfigDirLibraryResourceProvider extends MCRLFSResourceProvider {

    private final MCRResourceFilterMode mode;

    public MCRConfigDirLibraryResourceProvider(String coverage, MCRResourceFilterMode mode) {
        super(
            coverage,
            new MCRClassLoaderResourceLocator(),
            new MCRCombinedResourceFilter(
                new MCRLibraryResourceFilter(MCRResourceFilterMode.MUST_MATCH),
                new MCRConfigDirLibraryResourceFilter(mode)
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

    public static class Factory implements Supplier<MCRConfigDirLibraryResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.ConfigDirLibrary.Coverage")
        public String coverage;

        @MCRProperty(name = "Mode", defaultName = "MCR.Resource.Provider.Default.ConfigDirLibrary.Mode")
        public String mode;

        @Override
        public MCRConfigDirLibraryResourceProvider get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRConfigDirLibraryResourceProvider(coverage, mode);
        }

    }

}
