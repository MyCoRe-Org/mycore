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
import org.mycore.common.MCRCache;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;

/**
 * {@link MCRCachingResourceProvider} is an implementation of {@link MCRResourceProvider} that delegates to another
 * {@link MCRResourceProvider} and uses a {@link MCRCache} to cache the results.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The provider is configured using the property suffix {@link MCRCachingResourceProvider#PROVIDER_KEY}.
 * <li> The property suffix {@link MCRCachingResourceProvider#CAPACITY_KEY} can be used to configure tha capacity
 * of the underlying cache.
 * <li> The property suffix {@link MCRCachingResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.provider.MCRCachingResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Capacity=1000
 * [...].Provider.Class=foo.bar.FooProvider
 * [...].Provider.Key1=Value1
 * [...].Provider.Key2=Value2
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCachingResourceProvider.Factory.class)
public class MCRCachingResourceProvider extends MCRResourceProviderBase {

    public static final String COVERAGE_KEY = "Coverage";

    public static final String CAPACITY_KEY = "Capacity";

    public static final String PROVIDER_KEY = "Provider";

    private final int capacity;

    private final MCRResourceProvider provider;

    private final MCRCache<MCRResourcePath, Optional<URL>> cache;

    public MCRCachingResourceProvider(String coverage, int capacity, MCRResourceProvider provider) {
        super(coverage);
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be positive, got " + capacity);
        }
        this.capacity = capacity;
        this.provider = Objects.requireNonNull(provider, "Provider must not be null");
        this.cache = new MCRCache<>(capacity, coverage);
    }

    @Override
    @SuppressWarnings("OptionalAssignedToNull")
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        Optional<URL> resourceUrl = cache.get(path);
        if (resourceUrl == null) {
            logger.debug("Cache miss for {}", path);
            resourceUrl = provider.provide(path, hints);
            cache.put(path, resourceUrl);
        } else {
            logger.debug("Cache hit for {}", path);
        }
        return resourceUrl;
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return provider.provideAll(path, hints);
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return provider.prefixStrippers(hints);
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        description.add("Capacity", Integer.toString(capacity));
        description.add("Provider", provider.compileDescription(level));
        return description;
    }

    public static class Factory implements Supplier<MCRCachingResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.Caching.Coverage")
        public String coverage;

        @MCRProperty(name = CAPACITY_KEY, defaultName = "MCR.Resource.Provider.Default.Caching.Capacity")
        public String capacity;

        @MCRInstance(name = PROVIDER_KEY, valueClass = MCRResourceProvider.class)
        public MCRResourceProvider provider;

        @Override
        public MCRCachingResourceProvider get() {
            int capacity = Integer.parseInt(this.capacity);
            return new MCRCachingResourceProvider(coverage, capacity, provider);
        }

    }

}
