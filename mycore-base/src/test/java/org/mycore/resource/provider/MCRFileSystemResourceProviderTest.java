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
import static org.mycore.resource.MCRFileSystemResourceHelper.getConfigDirTestBasePath;
import static org.mycore.resource.MCRFileSystemResourceHelper.touchFiles;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRFileSystemResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath WEB_FOO_PATH = MCRResourcePath.ofWebPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath BAZ_PATH = MCRResourcePath.ofPath("baz").orElseThrow();

    private static Path emptyBaseDir;

    private static Path empty2BaseDir;

    private static Path fooBaseDir;

    private static Path foo2BaseDir;

    private static Path webFooBaseDir;

    private static Path barBaseDir;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirTestBasePath(MCRFileSystemResourceProviderTest.class);

        Path fooPath = Path.of("foo");
        Path webFooPath = Path.of("META-INF/resources/foo");
        Path barPath = Path.of("bar");

        emptyBaseDir = touchFiles(basePath.resolve("empty"));
        empty2BaseDir = touchFiles(basePath.resolve("empty2"));
        fooBaseDir = touchFiles(basePath.resolve("foo"), fooPath);
        foo2BaseDir = touchFiles(basePath.resolve("foo2"), fooPath);
        webFooBaseDir = touchFiles(basePath.resolve("webFoo"), webFooPath);
        barBaseDir = touchFiles(basePath.resolve("bar"), barPath);

    }

    @Test
    public void provideAbsentAbsent() {

        MCRResourceProvider provider = fileSystemProvider(emptyBaseDir, empty2BaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideAbsentPresent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(emptyBaseDir, fooBaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void providePresentAbsent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(fooBaseDir, emptyBaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void providePresentPresent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(fooBaseDir, foo2BaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void provideAbsentButNotEmptyPresent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(fooBaseDir, barBaseDir);

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(barBaseDir, "bar"), resourceUrl.get());

    }

    @Test
    public void provideAllAbsentAbsent() {

        MCRResourceProvider provider = fileSystemProvider(emptyBaseDir, emptyBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllAbsentPresent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(emptyBaseDir, fooBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));

    }

    @Test
    public void provideAllPresentAbsent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(fooBaseDir, emptyBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));

    }

    @Test
    public void provideAllPresentPresent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(fooBaseDir, foo2BaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));
        assertTrue(resourceUrls.contains(toUrl(foo2BaseDir, "foo")));

    }

    @Test
    public void provideAllAbsentButNotEmptyPresent() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(fooBaseDir, barBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(barBaseDir, "bar")));

    }

    @Test
    public void provideResourceFromResourceBaseDir() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(MCRResourceProviderMode.RESOURCES, fooBaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void provideWebResourceFromResourceBaseDir() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(MCRResourceProviderMode.RESOURCES, webFooBaseDir);

        Optional<URL> resourceUrl = provider.provide(WEB_FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(webFooBaseDir, "META-INF/resources/foo"), resourceUrl.get());

    }

    @Test
    public void provideWebResourceFromWebResourceBaseDir() throws MalformedURLException {

        MCRResourceProvider provider = fileSystemProvider(MCRResourceProviderMode.WEB_RESOURCES, fooBaseDir);

        Optional<URL> resourceUrl = provider.provide(WEB_FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRFileSystemResourceProvider.class),
        @MCRTestProperty(key = "Test.Mode", string = "RESOURCES")
    })
    public void configuration() {

        MCRConfiguration2.set("Test.BaseDirs", fooBaseDir.toAbsolutePath() + "," + barBaseDir.toAbsolutePath());

        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRFileSystemResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, MCRHints.EMPTY);
        Optional<URL> bazResourceUrl = provider.provide(BAZ_PATH, MCRHints.EMPTY);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isPresent());
        assertTrue(bazResourceUrl.isEmpty());

    }

    private static MCRResourceProvider fileSystemProvider(MCRResourceProviderMode mode, Path baseDir) {
        return new MCRFileSystemResourceProvider("file system test", mode, List.of(baseDir));
    }

    private static MCRResourceProvider fileSystemProvider(Path... baseDirs) {
        return new MCRFileSystemResourceProvider("file system test", MCRResourceProviderMode.RESOURCES, baseDirs);
    }

    private URL toUrl(Path baseDir, String fileName) throws MalformedURLException {
        return baseDir.resolve(fileName).toUri().toURL();
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

}
