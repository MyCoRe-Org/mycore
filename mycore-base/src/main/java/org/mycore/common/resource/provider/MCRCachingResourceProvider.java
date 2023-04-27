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

import org.apache.logging.log4j.Level;
import org.mycore.common.MCRCache;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.common.resource.MCRResourcePath;

/**
 * A {@link MCRCachingResourceProvider} is a {@link MCRResourceProvider} that delegates to another
 * {@link MCRResourceProvider} and uses a {@link MCRCache} to cache the results.
 */
@MCRConfigurationProxy(proxyClass = MCRCachingResourceProvider.Factory.class)
public class MCRCachingResourceProvider extends MCRResourceProviderBase {

    private final int capacity;

    private final MCRResourceProvider provider;

    private final MCRCache<MCRResourcePath, Optional<URL>> cache;

    public MCRCachingResourceProvider(String coverage, int capacity, MCRResourceProvider provider) {
        super(coverage);
        this.capacity = capacity;
        this.provider = Objects.requireNonNull(provider);
        this.cache = new MCRCache<>(capacity, coverage);
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        Optional<URL> resourceUrl = cache.get(path);
        if (resourceUrl == null) {
            getLogger().debug("Cache miss for {}", path);
            resourceUrl = provider.provide(path, hints);
            cache.put(path, resourceUrl);
        } else {
            getLogger().debug("Cache hit for {}", path);
        }
        return resourceUrl;
    }

    @Override
    protected final List<ProvidedURL> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return provider.provideAll(path, hints);
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        description.add("Capacity", Integer.toString(capacity));
        description.add("Provider", provider.compileDescription(level));
        return description;
    }

    public static class Factory implements Supplier<MCRCachingResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.Caching.Coverage")
        public String coverage;

        @MCRProperty(name = "Capacity", defaultName = "MCR.Resource.Provider.Default.Caching.Capacity")
        public String capacity;

        @MCRInstance(name = "Provider", valueClass = MCRResourceProvider.class)
        public MCRResourceProvider provider;

        @Override
        public MCRCachingResourceProvider get() {
            int capacity = Integer.parseInt(this.capacity);
            return new MCRCachingResourceProvider(coverage, capacity, provider);
        }

    }

}
