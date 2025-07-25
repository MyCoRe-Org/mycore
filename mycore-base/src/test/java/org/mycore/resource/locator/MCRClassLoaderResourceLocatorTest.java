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
import static org.mycore.resource.MCRFileSystemResourceHelper.getConfigDirTestBasePath;
import static org.mycore.resource.MCRFileSystemResourceHelper.touchFiles;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceUtils;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRClassLoaderResourceLocatorTest {

    public static final ClassLoader PARENT_CLASS_LOADER = MCRClassTools.getClassLoader();

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static Path emptyBaseDir;

    private static Path empty2BaseDir;

    private static Path fooBaseDir;

    private static Path foo2BaseDir;

    private static Path barBaseDir;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirTestBasePath(MCRClassLoaderResourceLocatorTest.class);

        Path fooPath = Path.of("foo");
        Path barPath = Path.of("bar");

        emptyBaseDir = touchFiles(basePath.resolve("empty"));
        empty2BaseDir = touchFiles(basePath.resolve("empty2"));
        fooBaseDir = touchFiles(basePath.resolve("foo"), fooPath);
        foo2BaseDir = touchFiles(basePath.resolve("foo2"), fooPath);
        barBaseDir = touchFiles(basePath.resolve("bar"), barPath);

    }

    @Test
    public void locateAbsentAbsent() {

        MCRHints hints = toHints(emptyBaseDir, empty2BaseDir);
        MCRResourceLocator locator = classLoaderLocator();

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void locateAbsentPresent() {

        MCRHints hints = toHints(emptyBaseDir, fooBaseDir);
        MCRResourceLocator locator = classLoaderLocator();

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertEquals(1, resourceUrls.size());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrls.get(0));

    }

    @Test
    public void locatePresentAbsent() {

        MCRHints hints = toHints(fooBaseDir, emptyBaseDir);
        MCRResourceLocator locator = classLoaderLocator();

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertEquals(1, resourceUrls.size());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrls.get(0));

    }

    @Test
    public void locatePresentPresent() {

        MCRHints hints = toHints(fooBaseDir, foo2BaseDir);
        MCRResourceLocator locator = classLoaderLocator();

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertEquals(2, resourceUrls.size());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrls.get(0));
        assertEquals(toUrl(foo2BaseDir, "foo"), resourceUrls.get(1));

    }

    @Test
    public void locateAbsentButNotEmptyPresent() {

        MCRHints hints = toHints(fooBaseDir, barBaseDir);
        MCRResourceLocator locator = classLoaderLocator();

        List<URL> resourceUrls = locator.locate(BAR_PATH, hints).toList();

        assertEquals(1, resourceUrls.size());
        assertEquals(toUrl(barBaseDir, "bar"), resourceUrls.get(0));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRClassLoaderResourceLocator.class)
    })
    public void configuration() {

        MCRHints hints = toHints(fooBaseDir);
        MCRResourceLocator locator = MCRConfiguration2.getInstanceOfOrThrow(
            MCRClassLoaderResourceLocator.class, "Test.Class");

        List<URL> fooResourceUrl = locator.locate(FOO_PATH, hints).toList();
        List<URL> barResourceUrl = locator.locate(BAR_PATH, hints).toList();

        assertEquals(1, fooResourceUrl.size());
        assertEquals(0, barResourceUrl.size());

    }

    private static MCRResourceLocator classLoaderLocator() {
        return new MCRClassLoaderResourceLocator();
    }

    private static MCRHints toHints(Path... baseDirs) {
        ClassLoader classLoader = new URLClassLoader("test", toUrls(baseDirs), PARENT_CLASS_LOADER);
        return new MCRHintsBuilder().add(MCRResourceHintKeys.CLASS_LOADER, classLoader).build();
    }

    private static URL[] toUrls(Path[] baseDirs) {
        URL[] urls = new URL[baseDirs.length];
        for (int i = 0; i < baseDirs.length; i++) {
            urls[i] = MCRResourceUtils.toFileUrl(baseDirs[i]);
        }
        return urls;
    }

    private URL toUrl(Path baseDir, String fileName) {
        return MCRResourceUtils.toFileUrl(baseDir.resolve(fileName));
    }

}
