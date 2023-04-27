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

package org.mycore.wcms2;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.resource.provider.MCRFileSystemResourceProvider;
import org.mycore.common.resource.provider.MCRResourceProvider;
import org.mycore.common.resource.provider.MCRResourceProviderMode;

/**
 * A {@link MCRFileSystemResourceProvider} is a {@link MCRResourceProvider} that looks up web resources
 * in the file system. It uses the directory configured in as {@link MCRWCMSUtil#getWCMSDataDir()} (which is the
 * directory that the WCMS writes into) as a base directory for the lookup.
 * <p>
 * This provider replaces the previously used <code>MCRWebPagesSynchronizer</code> that copied the content
 * of the above-mentioned directory in the webapp directory used by the web container.
 */
@MCRConfigurationProxy(proxyClass = MCRWCMSWebResourceProvider.Factory.class)
public class MCRWCMSWebResourceProvider extends MCRFileSystemResourceProvider {

    public MCRWCMSWebResourceProvider(String coverage) {
        super(coverage, MCRResourceProviderMode.WEB_RESOURCES, getBaseDirs());
    }

    private static List<File> getBaseDirs() {
        return Collections.singletonList(MCRWCMSUtil.getWCMSDataDir());
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRWCMSWebResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.WCMS.Coverage")
        public String coverage;

        @Override
        public MCRWCMSWebResourceProvider get() {
            return new MCRWCMSWebResourceProvider(coverage);
        }

    }

}
