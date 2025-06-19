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
import java.util.Collections;
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

/**
 * {@link MCRWebResourceProvider} is an implementation of {@link MCRResourceProvider} that delegates to another
 * {@link MCRResourceProvider} but only allows web resources to be resolved.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The provider is configured using the property suffix {@link MCRWebResourceProvider#PROVIDER_KEY}.
 * <li> The property suffix {@link MCRWebResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.provider.MCRWebResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Provider.Class=foo.bar.FooProvider
 * [...].Provider.Key1=Value1
 * [...].Provider.Key2=Value2
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRWebResourceProvider.Factory.class)
public class MCRWebResourceProvider extends MCRResourceProviderBase {

    public static final String COVERAGE_KEY = "Coverage";

    public static final String PROVIDER_KEY = "Provider";

    private final MCRResourceProvider provider;

    public MCRWebResourceProvider(String coverage, MCRResourceProvider provider) {
        super(coverage);
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        return path.isWebPath() ? provider.provide(path, hints) : Optional.empty();
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return path.isWebPath() ? provider.provideAll(path, hints) : Collections.emptyList();
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return provider.prefixStrippers(hints);
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        description.add("Provider", provider.compileDescription(level));
        return description;
    }

    public static class Factory implements Supplier<MCRWebResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.Web.Coverage")
        public String coverage;

        @MCRInstance(name = PROVIDER_KEY, valueClass = MCRResourceProvider.class)
        public MCRResourceProvider provider;

        @Override
        public MCRWebResourceProvider get() {
            return new MCRWebResourceProvider(coverage, provider);
        }

    }

}
