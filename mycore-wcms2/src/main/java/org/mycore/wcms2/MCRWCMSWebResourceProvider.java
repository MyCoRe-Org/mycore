/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.wcms2;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.resource.provider.MCRFileSystemResourceProvider;
import org.mycore.resource.provider.MCRResourceProvider;
import org.mycore.resource.provider.MCRResourceProviderMode;

/**
 * {@link MCRWCMSWebResourceProvider} is an implementation of {@link MCRResourceProvider} that looks up web resources
 * in the file system. It uses the directory configured in as {@link MCRWCMSUtil#getWCMSDataDir()} (which is the
 * directory that the WCMS writes into) as a base directory for the lookup.
 * <p>
 * This provider replaces the previously used <code>MCRWebPagesSynchronizer</code> that copied the content
 * of the above-mentioned directory in the webapp directory used by the web container.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The property suffix {@link MCRWCMSWebResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.wcms2.MCRWCMSWebResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRWCMSWebResourceProvider.Factory.class)
public class MCRWCMSWebResourceProvider extends MCRFileSystemResourceProvider {

    public static final String COVERAGE_KEY = "Coverage";

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

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.WCMS.Coverage")
        public String coverage;

        @Override
        public MCRWCMSWebResourceProvider get() {
            return new MCRWCMSWebResourceProvider(coverage);
        }

    }

}
