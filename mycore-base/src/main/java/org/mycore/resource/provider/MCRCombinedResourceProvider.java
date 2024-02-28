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

package org.mycore.resource.provider;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;

/**
 * A {@link MCRCachingResourceProvider} is a {@link MCRResourceProvider} that delegates to multiple other
 * {@link MCRResourceProvider}. If multiple providers return a result when looking up a resource, only the
 * first result is considered.
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedResourceProvider.Factory.class)
public class MCRCombinedResourceProvider extends MCRResourceProviderBase {

    private final List<MCRResourceProvider> providers;

    public MCRCombinedResourceProvider(String coverage, MCRResourceProvider... providers) {
        this(coverage, Arrays.asList(providers));
    }

    public MCRCombinedResourceProvider(String coverage, List<MCRResourceProvider> providers) {
        super(coverage);
        this.providers = new ArrayList<>(Objects.requireNonNull(providers));
        this.providers.forEach(Objects::requireNonNull);
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        for (MCRResourceProvider provider : providers) {
            Optional<URL> resourceUrl = provider.provide(path, hints);
            if (resourceUrl.isPresent()) {
                return resourceUrl;
            }
        }
        return Optional.empty();
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        List<ProvidedUrl> resourceUrls = new LinkedList<>();
        for (MCRResourceProvider provider : providers) {
            for (ProvidedUrl providedURL : provider.provideAll(path, hints)) {
                String origin = coverage() + " / " + providedURL.origin;
                resourceUrls.add(new ProvidedUrl(providedURL.url, origin));
            }
        }
        return resourceUrls;
    }

    @Override
    public List<Supplier<List<PrefixStripper>>> prefixStrippers(MCRHints hints) {
        return providers.stream().flatMap(provider -> provider.prefixStrippers(hints).stream()).toList();
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        providers.forEach(provider -> description.add("Provider", provider.compileDescription(level)));
        return description;
    }

    public static class Factory implements Supplier<MCRCombinedResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.Combined.Coverage")
        public String coverage;

        @MCRInstanceList(name = "Providers", valueClass = MCRResourceProvider.class)
        public List<MCRResourceProvider> providers;

        @Override
        public MCRCombinedResourceProvider get() {
            return new MCRCombinedResourceProvider(coverage, providers);
        }

    }

}
