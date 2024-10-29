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

import static org.mycore.common.config.MCRConfiguration2.splitValue;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * {@link MCRDeveloperOverrideResourceProvider} is an implementation of {@link MCRResourceProvider} that looks up
 * resources in the file system. It uses a fixed list of base directories configured as a comma-separated-list in
 * {@link MCRDeveloperOverrideResourceProvider#DEVELOPER_RESOURCE_OVERRIDE_PROPERTY} as base directories for the lookup.
 * <p>
 * This provider replaces the previously used <code>MCRDeveloperTools</code>.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The property suffix {@link MCRDeveloperOverrideResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.provider.MCRDeveloperOverrideResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRDeveloperOverrideResourceProvider.Factory.class)
public class MCRDeveloperOverrideResourceProvider extends MCRFileSystemResourceProvider {

    public static final String DEVELOPER_RESOURCE_OVERRIDE_PROPERTY = "MCR.Developer.Resource.Override";

    public static final String COVERAGE_KEY = "Coverage";

    public MCRDeveloperOverrideResourceProvider(String coverage) {
        super(coverage, MCRResourceProviderMode.RESOURCES, getBaseDirs());
    }

    private static List<File> getBaseDirs() {
        String paths = MCRConfiguration2.getString(DEVELOPER_RESOURCE_OVERRIDE_PROPERTY).orElse("");
        return splitValue(paths).map(File::new).toList();
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
