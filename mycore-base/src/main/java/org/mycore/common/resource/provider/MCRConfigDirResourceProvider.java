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

import java.io.File;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRConfigDirResourceProvider} is a {@link MCRResourceProvider} that searches resources
 * in the file system. It uses the <code>/resources</code> directory in the config directory as a base
 * directory for the lookup.
 * <p>
 * It uses the config directory hinted at by {@link MCRResourceHintKeys#CONFIG_DIR}, if present.
 */
@MCRConfigurationProxy(proxyClass = MCRConfigDirResourceProvider.Factory.class)
public class MCRConfigDirResourceProvider extends MCRFileSystemResourceProviderBase {

    public MCRConfigDirResourceProvider(String coverage) {
        super(coverage, MCRResourceProviderMode.RESOURCES);
    }

    @Override
    protected final Stream<File> getBaseDirs(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.CONFIG_DIR).map(this::getResourcesDir).stream();
    }

    private File getResourcesDir(File configDir) {
        return new File(configDir, "resources");
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRConfigDirResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.ConfigDir.Coverage")
        public String coverage;

        @Override
        public MCRConfigDirResourceProvider get() {
            return new MCRConfigDirResourceProvider(coverage);
        }

    }

}
