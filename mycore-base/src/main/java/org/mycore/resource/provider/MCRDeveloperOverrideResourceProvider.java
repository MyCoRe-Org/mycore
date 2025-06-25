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
import java.util.List;
import java.util.function.Supplier;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRDeveloperOverrideResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * in the file system. It uses a fixed list of base directories configured as a comma-separated-list in
 * {@link MCRDeveloperOverrideResourceProvider#DEVELOPER_RESOURCE_OVERRIDE_PROPERTY}
 * as base directories for the lookup.
 * <p>
 * <em>This provider replaces the previously used <code>MCRDeveloperTools</code>.</em>
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRDeveloperOverrideResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRDeveloperOverrideResourceProvider.Factory.class)
public final class MCRDeveloperOverrideResourceProvider extends MCRFileSystemResourceProvider {

    public static final String DEVELOPER_RESOURCE_OVERRIDE_PROPERTY = "MCR.Developer.Resource.Override";

    public MCRDeveloperOverrideResourceProvider(String coverage) {
        super(coverage, MCRResourceProviderMode.RESOURCES, getBaseDirs());
    }

    private static List<Path> getBaseDirs() {
        String paths = MCRConfiguration2.getString(DEVELOPER_RESOURCE_OVERRIDE_PROPERTY).orElse("");
        return MCRConfiguration2.splitValue(paths).map(Paths::get).toList();
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRDeveloperOverrideResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.DeveloperOverride.Coverage")
        public String coverage;

        @Override
        public MCRDeveloperOverrideResourceProvider get() {
            return new MCRDeveloperOverrideResourceProvider(coverage);
        }

    }

}
