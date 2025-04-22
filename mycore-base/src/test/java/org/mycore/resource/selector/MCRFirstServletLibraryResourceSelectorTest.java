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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.test.MyCoReTest;

import jakarta.servlet.ServletContext;

@MyCoReTest
public class MCRFirstServletLibraryResourceSelectorTest {

    private static URL fileResourceUrl;

    private static URL library1ResourceUrl;

    private static URL library2ResourceUrl;

    private static URL otherLibrary1ResourceUrl;

    private static URL otherLibraryResourceUrl;

    @BeforeAll
    public static void prepare() throws IOException {

        fileResourceUrl = URI.create("file:/foo/bar").toURL();
        library1ResourceUrl = URI.create("jar:file:/foo/WEB-INF/lib/library1.jar!/foo/bar").toURL();
        library2ResourceUrl = URI.create("jar:file:/foo/WEB-INF/lib/library2.jar!/foo/bar").toURL();
        otherLibrary1ResourceUrl = URI.create("jar:file:/foo/library1.jar!/foo/bar").toURL();
        otherLibraryResourceUrl = URI.create("jar:file:/foo/library.jar!/foo/bar").toURL();

    }

    @Test
    public void nonMatchingUrl() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // no selection can be made (resource URL not part of the libraries) ...
        List<URL> resourceUrls = selector.select(List.of(fileResourceUrl), hints);

        // ... expect all resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileResourceUrl));

    }

    @Test
    public void nonMatchingUrls() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // no selection can be made (resource URLs not part of the libraries) ...
        List<URL> resourceUrls = selector.select(List.of(fileResourceUrl, otherLibraryResourceUrl), hints);

        // ... expect all resource URLs
        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileResourceUrl));
        assertTrue(resourceUrls.contains(otherLibraryResourceUrl));

    }

    @Test
    public void nonMatchingUrlWithSameNameAsServletLibrary() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // no selection can be made (resource URL not part of the libraries) ...
        List<URL> resourceUrls = selector.select(List.of(otherLibrary1ResourceUrl), hints);

        // ... expect all resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(otherLibrary1ResourceUrl));

    }

    @Test
    public void matchingUrl() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // selection can be made (first resource URL part of first library) ...
        List<URL> resourceUrls = selector.select(List.of(library1ResourceUrl, otherLibraryResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(library1ResourceUrl));

    }

    @Test
    public void lowerPriorityMatchingUrl() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // selection can be made (second resource URL part of second library) ...
        List<URL> resourceUrls = selector.select(List.of(otherLibraryResourceUrl, library2ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(library2ResourceUrl));

    }

    @Test
    public void multipleMatchingUrls() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(library1ResourceUrl, library2ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(library1ResourceUrl));

    }

    @Test
    public void multipleMatchingUrls2() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(library2ResourceUrl, library1ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(library1ResourceUrl));

    }

    @Test
    public void multipleMatchingUrlsReverseOrder() {

        MCRHints hints = toHints("library3.jar", "library2.jar", "library1.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(library1ResourceUrl, library2ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(library2ResourceUrl));

    }

    @Test
    public void multipleMatchingUrlsReverseOrder2() {

        MCRHints hints = toHints("library3.jar", "library2.jar", "library1.jar");
        MCRResourceSelector selector = firstServletLibrarySelector();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(library2ResourceUrl, library1ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(library2ResourceUrl));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRFirstServletLibraryResourceSelector.class)
    })
    public void configuration() {

        MCRHints hints = toHints("library1.jar", "library2.jar", "library3.jar");
        MCRResourceSelector selector = MCRConfiguration2.getInstanceOfOrThrow(
            MCRFirstServletLibraryResourceSelector.class, "Test.Class");

        List<URL> resourceUrls = selector.select(List.of(library1ResourceUrl), hints);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(library1ResourceUrl));

    }

    private static MCRResourceSelector firstServletLibrarySelector() {
        return new MCRFirstServletLibraryResourceSelector();
    }

    private static MCRHints toHints(String... jarUrls) {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(ServletContext.ORDERED_LIBS))
            .thenAnswer(invocation -> Arrays.asList(jarUrls));
        return new MCRHintsBuilder().add(MCRResourceHintKeys.SERVLET_CONTEXT, servletContext).build();
    }

}
