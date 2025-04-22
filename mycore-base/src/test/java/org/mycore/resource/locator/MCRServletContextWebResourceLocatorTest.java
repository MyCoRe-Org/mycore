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

package org.mycore.resource.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.test.MCRTestUrlExtension;
import org.mycore.test.MyCoReTest;

import jakarta.servlet.ServletContext;

@MyCoReTest
@ExtendWith(MCRTestUrlExtension.class)
public class MCRServletContextWebResourceLocatorTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath WEB_FOO_PATH = MCRResourcePath.ofWebPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath WEB_BAR_PATH = MCRResourcePath.ofWebPath("bar").orElseThrow();

    @Test
    public void locateAbsent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceLocator locator = servletContextLocator();

        List<URL> resourceUrls = locator.locate(BAR_PATH, hints).toList();

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void locateWebAbsent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceLocator locator = servletContextLocator();

        List<URL> resourceUrls = locator.locate(WEB_BAR_PATH, hints).toList();

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void locatePresent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceLocator locator = servletContextLocator();

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertTrue(resourceUrls.isEmpty());
    }

    @Test
    public void locateWebPresent() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceLocator locator = servletContextLocator();

        List<URL> resourceUrls = locator.locate(WEB_FOO_PATH, hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toMockJarUrl("foo")));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRServletContextWebResourceLocator.class)
    })
    public void configuration() throws MalformedURLException {

        MCRHints hints = toHints("foo");
        MCRResourceLocator locator = MCRConfiguration2.getInstanceOfOrThrow(
            MCRServletContextWebResourceLocator.class, "Test.Class");

        List<URL> fooResourceUrl = locator.locate(WEB_FOO_PATH, hints).toList();
        List<URL> barResourceUrl = locator.locate(WEB_BAR_PATH, hints).toList();

        assertEquals(1, fooResourceUrl.size());
        assertEquals(0, barResourceUrl.size());

    }

    private static MCRResourceLocator servletContextLocator() {
        return new MCRServletContextWebResourceLocator();
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

    private static URL toMockJarUrl(String path) throws MalformedURLException {
        return URI.create("jar:file:/foo/library.jar!/" + path).toURL();
    }

}
