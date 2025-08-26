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

package org.mycore.wcms2;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.provider.MCRFileSystemResourceProviderBase;
import org.mycore.resource.provider.MCRResourceProvider;
import org.mycore.resource.provider.MCRResourceProviderBase;
import org.mycore.resource.provider.MCRResourceProviderMode;

/**
 * A {@link MCRWCMSWebResourceProvider} is a {@link MCRResourceProvider} that looks up web resources
 * in the file system. It uses the directory configured in as {@link MCRWCMSUtil#getWCMSDataDirPath()}
 * (which is the directory that the WCMS writes into) as a base directory for the lookup.
 * <p>
 * <em>This provider replaces the previously used <code>MCRWebPagesSynchronizer</code> that copied the content
 * of the above-mentioned directory in the webapp directory used by the web container.</em>
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.wcms2.MCRWCMSWebResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRWCMSWebResourceProvider.Factory.class)
public final class MCRWCMSWebResourceProvider extends MCRFileSystemResourceProviderBase {

    public MCRWCMSWebResourceProvider(String coverage) {
        super(coverage, MCRResourceProviderMode.WEB_RESOURCES);
    }

    @Override
    protected Stream<Path> getBaseDirs(MCRHints hints) {
        return Stream.of(MCRWCMSUtil.getWCMSDataDirPath());
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
