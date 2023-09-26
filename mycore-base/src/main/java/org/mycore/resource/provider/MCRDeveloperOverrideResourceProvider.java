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

import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRFileSystemResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * in the file system. It uses a fixed list of base directories configured as a comma-separated-list
 * in <code>MCR.Developer.Resource.Override</code> as base directories for the lookup.
 * <p>
 * This provider replaces the previously used <code>MCRDeveloperTools</code>.
 */
@MCRConfigurationProxy(proxyClass = MCRDeveloperOverrideResourceProvider.Factory.class)
public class MCRDeveloperOverrideResourceProvider extends MCRFileSystemResourceProvider {

    public static final String DEVELOPER_RESOURCE_OVERRIDE_PROPERTY = "MCR.Developer.Resource.Override";

    public MCRDeveloperOverrideResourceProvider(String coverage) {
        super(coverage, MCRResourceProviderMode.RESOURCES, getBaseDirs());
    }

    private static List<File> getBaseDirs() {
        String paths = MCRConfiguration2.getString(DEVELOPER_RESOURCE_OVERRIDE_PROPERTY).orElse("");
        return MCRConfiguration2.splitValue(paths).map(File::new).collect(Collectors.toList());
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRDeveloperOverrideResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.DeveloperOverride.Coverage")
        public String coverage;

        @Override
        public MCRDeveloperOverrideResourceProvider get() {
            return new MCRDeveloperOverrideResourceProvider(coverage);
        }

    }


}
