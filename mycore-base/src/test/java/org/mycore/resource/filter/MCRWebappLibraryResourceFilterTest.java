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
public class MCRWebappLibraryResourceFilterTest {

    private static Path fooWebappDir;

    private static URL fileUrl;

    private static URL nonWebappLibraryUrl;

    private static URL webappLibraryUrl;

    private static List<URL> allResourceUrls;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirResourcesTestBasePath(MCRWebappLibraryResourceFilterTest.class);

        fooWebappDir = touchFiles(basePath.resolve("foo"));

        fileUrl = URI.create("file:/foo/bar").toURL();
        nonWebappLibraryUrl = URI.create("jar:file:/foo/library.jar!/foo/bar").toURL();
        webappLibraryUrl = URI.create("jar:" + fooWebappDir.toUri() + "WEB-INF/lib/library.jar!/foo/bar").toURL();

        allResourceUrls = List.of(fileUrl, nonWebappLibraryUrl, webappLibraryUrl);

    }

    @Test
    public void mustMatch() {

        MCRHints hints = toHints(fooWebappDir);
        MCRResourceFilter filter = webappLibraryFilter(MCRResourceFilterMode.MUST_MATCH);

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(webappLibraryUrl));

    }

    @Test
    public void mustNotMatch() {

        MCRHints hints = toHints(fooWebappDir);
        MCRResourceFilter filter = webappLibraryFilter(MCRResourceFilterMode.MUST_NOT_MATCH);

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), hints).toList();

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrl));
        assertTrue(resourceUrls.contains(nonWebappLibraryUrl));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRWebappLibraryResourceFilter.class),
        @MCRTestProperty(key = "Test.Mode", string = "MUST_MATCH")
    })
    public void configuration() {

        MCRHints hints = toHints(fooWebappDir);
        MCRResourceFilter filter = MCRConfiguration2.getInstanceOfOrThrow(
            MCRWebappLibraryResourceFilter.class, "Test.Class");

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(webappLibraryUrl));

    }

    private static MCRResourceFilter webappLibraryFilter(MCRResourceFilterMode mode) {
        return new MCRWebappLibraryResourceFilter(mode);
    }

    private static MCRHints toHints(Path webappDir) {
        return new MCRHintsBuilder().add(MCRResourceHintKeys.WEBAPP_DIR, webappDir).build();
    }

}
