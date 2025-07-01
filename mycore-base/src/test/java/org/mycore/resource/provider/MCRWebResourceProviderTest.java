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
public class MCRWebResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath WEB_FOO_PATH = MCRResourcePath.ofWebPath("foo").orElseThrow();

    private static final MCRSyntheticResourceSpec FOO_SPEC = new MCRSyntheticResourceSpec("test:", FOO_PATH);

    private static final MCRSyntheticResourceSpec FOO2_SPEC = new MCRSyntheticResourceSpec("test2:", FOO_PATH);

    private static final MCRSyntheticResourceSpec WEB_FOO_SPEC = new MCRSyntheticResourceSpec("test:", WEB_FOO_PATH);

    private static final MCRSyntheticResourceSpec WEB_FOO2_SPEC = new MCRSyntheticResourceSpec("test2:", WEB_FOO_PATH);

    @Test
    public void provideAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = webProvider(Collections.emptyList());

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void providePresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideWebAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = webProvider(Collections.emptyList());

        Optional<URL> resourceUrl = provider.provide(WEB_FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideWebPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(WEB_FOO_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        Optional<URL> resourceUrl = provider.provide(WEB_FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(WEB_FOO_SPEC.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void provideMultiplePresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC, FOO2_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideMultipleWebPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(WEB_FOO_SPEC, WEB_FOO2_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        Optional<URL> resourceUrl = provider.provide(WEB_FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(WEB_FOO_SPEC.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void provideAllAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = webProvider(Collections.emptyList());

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllWebAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = webProvider(Collections.emptyList());

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllWebPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(WEB_FOO_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(WEB_FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(WEB_FOO_SPEC.toUrl(factory)));

    }

    @Test
    public void provideAllMultiplePresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC, FOO2_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());
    }

    @Test
    public void provideAllMultipleWebPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(WEB_FOO_SPEC, WEB_FOO2_SPEC);
        MCRResourceProvider provider = webProvider(specs);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(WEB_FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(WEB_FOO_SPEC.toUrl(factory)));
        assertTrue(resourceUrls.contains(WEB_FOO2_SPEC.toUrl(factory)));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRWebResourceProvider.class),
        @MCRTestProperty(key = "Test.Provider.Class", classNameOf = MCRSyntheticResourceProvider.class),
        @MCRTestProperty(key = "Test.Provider.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Provider.Specs.1.Path", string = "foo"),
        @MCRTestProperty(key = "Test.Provider.Specs.2.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Provider.Specs.2.Path", string = "META-INF/resources/foo"),
    })
    public void configuration(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRWebResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, hints);
        Optional<URL> webFooResourceUrl = provider.provide(WEB_FOO_PATH, hints);

        assertTrue(fooResourceUrl.isEmpty());
        assertTrue(webFooResourceUrl.isPresent());

    }

    private static MCRResourceProvider webProvider(List<MCRSyntheticResourceSpec> specs) {
        return new MCRWebResourceProvider("web test", syntheticProvider(specs));
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
