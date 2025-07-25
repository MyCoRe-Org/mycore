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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceUtils;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.test.MyCoReTest;

import jakarta.servlet.ServletContext;

@MyCoReTest
public class MCRServletContextWebResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath WEB_FOO_PATH = MCRResourcePath.ofWebPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath WEB_BAR_PATH = MCRResourcePath.ofWebPath("bar").orElseThrow();

    @Test
    public void provideAbsent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideWebAbsent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        Optional<URL> resourceUrl = provider.provide(WEB_BAR_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void providePresent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideWebPresent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        Optional<URL> resourceUrl = provider.provide(WEB_FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toMockJarUrl("foo"), resourceUrl.get());

    }

    @Test
    public void provideAllAbsent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllWebAbsent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(WEB_BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllPresent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllWebPresent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = servletContextWebProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(WEB_FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toMockJarUrl("foo")));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRServletContextWebResourceProvider.class)
    })
    public void configuration() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRServletContextWebResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(WEB_FOO_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(WEB_BAR_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isEmpty());

    }

    private static MCRResourceProvider servletContextWebProvider() {
        return new MCRServletContextWebResourceProvider("servlet context test");
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

    private static MCRHints toHints(String... paths) throws MalformedURLException {
        Set<String> pathSet = new HashSet<>(Arrays.asList(paths));
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getResource(Mockito.anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            if (path.startsWith("/")) {
                String relativePath = path.substring(1);
                if (pathSet.contains(relativePath)) {
                    return toMockJarUrl(relativePath);
                }
            }
            return null;
        });
        return new MCRHintsBuilder().add(MCRResourceHintKeys.SERVLET_CONTEXT, servletContext).build();
    }

    private static URL toMockJarUrl(String path) {
        return MCRResourceUtils.toJarFileUrl("/foo/library.jar", path);
    }

}
