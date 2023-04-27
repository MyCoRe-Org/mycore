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

package org.mycore.common.resource.provider;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.common.resource.MCRResourcePath;
import org.mycore.common.resource.filter.MCRResourceFilter;
import org.mycore.common.resource.locator.MCRResourceLocator;
import org.mycore.common.resource.selector.MCRResourceSelector;

/**
 * A {@link MCRLFSResourceProvider} is a {@link MCRResourceProvider} that splits the lookup strategy in three phases:
 * <em>locate</em>, <em>filter</em>, <em>select</em> (LFS).
 * <p>
 * In the <em>locate</em>-phase, a {@link MCRResourceLocator} is used to locate a set of possible candidates.
 * In the <em>filter</em>-phase, a {@link MCRResourceFilter} is used to reduce the result of the <em>locate</em>-phase
 * to the set of allowed resources.
 * In the <em>select</em>-phase, a {@link MCRResourceSelector} is used to select prioritized resources from
 * the result of the <em>filter</em>-phase. If, after the <em>select</em>-phase, multiple candidates are
 * till available, the first candidate is chosen.
 */
@MCRConfigurationProxy(proxyClass = MCRLFSResourceProvider.Factory.class)
public class MCRLFSResourceProvider extends MCRResourceProviderBase {

    private final MCRResourceLocator locator;

    private final MCRResourceFilter filter;

    private final MCRResourceSelector selector;

    public MCRLFSResourceProvider(String coverage, MCRResourceLocator locator, MCRResourceFilter filter,
                                  MCRResourceSelector selector) {
        super(coverage);
        this.locator = Objects.requireNonNull(locator);
        this.filter = Objects.requireNonNull(filter);
        this.selector = Objects.requireNonNull(selector);
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        return doProvide(filter.filter(locator.locate(path, hints), hints), hints);
    }

    @Override
    protected final List<ProvidedURL> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return filter.filter(locator.locate(path, hints), hints).map(this::providedURL).collect(Collectors.toList());
    }

    private Optional<URL> doProvide(Stream<URL> resourceUrls, MCRHints hints) {
        return doProvide(resourceUrls.collect(Collectors.toList()), hints);
    }

    private Optional<URL> doProvide(List<URL> resourceUrls, MCRHints hints) {
        if (!resourceUrls.isEmpty()) {
            return selector.select(resourceUrls, hints).stream().findFirst();
        } else {
            return Optional.empty();
        }
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

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.LFS.Coverage")
        public String coverage;

        @MCRInstance(name = "Locator", valueClass = MCRResourceLocator.class)
        public MCRResourceLocator locator;

        @MCRInstance(name = "Filter", valueClass = MCRResourceFilter.class)
        public MCRResourceFilter filter;

        @MCRInstance(name = "Selector", valueClass = MCRResourceSelector.class)
        public MCRResourceSelector selector;

        @Override
        public MCRLFSResourceProvider get() {
            return new MCRLFSResourceProvider(coverage, locator, filter, selector);
        }

    }

}
