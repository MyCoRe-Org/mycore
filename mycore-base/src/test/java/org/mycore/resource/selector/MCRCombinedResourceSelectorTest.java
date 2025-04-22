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

package org.mycore.resource.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRCombinedResourceSelectorTest {

    private static URL fooFooResourceUrl;

    private static URL fooBarResourceUrl;

    private static URL barFooResourceUrl;

    private static URL barBarResourceUrl;

    private static List<URL> allResourceUrls;

    @BeforeAll
    public static void prepare() throws IOException {

        fooFooResourceUrl = URI.create("file:/foo/foo").toURL();
        fooBarResourceUrl = URI.create("file:/foo/bar").toURL();
        barFooResourceUrl = URI.create("file:/bar/foo").toURL();
        barBarResourceUrl = URI.create("file:/bar/bar").toURL();

        allResourceUrls = List.of(fooFooResourceUrl, fooBarResourceUrl, barFooResourceUrl, barBarResourceUrl);

    }

    @Test
    public void noSelectors() {

        MCRResourceSelector selector = combinedSelector();

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(4, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooFooResourceUrl));
        assertTrue(resourceUrls.contains(fooBarResourceUrl));
        assertTrue(resourceUrls.contains(barFooResourceUrl));
        assertTrue(resourceUrls.contains(barBarResourceUrl));

    }

    @Test
    public void noMatchingSelector() {

        MCRResourceSelector selector = combinedSelector(new FilenameResourceSelector("baz"));

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(4, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooFooResourceUrl));
        assertTrue(resourceUrls.contains(fooBarResourceUrl));
        assertTrue(resourceUrls.contains(barFooResourceUrl));
        assertTrue(resourceUrls.contains(barBarResourceUrl));

    }

    @Test
    public void noMatchingSelectors() {

        MCRResourceSelector selector = combinedSelector(new FilenameResourceSelector("baz"),
            new DirectoryNameResourceSelector("baz"));

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(4, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooFooResourceUrl));
        assertTrue(resourceUrls.contains(fooBarResourceUrl));
        assertTrue(resourceUrls.contains(barFooResourceUrl));
        assertTrue(resourceUrls.contains(barBarResourceUrl));

    }

    @Test
    public void oneMatchingSelector() {

        MCRResourceSelector selector = combinedSelector(new DirectoryNameResourceSelector("foo"),
            new FilenameResourceSelector("baz"));

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooFooResourceUrl));
        assertTrue(resourceUrls.contains(fooBarResourceUrl));

    }

    @Test
    public void oneMatchingSelector2() {

        MCRResourceSelector selector = combinedSelector(new DirectoryNameResourceSelector("baz"),
            new FilenameResourceSelector("bar"));

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooBarResourceUrl));
        assertTrue(resourceUrls.contains(barBarResourceUrl));

    }

    @Test
    public void twoMatchingSelector() {

        MCRResourceSelector selector = combinedSelector(new DirectoryNameResourceSelector("foo"),
            new FilenameResourceSelector("bar"));

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooBarResourceUrl));

    }

    @Test
    public void twoMatchingSelector2() {

        MCRResourceSelector selector = combinedSelector(new DirectoryNameResourceSelector("bar"),
            new FilenameResourceSelector("foo"));

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(barFooResourceUrl));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRCombinedResourceSelector.class),
        @MCRTestProperty(key = "Test.Selectors.1.Class", classNameOf = FooDirectoryNameResourceSelector.class),
        @MCRTestProperty(key = "Test.Selectors.2.Class", classNameOf = BarFilenameResourceSelector.class)
    })
    public void configuration() {

        MCRResourceSelector selector = MCRConfiguration2.getInstanceOfOrThrow(
            MCRCombinedResourceSelector.class, "Test.Class");

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooBarResourceUrl));

    }

    private static MCRResourceSelector combinedSelector(MCRResourceSelector... selectors) {
        return new MCRCombinedResourceSelector(selectors);
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

    public static class FooDirectoryNameResourceSelector extends DirectoryNameResourceSelector {

        public FooDirectoryNameResourceSelector() {
            super("foo");
        }

    }

    private static class FilenameResourceSelector extends MCRResourceSelectorBase {

        private final String filename;

        private FilenameResourceSelector(String filename) {
            this.filename = filename;
        }

        @Override
        protected final List<URL> doSelect(List<URL> resourceUrls, MCRHints hints) {
            return resourceUrls.stream().filter(u -> u.toString().endsWith("/" + filename)).toList();
        }

    }

    public static class BarFilenameResourceSelector extends FilenameResourceSelector {

        public BarFilenameResourceSelector() {
            super("bar");
        }

    }

}
