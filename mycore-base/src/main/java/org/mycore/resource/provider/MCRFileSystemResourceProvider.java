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
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

import static org.mycore.common.config.MCRConfiguration2.splitValue;

/**
 * {@link MCRFileSystemResourceProvider} is an implementation of {@link MCRResourceProvider} that looks up,
 * depending on the given {@link MCRResourceProviderMode} value, resources or web resources in the file system.
 * It uses a fixed list of base directories for the lookup.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The mode is configures using the property suffix {@link MCRFileSystemResourceProvider#MODE_KEY}.
 * <li> The property suffix {@link MCRFileSystemResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.provider.MCRFileSystemResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].MODE=RESOURCES
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRFileSystemResourceProvider.Factory.class)
public class MCRFileSystemResourceProvider extends MCRFileSystemResourceProviderBase {

    public static final String COVERAGE_KEY = "Coverage";

    public static final String MODE_KEY = "Mode";

    public static final String BASE_DIRS_KEY = "BaseDirs";

    private final List<File> baseDirs;

    public MCRFileSystemResourceProvider(String coverage, MCRResourceProviderMode mode, List<File> baseDirs) {
        super(coverage, mode);
        this.baseDirs = Objects.requireNonNull(baseDirs);
    }

    @Override
    protected final Stream<File> getBaseDirs(MCRHints hints) {
        return baseDirs.stream();
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        baseDirs.forEach(baseDir -> description.add("BaseDir", baseDir.getAbsolutePath()));
        return description;
    }

    public static class Factory implements Supplier<MCRFileSystemResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.FileSystem.Coverage")
        public String coverage;

        @MCRProperty(name = MODE_KEY)
        public String mode;

        @MCRProperty(name = BASE_DIRS_KEY)
        public String baseDirs;

        @Override
        public MCRFileSystemResourceProvider get() {
            MCRResourceProviderMode mode = MCRResourceProviderMode.valueOf(this.mode);
            List<File> baseDirs = splitValue(this.baseDirs).map(File::new).toList();
            return new MCRFileSystemResourceProvider(coverage, mode, baseDirs);
        }

    }

}
