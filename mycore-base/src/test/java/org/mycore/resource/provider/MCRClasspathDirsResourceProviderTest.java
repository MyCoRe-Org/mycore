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
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceUtils;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRClasspathDirsResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath BAZ_PATH = MCRResourcePath.ofPath("baz").orElseThrow();

    private static Path emptyBaseDir;

    private static Path empty2BaseDir;

    private static Path fooBaseDir;

    private static Path foo2BaseDir;

    private static Path barBaseDir;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirTestBasePath(MCRClasspathDirsResourceProviderTest.class);

        Path fooPath = Path.of("foo");
        Path barPath = Path.of("bar");

        emptyBaseDir = touchFiles(basePath.resolve("empty"));
        empty2BaseDir = touchFiles(basePath.resolve("empty2"));
        fooBaseDir = touchFiles(basePath.resolve("foo"), fooPath);
        foo2BaseDir = touchFiles(basePath.resolve("foo2"), fooPath);
        barBaseDir = touchFiles(basePath.resolve("bar"), barPath);

    }

    @Test
    public void provideAbsentAbsent() {

        MCRHints hints = toHints(emptyBaseDir, empty2BaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideAbsentPresent() {

        MCRHints hints = toHints(emptyBaseDir, fooBaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void providePresentAbsent() {

        MCRHints hints = toHints(fooBaseDir, emptyBaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void providePresentPresent() {

        MCRHints hints = toHints(fooBaseDir, foo2BaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void provideAbsentButNotEmptyPresent() {

        MCRHints hints = toHints(fooBaseDir, barBaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(barBaseDir, "bar"), resourceUrl.get());

    }

    @Test
    public void provideAllAbsentAbsent() {

        MCRHints hints = toHints(emptyBaseDir, emptyBaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllAbsentPresent() {

        MCRHints hints = toHints(emptyBaseDir, fooBaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));

    }

    @Test
    public void provideAllPresentAbsent() {

        MCRHints hints = toHints(fooBaseDir, emptyBaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));

    }

    @Test
    public void provideAllPresentPresent() {

        MCRHints hints = toHints(fooBaseDir, foo2BaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));
        assertTrue(resourceUrls.contains(toUrl(foo2BaseDir, "foo")));

    }

    @Test
    public void provideAllAbsentButNotEmptyPresent() {

        MCRHints hints = toHints(fooBaseDir, barBaseDir);
        MCRResourceProvider provider = classpathDirsProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(barBaseDir, "bar")));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRClasspathDirsResourceProvider.class)
    })
    public void configuration() {

        MCRHints hints = toHints(fooBaseDir, barBaseDir);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRClasspathDirsResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, hints);
        Optional<URL> bazResourceUrl = provider.provide(BAZ_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isPresent());
        assertTrue(bazResourceUrl.isEmpty());

    }

    private static MCRResourceProvider classpathDirsProvider() {
        return new MCRClasspathDirsResourceProvider("classpath dirs");
    }

    private MCRHints toHints(Path... baseDirs) {
        return new MCRHintsBuilder()
            .add(MCRResourceHintKeys.CLASSPATH_DIRS_PROVIDER, hints -> List.of(baseDirs))
            .build();
    }

    private URL toUrl(Path baseDir, String fileName) {
        return MCRResourceUtils.toFileUrl(baseDir.resolve(fileName));
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

}
