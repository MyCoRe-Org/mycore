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
import org.mycore.resource.filter.MCRResourceFilter;
import org.mycore.resource.locator.MCRResourceLocator;
import org.mycore.resource.selector.MCRResourceSelector;

/**
 * A {@link MCRLFSResourceProvider} is a {@link MCRResourceProvider} that splits the lookup strategy in three phases:
 * <em>locate</em>, <em>filter</em>, <em>select</em> (LFS).
 * <p>
 * In the <em>locate</em>-phase, a {@link MCRResourceLocator} is used to locate a set of possible candidates.
 * In the <em>filter</em>-phase, a {@link MCRResourceFilter} is used to reduce the result of the <em>locate</em>-phase
 * to the set of allowed resources.
 * In the <em>select</em>-phase, a {@link MCRResourceSelector} is used to select prioritized resources from
 * the result of the <em>filter</em>-phase. If, after the <em>select</em>-phase, multiple candidates are
 * still available, the first candidate is chosen.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * <li> The property suffix {@link MCRLFSResourceProvider#LOCATOR_KEY} can be used to
 * specify the locator to be used.
 * <li> The property suffix {@link MCRLFSResourceProvider#FILTER_KEY} can be used to
 * specify the filter to be used.
 * <li> The property suffix {@link MCRLFSResourceProvider#SELECTOR_KEY} can be used to
 * specify the selector to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRFileSystemResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Locator.Class=foo.bar.FooLocator
 * [...].Locator.Key1=Value1
 * [...].Locator.Key2=Value2
 * [...].Filter.Class=foo.bar.FooFilter
 * [...].Filter.Key1=Value1
 * [...].Filter.Key2=Value2
 * [...].Selector.Class=foo.bar.FooSelector
 * [...].Selector.Key1=Value1
 * [...].Selector.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRLFSResourceProvider.Factory.class)
public class MCRLFSResourceProvider extends MCRResourceProviderBase {

    public static final String LOCATOR_KEY = "Locator";

    public static final String FILTER_KEY = "Filter";

    public static final String SELECTOR_KEY = "Selector";

    private final MCRResourceLocator locator;

    private final MCRResourceFilter filter;

    private final MCRResourceSelector selector;

    public MCRLFSResourceProvider(String coverage, MCRResourceLocator locator, MCRResourceFilter filter,
        MCRResourceSelector selector) {
        super(coverage);
        this.locator = Objects.requireNonNull(locator, "Locator must not be null");
        this.filter = Objects.requireNonNull(filter, "Filter must not be null");
        this.selector = Objects.requireNonNull(selector, "Selector must not be null");
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        return doProvide(filter.filter(locator.locate(path, hints), hints).toList(), hints);
    }

    private Optional<URL> doProvide(List<URL> resourceUrls, MCRHints hints) {
        if (!resourceUrls.isEmpty()) {
            return selector.select(resourceUrls, hints).stream().findFirst();
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return filter.filter(locator.locate(path, hints), hints).map(this::providedUrl).toList();
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
            description.add("Filter", filter.compileDescription(level));
            description.add("Selector", selector.compileDescription(level));
        }
        return description;
    }

    protected boolean suppressDescriptionDetails() {
        return false;
    }

    public static class Factory implements Supplier<MCRLFSResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.LFS.Coverage")
        public String coverage;

        @MCRInstance(name = LOCATOR_KEY, valueClass = MCRResourceLocator.class)
        public MCRResourceLocator locator;

        @MCRInstance(name = FILTER_KEY, valueClass = MCRResourceFilter.class)
        public MCRResourceFilter filter;

        @MCRInstance(name = SELECTOR_KEY, valueClass = MCRResourceSelector.class)
        public MCRResourceSelector selector;

        @Override
        public MCRLFSResourceProvider get() {
            return new MCRLFSResourceProvider(coverage, locator, filter, selector);
        }

    }

}
