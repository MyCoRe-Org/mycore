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

import static org.mycore.resource.common.MCRTraceLoggingHelper.update;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.locator.MCRResourceLocator;

/**
 * A {@link MCRLocatorResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * using a {@link MCRResourceLocator}. Resource locators are usually part of a {@link MCRLFSResourceProvider} setup.
 * This implementation is intended to easily adopt an existing resource locator into a resource provider.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * <li> The property suffix {@link MCRLocatorResourceProvider#LOCATOR_KEY} can be used to
 * specify the locator to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRFileSystemResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Locator.Class=foo.bar.FooLocator
 * [...].Locator.Key1=Value1
 * [...].Locator.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRLocatorResourceProvider.Factory.class)
public class MCRLocatorResourceProvider extends MCRResourceProviderBase {

    public static final String LOCATOR_KEY = "Locator";

    private final MCRResourceLocator locator;

    public MCRLocatorResourceProvider(String coverage, MCRResourceLocator locator) {
        super(coverage);
        this.locator = Objects.requireNonNull(locator, "Locator must not be null");
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        return locator.locate(path, update(hints, locator, null)).findFirst();
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return locator.locate(path, update(hints, locator, null)).map(this::providedUrl).toList();
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return locator.prefixStrippers(hints);
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        if (!suppressDescriptionDetails() || level.isLessSpecificThan(Level.DEBUG)) {
            description.add("Locator", locator.compileDescription(level));
        }
        return description;
    }

    protected boolean suppressDescriptionDetails() {
        return false;
    }

    public static class Factory implements Supplier<MCRLocatorResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.LFS.Coverage")
        public String coverage;

        @MCRInstance(name = LOCATOR_KEY, valueClass = MCRResourceLocator.class)
        public MCRResourceLocator locator;

        @Override
        public MCRLocatorResourceProvider get() {
            return new MCRLocatorResourceProvider(coverage, locator);
        }

    }

}
