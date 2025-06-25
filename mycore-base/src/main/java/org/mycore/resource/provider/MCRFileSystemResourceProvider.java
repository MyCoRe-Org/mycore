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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

/**
 * A {@link MCRFileSystemResourceProvider} is a {@link MCRResourceProvider} that looks up
 * (depending on the given {@link MCRResourceProviderMode} value) resources or web resources, in the file system.
 * It uses a fixed list of base directories for the lookup.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * <li> The property suffix  {@link MCRFileSystemResourceProviderBase#MODE_KEY} can be used to
 * specify the mode to be used.
 * <li> The property suffix {@link MCRFileSystemResourceProvider#BASE_DIRS_KEY} can be used to
 * specify the list of base directories to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRFileSystemResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Mode=RESOURCES
 * [...].BaseDirs.10=/base/dir/foo
 * [...].BaseDirs.20=/base/dir/bar
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRFileSystemResourceProvider.Factory.class)
public class MCRFileSystemResourceProvider extends MCRFileSystemResourceProviderBase {

    public static final String BASE_DIRS_KEY = "BaseDirs";

    private final List<Path> baseDirs;

    public MCRFileSystemResourceProvider(String coverage, MCRResourceProviderMode mode, Path... baseDirs) {
        this(coverage, mode, Arrays.asList(baseDirs));
    }

    public MCRFileSystemResourceProvider(String coverage, MCRResourceProviderMode mode, List<Path> baseDirs) {
        super(coverage, mode);
        this.baseDirs = Objects.requireNonNull(baseDirs, "Base dirs must not be null");
        this.baseDirs.forEach(baseDir -> Objects.requireNonNull(baseDir, "Base dir must not be null"));
    }

    @Override
    protected final Stream<Path> getBaseDirs(MCRHints hints) {
        return baseDirs.stream();
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        baseDirs.forEach(baseDir -> description.add("BaseDir", baseDir.toString()));
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
            List<Path> baseDirs = MCRConfiguration2.splitValue(this.baseDirs).map(Paths::get).toList();
            return new MCRFileSystemResourceProvider(coverage, mode, baseDirs);
        }

    }

}
