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
import org.mycore.resource.common.MCRResourceTracer;

/**
 * A {@link MCRObservingResourceProvider} is a {@link MCRResourceProvider} that delegates to another
 * {@link MCRResourceProvider} and invokes callback methods of an {@link Observer} when resources are looked up.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * <li> The property suffix {@link MCRObservingResourceProvider#PROVIDER_KEY} can be used to
 * specify the provider to be used.
 * <li> The property suffix {@link MCRObservingResourceProvider#OBSERVER_KEY} can be used to
 * specify the observer to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRCachingResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Provider.Class=foo.bar.FooProvider
 * [...].Provider.Key1=Value1
 * [...].Provider.Key2=Value2
 * [...].Observer.Class=foo.bar.FooObserver
 * [...].Observer.Key1=Value1
 * [...].Observer.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRObservingResourceProvider.Factory.class)
public class MCRObservingResourceProvider extends MCRResourceProviderBase {

    public static final String PROVIDER_KEY = "Provider";

    public static final String OBSERVER_KEY = "Observer";

    private final MCRResourceProvider provider;

    private final Observer observer;

    public MCRObservingResourceProvider(String coverage, MCRResourceProvider provider, Observer observer) {
        super(coverage);
        this.provider = Objects.requireNonNull(provider, "Provider must not be null");
        this.observer = Objects.requireNonNull(observer, "Observer must not be null");
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        observer.onProvide(path, hints);
        Optional<URL> resourceUrl = provider.provide(path, hints, tracer.update(provider, provider.coverage()));
        observer.onProvided(path, hints, resourceUrl);
        return resourceUrl;
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        observer.onProvideAll(path, hints);
        List<ProvidedUrl> providedUrls = provider.provideAll(path, hints, tracer.update(provider, provider.coverage()));
        observer.onProvidedAll(path, hints, providedUrls);
        return providedUrls;
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return provider.prefixStrippers(hints);
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        description.add("Provider", provider.compileDescription(level));
        description.add("Observer", observer.getClass().getName());
        return description;
    }

    public interface Observer {

        void onProvide(MCRResourcePath path, MCRHints hints);

        void onProvided(MCRResourcePath path, MCRHints hints, Optional<URL> resourceUrl);

        void onProvideAll(MCRResourcePath path, MCRHints hints);

        void onProvidedAll(MCRResourcePath path, MCRHints hints, List<ProvidedUrl> providedUrls);

    }

    public static class NoOpObserver implements Observer {

        @Override
        public void onProvide(MCRResourcePath path, MCRHints hints) {
        }

        @Override
        public void onProvided(MCRResourcePath path, MCRHints hints, Optional<URL> resourceUrl) {
        }

        @Override
        public void onProvideAll(MCRResourcePath path, MCRHints hints) {
        }

        @Override
        public void onProvidedAll(MCRResourcePath path, MCRHints hints, List<ProvidedUrl> providedUrls) {
        }

    }

    public static class Factory implements Supplier<MCRObservingResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.Observing.Coverage")
        public String coverage;

        @MCRInstance(name = PROVIDER_KEY, valueClass = MCRResourceProvider.class)
        public MCRResourceProvider provider;

        @MCRInstance(name = OBSERVER_KEY, valueClass = Observer.class)
        public Observer observer;

        @Override
        public MCRObservingResourceProvider get() {
            return new MCRObservingResourceProvider(coverage, provider, observer);
        }

    }

}
