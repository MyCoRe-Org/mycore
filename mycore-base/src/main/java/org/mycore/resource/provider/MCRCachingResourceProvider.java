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
import org.mycore.resource.common.MCRResourceTracer;

/**
 * A {@link MCRCachingResourceProvider} is a {@link MCRResourceProvider} that delegates to another
 * {@link MCRResourceProvider} and uses a {@link MCRCache} to cache the results indefinitely.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * <li> The property suffix {@link MCRCachingResourceProvider#PROVIDER_KEY} can be used to
 * specify the provider to be used.
 * <li> The property suffix {@link MCRCachingResourceProvider#CAPACITY_KEY} can be used to
 * configure the capacity of the underlying cache.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRCachingResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Capacity=1000
 * [...].Provider.Class=foo.bar.FooProvider
 * [...].Provider.Key1=Value1
 * [...].Provider.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCachingResourceProvider.Factory.class)
public class MCRCachingResourceProvider extends MCRResourceProviderBase {

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
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        Optional<URL> resourceUrl = cache.get(path);
        if (resourceUrl == null) {
            tracer.trace(() -> "Cache miss for " + path);
            resourceUrl = provider.provide(path, hints, tracer.update(provider, provider.coverage()));
            cache.put(path, resourceUrl);
        } else {
            tracer.trace(() -> "Cache hit for " + path);
        }
        return resourceUrl;
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        return provider.provideAll(path, hints, tracer.update(provider, provider.coverage()));
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
