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

package org.mycore.resource.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRCombinedResourceFilterTest {

    private static URL fileUrlFoo;

    private static URL fileUrlBar;

    private static URL fileUrlBaz;

    private static List<URL> allResourceUrls;

    @BeforeAll
    public static void prepare() throws IOException {

        fileUrlFoo = URI.create("file:/foo/bar/foo").toURL();
        fileUrlBar = URI.create("file:/foo/bar/bar").toURL();
        fileUrlBaz = URI.create("file:/foo/bar/baz").toURL();

        allResourceUrls = List.of(fileUrlFoo, fileUrlBar, fileUrlBaz);

    }

    @Test
    public void noFilters() {

        MCRResourceFilter filter = configDirLibraryFilter();

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), MCRHints.EMPTY).toList();

        assertEquals(3, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrlFoo));
        assertTrue(resourceUrls.contains(fileUrlBar));
        assertTrue(resourceUrls.contains(fileUrlBaz));

    }

    @Test
    public void noMatchingFilters() throws MalformedURLException {

        MCRResourceFilter filter = configDirLibraryFilter(URI.create("file:/something/completely/different").toURL());

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), MCRHints.EMPTY).toList();

        assertEquals(3, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrlFoo));
        assertTrue(resourceUrls.contains(fileUrlBar));
        assertTrue(resourceUrls.contains(fileUrlBaz));

    }

    @Test
    public void oneMatchingFilter() {

        MCRResourceFilter filter = configDirLibraryFilter(fileUrlFoo);

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), MCRHints.EMPTY).toList();

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrlBar));
        assertTrue(resourceUrls.contains(fileUrlBaz));

    }

    @Test
    public void twoMatchingFilters() {

        MCRResourceFilter filter = configDirLibraryFilter(fileUrlFoo, fileUrlBaz);

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), MCRHints.EMPTY).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrlBar));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRCombinedResourceFilter.class),
        @MCRTestProperty(key = "Test.Filters.1.Class", classNameOf = NoFooFilter.class),
        @MCRTestProperty(key = "Test.Filters.2.Class", classNameOf = NoBarFilter.class),
    })
    public void configuration() {

        MCRResourceFilter filter = MCRConfiguration2.getInstanceOfOrThrow(
            MCRCombinedResourceFilter.class, "Test.Class");

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), MCRHints.EMPTY).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrlBaz));

    }

    private static MCRResourceFilter configDirLibraryFilter(URL... urls) {
        return new MCRCombinedResourceFilter(Arrays.stream(urls).map(MCRCombinedResourceFilterTest::toFilter)
            .toArray(MCRResourceFilter[]::new));
    }

    private static MCRResourceFilter toFilter(URL url) {

        return new MCRResourceFilterBase() {

            @Override
            protected Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
                return resourceUrls.filter(u -> u != url);
            }

        };

    }

    public static class NoFooFilter extends MCRResourceFilterBase {

        @Override
        protected Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
            return resourceUrls.filter(u -> !u.toString().endsWith("/foo"));
        }

    }

    public static class NoBarFilter extends MCRResourceFilterBase {

        @Override
        protected Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
            return resourceUrls.filter(u -> !u.toString().endsWith("/bar"));
        }

    }

}
