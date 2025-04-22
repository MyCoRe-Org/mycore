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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRSyntheticResourceSpec;
import org.mycore.resource.locator.MCRResourceLocator;
import org.mycore.resource.locator.MCRSyntheticResourceLocator;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.test.MCRTestUrlExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRTestUrlExtension.class)
public class MCRLocatorResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRSyntheticResourceSpec FOO_SPEC = new MCRSyntheticResourceSpec("test:", FOO_PATH);

    @Test
    public void provideAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = locatorProvider(Collections.emptyList());

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void providePresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = locatorProvider(specs);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(FOO_SPEC.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void provideAllAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = locatorProvider(Collections.emptyList());

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(0, resourceUrls.size());

    }

    @Test
    public void provideAllPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceProvider provider = locatorProvider(specs);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRLocatorResourceProvider.class),
        @MCRTestProperty(key = "Test.Locator.Class", classNameOf = MCRSyntheticResourceLocator.class),
        @MCRTestProperty(key = "Test.Locator.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Locator.Specs.1.Path", string = "foo")
    })
    public void configuration(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRLocatorResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isEmpty());

    }

    private static MCRResourceProvider locatorProvider(List<MCRSyntheticResourceSpec> specs) {
        return new MCRLocatorResourceProvider("locator test", syntheticLocator(specs));
    }

    private static MCRResourceLocator syntheticLocator(List<MCRSyntheticResourceSpec> specs) {
        return new MCRSyntheticResourceLocator(specs);
    }

    private static MCRHints toHints(URLStreamHandlerFactory factory) {
        return new MCRHintsBuilder().add(MCRSyntheticResourceSpec.URL_STREAM_HANDLER_FACTORY, factory).build();
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

}
