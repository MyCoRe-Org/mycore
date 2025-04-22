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

import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.resource.locator.MCRClassLoaderResourceLocator;

/**
 * A {@link MCRClassLoaderResourceProvider} is a {@link MCRResourceProvider} that uses
 * {@link ClassLoader#getResource(String)} to look up a resource.
 * <p>
 * It uses the {@link ClassLoader} hinted at by {@link MCRResourceHintKeys#CLASS_LOADER}, if present.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRClassLoaderResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRClassLoaderResourceProvider.Factory.class)
public final class MCRClassLoaderResourceProvider extends MCRLocatorResourceProvider {

    public MCRClassLoaderResourceProvider(String coverage) {
        super(coverage, new MCRClassLoaderResourceLocator());
    }

    public static class Factory implements Supplier<MCRClassLoaderResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.ClassLoader.Coverage")
        public String coverage;

        @Override
        public MCRClassLoaderResourceProvider get() {
            return new MCRClassLoaderResourceProvider(coverage);
        }

    }

}
