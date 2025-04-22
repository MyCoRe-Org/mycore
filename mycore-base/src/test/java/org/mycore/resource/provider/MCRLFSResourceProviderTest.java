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
import static org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.mycore.resource.filter.MCRNoOpResourceFilter;
import org.mycore.resource.filter.MCRResourceFilterBase;
import org.mycore.resource.locator.MCRSyntheticResourceLocator;
import org.mycore.resource.selector.MCRNoOpResourceSelector;
import org.mycore.resource.selector.MCRResourceSelectorBase;
import org.mycore.test.MCRTestUrlExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRTestUrlExtension.class)
@MCRTestUrlConfiguration(protocols = "test2")
public class MCRLFSResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRSyntheticResourceSpec FOO_SPEC1 = new MCRSyntheticResourceSpec("test:/foo/1", FOO_PATH);

    private static final MCRSyntheticResourceSpec FOO_SPEC2 = new MCRSyntheticResourceSpec("test:/foo/2", FOO_PATH);

    private static final MCRSyntheticResourceSpec FOO_SPEC3 = new MCRSyntheticResourceSpec("test:/bar/1", FOO_PATH);

    private static final MCRSyntheticResourceSpec FOO_SPEC4 = new MCRSyntheticResourceSpec("test:/bar/2", FOO_PATH);

    private static final List<MCRSyntheticResourceSpec> ALL_SPECS =
        List.of(FOO_SPEC1, FOO_SPEC2, FOO_SPEC3, FOO_SPEC4);

    @Test
    public void provide(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = lfsProvider(ALL_SPECS, "foo", "1");

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(FOO_SPEC1.toUrl(factory), resourceUrl.get());

    }

    @Test
    public void provideAll(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = lfsProvider(ALL_SPECS, "foo", "1");

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        // expect all matching resource URLs, no selection
        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC1.toUrl(factory)));
        assertTrue(resourceUrls.contains(FOO_SPEC2.toUrl(factory)));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRLFSResourceProvider.class),
        @MCRTestProperty(key = "Test.Locator.Class", classNameOf = MCRSyntheticResourceLocator.class),
        @MCRTestProperty(key = "Test.Locator.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Locator.Specs.1.Path", string = "foo"),
        @MCRTestProperty(key = "Test.Filter.Class", classNameOf = MCRNoOpResourceFilter.class),
        @MCRTestProperty(key = "Test.Selector.Class", classNameOf = MCRNoOpResourceSelector.class)
    })
    public void configuration(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRLFSResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isEmpty());

    }

    private static MCRResourceProvider lfsProvider(List<MCRSyntheticResourceSpec> specs, String filterDirName,
        String selectorDirName) {
        return new MCRLFSResourceProvider("lfs test", new MCRSyntheticResourceLocator(specs),
            new DirectoryNameResourceFilter(filterDirName), new DirectoryNameResourceSelector(selectorDirName));
    }

    private static MCRHints toHints(URLStreamHandlerFactory factory) {
        return new MCRHintsBuilder().add(MCRSyntheticResourceSpec.URL_STREAM_HANDLER_FACTORY, factory).build();
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

    private static class DirectoryNameResourceFilter extends MCRResourceFilterBase {

        private final String dirName;

        private DirectoryNameResourceFilter(String dirName) {
            this.dirName = dirName;
        }

        @Override
        protected Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints) {
            return resourceUrls.filter(u -> u.toString().contains("/" + dirName + "/"));
        }

    }

    private static class DirectoryNameResourceSelector extends MCRResourceSelectorBase {

        private final String dirName;

        private DirectoryNameResourceSelector(String dirName) {
            this.dirName = dirName;
        }

        @Override
        protected final List<URL> doSelect(List<URL> resourceUrls, MCRHints hints) {
            return resourceUrls.stream().filter(u -> u.toString().contains("/" + dirName + "/")).toList();
        }

    }

}
