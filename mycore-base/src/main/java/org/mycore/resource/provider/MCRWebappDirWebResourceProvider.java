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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRFileSystemResourceProvider} is a {@link MCRResourceProvider} that looks up web resources
 * in the file system. It uses the webapp directory used by the web container as the base directory
 * for the lookup.
 * <p>
 * Unless placed there manually, such resources originate from the root directory inside the WAR file.
 * <p>
 * In a usual build, such resources originate from the <code>/src/main/webapp</code> directory
 * inside the Maven project that creates the WAR file.
 * <p>
 * It uses the webapp directory hinted at by {@link MCRResourceHintKeys#WEBAPP_DIR}, if present.
 */

/**
 * {@link MCRWebappDirWebResourceProvider} is an implementation of {@link MCRResourceProvider} that looks up web
 * resources in the file system. It uses the webapp directory used by the web container as the base directory
 * for the lookup.
 * <p>
 * Unless placed there manually, such resources originate from the root directory inside the WAR file. In a usual
 * build, such resources originate from the <code>/src/main/webapp</code> directory inside the Maven project that
 * creates the WAR file.
 * <p>
 * It uses the webapp directory hinted at by {@link MCRResourceHintKeys#WEBAPP_DIR}, if present.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The property suffix {@link MCRWebappDirWebResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.provider.MCRWebappDirWebResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRWebappDirWebResourceProvider.Factory.class)
public class MCRWebappDirWebResourceProvider extends MCRFileSystemResourceProviderBase {

    public static final String COVERAGE_KEY = "Coverage";

    public MCRWebappDirWebResourceProvider(String coverage) {
        super(coverage, MCRResourceProviderMode.WEB_RESOURCES);
    }

    @Override
    protected final Stream<File> getBaseDirs(MCRHints hints) {
        return getWebResourcesDir(hints).stream();
    }

    private Optional<File> getWebResourcesDir(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.WEBAPP_DIR);
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRWebappDirWebResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.WebappDir.Coverage")
        public String coverage;

        @Override
        public MCRWebappDirWebResourceProvider get() {
            return new MCRWebappDirWebResourceProvider(coverage);
        }

    }

}
