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
import static org.mycore.resource.MCRFileSystemResourceHelper.getConfigDirResourcesTestBasePath;
import static org.mycore.resource.MCRFileSystemResourceHelper.touchFiles;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRLibraryResourceFilterTest {

    private static File fooConfigDir;

    private static URL fileUrl;

    private static URL libraryUrl;

    private static List<URL> allResourceUrls;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirResourcesTestBasePath(MCRLibraryResourceFilterTest.class);

        fooConfigDir = touchFiles(basePath.resolve("foo")).toFile();

        fileUrl = URI.create("file:/foo/bar").toURL();
        libraryUrl = URI.create("jar:file:/foo/library.jar!/foo/bar").toURL();

        allResourceUrls = List.of(fileUrl, libraryUrl);

    }

    @Test
    public void mustMatch() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceFilter filter = libraryFilter(MCRResourceFilterMode.MUST_MATCH);

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(libraryUrl));

    }

    @Test
    public void mustNotMatch() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceFilter filter = libraryFilter(MCRResourceFilterMode.MUST_NOT_MATCH);

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrl));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRLibraryResourceFilter.class),
        @MCRTestProperty(key = "Test.Mode", string = "MUST_MATCH")
    })
    public void configuration() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceFilter filter = MCRConfiguration2.getInstanceOfOrThrow(
            MCRLibraryResourceFilter.class, "Test.Class");

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(libraryUrl));

    }

    private static MCRLibraryResourceFilter libraryFilter(MCRResourceFilterMode mode) {
        return new MCRLibraryResourceFilter(mode);
    }

    private static MCRHints toHints(File configDir) {
        return new MCRHintsBuilder().add(MCRResourceHintKeys.CONFIG_DIR, configDir).build();
    }

}
