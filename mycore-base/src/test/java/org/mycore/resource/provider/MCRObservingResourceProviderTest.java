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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTestUrlConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRSyntheticResourceSpec;
import org.mycore.resource.provider.MCRObservingResourceProvider.Observer;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.test.MCRTestUrlExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRTestUrlExtension.class)
@MCRTestUrlConfiguration(protocols = "test2")
public class MCRObservingResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRSyntheticResourceSpec FOO_SPEC = new MCRSyntheticResourceSpec("test:", FOO_PATH);

    private static final MCRSyntheticResourceSpec FOO2_SPEC = new MCRSyntheticResourceSpec("test2:", FOO_PATH);

    @Test
    public void provideAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        Observer observer = Mockito.mock(Observer.class);
        MCRResourceProvider provider = observingProvider(Collections.emptyList(), observer);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());
        Mockito.verify(observer, Mockito.times(1)).onProvide(FOO_PATH, hints);
        Mockito.verify(observer, Mockito.times(1)).onProvided(FOO_PATH, hints, resourceUrl);

    }

    @Test
    public void providePresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        Observer observer = Mockito.mock(Observer.class);
        MCRResourceProvider provider = observingProvider(specs, observer);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(FOO_SPEC.toUrl(factory), resourceUrl.get());
        Mockito.verify(observer, Mockito.times(1)).onProvide(FOO_PATH, hints);
        Mockito.verify(observer, Mockito.times(1)).onProvided(FOO_PATH, hints, resourceUrl);

    }

    @Test
    public void provideMultiplePresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC, FOO2_SPEC);
        Observer observer = Mockito.mock(Observer.class);
        MCRResourceProvider provider = observingProvider(specs, observer);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(FOO_SPEC.toUrl(factory), resourceUrl.get());
        Mockito.verify(observer, Mockito.times(1)).onProvide(FOO_PATH, hints);
        Mockito.verify(observer, Mockito.times(1)).onProvided(FOO_PATH, hints, resourceUrl);

    }

    @Test
    public void provideAllAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        Observer observer = Mockito.mock(Observer.class);
        MCRResourceProvider provider = observingProvider(Collections.emptyList(), observer);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());
        Mockito.verify(observer, Mockito.times(1)).onProvideAll(FOO_PATH, hints);
        Mockito.verify(observer, Mockito.times(1)).onProvidedAll(FOO_PATH, hints, providedResourceUrls);

    }

    @Test
    public void provideAllPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        Observer observer = Mockito.mock(Observer.class);
        MCRResourceProvider provider = observingProvider(specs, observer);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));
        Mockito.verify(observer, Mockito.times(1)).onProvideAll(FOO_PATH, hints);
        Mockito.verify(observer, Mockito.times(1)).onProvidedAll(FOO_PATH, hints, providedResourceUrls);

    }

    @Test
    public void provideAllMultiplePresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC, FOO2_SPEC);
        Observer observer = Mockito.mock(Observer.class);
        MCRResourceProvider provider = observingProvider(specs, observer);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));
        assertTrue(resourceUrls.contains(FOO2_SPEC.toUrl(factory)));
        Mockito.verify(observer, Mockito.times(1)).onProvideAll(FOO_PATH, hints);
        Mockito.verify(observer, Mockito.times(1)).onProvidedAll(FOO_PATH, hints, providedResourceUrls);

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRObservingResourceProvider.class),
        @MCRTestProperty(key = "Test.Provider.Class", classNameOf = MCRSyntheticResourceProvider.class),
        @MCRTestProperty(key = "Test.Provider.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Provider.Specs.1.Path", string = "foo"),
        @MCRTestProperty(key = "Test.Observer.Class", classNameOf = MCRObservingResourceProvider.NoOpObserver.class)
    })
    public void configuration(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRObservingResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isEmpty());

    }

    private static MCRResourceProvider observingProvider(List<MCRSyntheticResourceSpec> specs, Observer observer) {
        return new MCRObservingResourceProvider("observing test", syntheticProvider(specs), observer);
    }

    private static MCRResourceProvider syntheticProvider(List<MCRSyntheticResourceSpec> specs) {
        return new MCRSyntheticResourceProvider("synthetic test", specs);
    }

    private static MCRHints toHints(URLStreamHandlerFactory factory) {
        return new MCRHintsBuilder().add(MCRSyntheticResourceSpec.URL_STREAM_HANDLER_FACTORY, factory).build();
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

}
