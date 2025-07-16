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

import java.io.File;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * {@link MCRConfigDirResourceProvider} is an implementation of {@link MCRResourceProvider} that searches resources
 * in the file system. It uses the <code>/resources</code> directory in the config directory as a base directory
 * for the lookup.
 * <p>
 * It uses the config directory hinted at by {@link MCRResourceHintKeys#CONFIG_DIR}, if present.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The property suffix {@link MCRConfigDirResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.provider.MCRConfigDirResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRConfigDirResourceProvider.Factory.class)
public class MCRConfigDirResourceProvider extends MCRFileSystemResourceProviderBase {

    public static final String COVERAGE_KEY = "Coverage";

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

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.ConfigDir.Coverage")
        public String coverage;

        @Override
        public MCRConfigDirResourceProvider get() {
            return new MCRConfigDirResourceProvider(coverage);
        }

    }

}
