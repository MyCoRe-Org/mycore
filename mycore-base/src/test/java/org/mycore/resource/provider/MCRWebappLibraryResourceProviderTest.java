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
import static org.mycore.resource.MCRFileSystemResourceHelper.getConfigDirResourcesTestBasePath;
import static org.mycore.resource.MCRFileSystemResourceHelper.touchFiles;
import static org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.filter.MCRResourceFilterMode;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRWebappLibraryResourceProviderTest {

    private static final MCRResourcePath FOO_BAR_PATH = MCRResourcePath.ofPath("foo/bar").orElseThrow();

    private static final MCRResourcePath BAR_FOO_PATH = MCRResourcePath.ofPath("bar/foo").orElseThrow();

    private static File fooWebappDir;

    private static URL nonWebappLibraryUrl;

    private static URL webappLibraryUrl;

    private static List<URL> allResourceUrls;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirResourcesTestBasePath(MCRWebappLibraryResourceProviderTest.class);

        fooWebappDir = touchFiles(basePath.resolve("foo")).toFile();

        URL fileUrl = URI.create("file:/foo/bar").toURL();
        nonWebappLibraryUrl = URI.create("jar:file:/foo/library.jar!/foo/bar").toURL();
        webappLibraryUrl = URI.create("jar:file:" + fooWebappDir + "/WEB-INF/lib/library.jar!/foo/bar").toURL();

        allResourceUrls = List.of(fileUrl, nonWebappLibraryUrl, webappLibraryUrl);

    }

    @Test
    public void provideMustMatch() throws IOException {

        MCRHints hints = toHints(fooWebappDir, allResourceUrls);
        MCRResourceProvider provider = webappLibraryProvider(MCRResourceFilterMode.MUST_MATCH);

        Optional<URL> resourceUrl = provider.provide(FOO_BAR_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(webappLibraryUrl, resourceUrl.get());

    }

    @Test
    public void provideMustNotMatch() throws IOException {

        MCRHints hints = toHints(fooWebappDir, allResourceUrls);
        MCRResourceProvider provider = webappLibraryProvider(MCRResourceFilterMode.MUST_NOT_MATCH);

        Optional<URL> resourceUrl = provider.provide(FOO_BAR_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(nonWebappLibraryUrl, resourceUrl.get());

    }

    @Test
    public void provideAllMustMatch() throws IOException {

        MCRHints hints = toHints(fooWebappDir, allResourceUrls);
        MCRResourceProvider provider = webappLibraryProvider(MCRResourceFilterMode.MUST_MATCH);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(webappLibraryUrl));

    }

    @Test
    public void provideAllMustNotMatch() throws IOException {

        MCRHints hints = toHints(fooWebappDir, allResourceUrls);
        MCRResourceProvider provider = webappLibraryProvider(MCRResourceFilterMode.MUST_NOT_MATCH);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(nonWebappLibraryUrl));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRWebappLibraryResourceProvider.class),
        @MCRTestProperty(key = "Test.Mode", string = "MUST_MATCH")
    })
    public void configuration() throws IOException {

        MCRHints hints = toHints(fooWebappDir, allResourceUrls);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRWebappLibraryResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_BAR_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(BAR_FOO_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isEmpty());

    }

    private static MCRResourceProvider webappLibraryProvider(MCRResourceFilterMode mode) {
        return new MCRWebappLibraryResourceProvider("webapp library test", mode);
    }

    private static MCRHints toHints(File webappDir, List<URL> resourceUrls) throws IOException {
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(classLoader.getResources(Mockito.anyString())).thenReturn(Collections.enumeration(resourceUrls));
        return new MCRHintsBuilder().add(MCRResourceHintKeys.WEBAPP_DIR, webappDir)
            .add(MCRResourceHintKeys.CLASS_LOADER, classLoader).build();
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

}
