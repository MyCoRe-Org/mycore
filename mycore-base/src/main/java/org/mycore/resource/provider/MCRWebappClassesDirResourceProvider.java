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
import org.mycore.resource.filter.MCRResourceFilterMode;
import org.mycore.resource.filter.MCRWebappClassesDirResourceFilter;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.resource.locator.MCRClassLoaderResourceLocator;
import org.mycore.resource.selector.MCRNoOpResourceSelector;

/**
 * A {@link MCRWebappClassesDirResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * in the <code>/WEB-INF/classes</code> directory inside the webapp directory used by the web container
 * as the base directory for the lookup.
 * <p>
 * <em>Unless placed there manually, such resources originate from the <code>/WEB-INF/classes</code> directory inside
 * the WAR file. In a usual build, such resources originate from the <code>/src/main/resources</code> directory inside
 * the Maven project that creates the WAR file.</em>
 * <p>
 * It uses the webapp directory hinted at by {@link MCRResourceHintKeys#WEBAPP_DIR}, if present
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRWebappClassesDirResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRWebappClassesDirResourceProvider.Factory.class)
public final class MCRWebappClassesDirResourceProvider extends MCRLFSResourceProvider {

    public MCRWebappClassesDirResourceProvider(String coverage) {
        super(
            coverage,
            new MCRClassLoaderResourceLocator(),
            new MCRWebappClassesDirResourceFilter(MCRResourceFilterMode.MUST_MATCH),
            new MCRNoOpResourceSelector());
    }

    @Override
    protected boolean suppressDescriptionDetails() {
        return true;
    }

    public static class Factory implements Supplier<MCRWebappClassesDirResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.WebappClassesDir.Coverage")
        public String coverage;

        @Override
        public MCRWebappClassesDirResourceProvider get() {
            return new MCRWebappClassesDirResourceProvider(coverage);
        }

    }

}
