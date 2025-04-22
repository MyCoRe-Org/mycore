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
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTestUrlConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRSyntheticResourceSpec;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.test.MCRTestUrlExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRTestUrlExtension.class)
@MCRTestUrlConfiguration(protocols = "test2")
public class MCRCombinedResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath BAZ_PATH = MCRResourcePath.ofPath("baz").orElseThrow();

    private static final MCRSyntheticResourceSpec FOO_SPEC = new MCRSyntheticResourceSpec("test:", FOO_PATH);

    private static final MCRSyntheticResourceSpec FOO2_SPEC = new MCRSyntheticResourceSpec("test2:", FOO_PATH);

    private static final MCRSyntheticResourceSpec BAR_SPEC = new MCRSyntheticResourceSpec("test2:", BAR_PATH);

    @Test
    public void provideAbsentAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = combinedProvider(Collections.emptyList(), Collections.emptyList());

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideAbsentPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = combinedProvider(Collections.emptyList(), specs);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(FOO_SPEC.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void providePresentAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = combinedProvider(specs, Collections.emptyList());

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(FOO_SPEC.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void providePresentPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs1 = List.of(FOO_SPEC);
        List<MCRSyntheticResourceSpec> specs2 = List.of(FOO2_SPEC);
        MCRResourceProvider provider = combinedProvider(specs1, specs2);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(FOO_SPEC.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void provideAbsentButNotEmptyPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs1 = List.of(FOO_SPEC);
        List<MCRSyntheticResourceSpec> specs2 = List.of(BAR_SPEC);
        MCRResourceProvider provider = combinedProvider(specs1, specs2);

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(BAR_SPEC.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void provideAllAbsentAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = combinedProvider(Collections.emptyList(), Collections.emptyList());

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllAbsentPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = combinedProvider(Collections.emptyList(), specs);

        List<MCRResourceProvider.ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));

    }

    @Test
    public void provideAllPresentAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = combinedProvider(specs, Collections.emptyList());

        List<MCRResourceProvider.ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));

    }

    @Test
    public void provideAllPresentPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs1 = List.of(FOO_SPEC);
        List<MCRSyntheticResourceSpec> specs2 = List.of(FOO2_SPEC);
        MCRResourceProvider provider = combinedProvider(specs1, specs2);

        List<MCRResourceProvider.ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));
        assertTrue(resourceUrls.contains(FOO2_SPEC.toUrl(factory)));

    }

    @Test
    public void provideAllAbsentButNotEmptyPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs1 = List.of(FOO_SPEC);
        List<MCRSyntheticResourceSpec> specs2 = List.of(BAR_SPEC);
        MCRResourceProvider provider = combinedProvider(specs1, specs2);

        List<MCRResourceProvider.ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(BAR_SPEC.toUrl(factory)));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRCombinedResourceProvider.class),
        @MCRTestProperty(key = "Test.Providers.1.Class", classNameOf = MCRSyntheticResourceProvider.class),
        @MCRTestProperty(key = "Test.Providers.1.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Providers.1.Specs.1.Path", string = "foo"),
        @MCRTestProperty(key = "Test.Providers.2.Class", classNameOf = MCRSyntheticResourceProvider.class),
        @MCRTestProperty(key = "Test.Providers.2.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Providers.2.Specs.1.Path", string = "bar")
    })
    public void configuration(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRCombinedResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, hints);
        Optional<URL> bazResourceUrl = provider.provide(BAZ_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isPresent());
        assertTrue(bazResourceUrl.isEmpty());

    }

    private static MCRResourceProvider combinedProvider(List<MCRSyntheticResourceSpec> specs1,
        List<MCRSyntheticResourceSpec> specs2) {
        return new MCRCombinedResourceProvider("combined test", syntheticProvider("1", specs1),
            syntheticProvider("2", specs2));
    }

    private static MCRResourceProvider syntheticProvider(String id, List<MCRSyntheticResourceSpec> specs) {
        return new MCRSyntheticResourceProvider("synthetic test " + id, specs);
    }

    private static MCRHints toHints(URLStreamHandlerFactory factory) {
        return new MCRHintsBuilder().add(MCRSyntheticResourceSpec.URL_STREAM_HANDLER_FACTORY, factory).build();
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

}
