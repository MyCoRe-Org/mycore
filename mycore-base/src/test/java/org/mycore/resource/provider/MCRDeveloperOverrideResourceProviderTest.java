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
import static org.mycore.resource.provider.MCRDeveloperOverrideResourceProvider.DEVELOPER_RESOURCE_OVERRIDE_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
public class MCRDeveloperOverrideResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath BAZ_PATH = MCRResourcePath.ofPath("baz").orElseThrow();

    private static File emptyBaseDir;

    private static File empty2BaseDir;

    private static File fooBaseDir;

    private static File foo2BaseDir;

    private static File barBaseDir;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirTestBasePath(MCRDeveloperOverrideResourceProviderTest.class);

        Path fooPath = Paths.get("foo");
        Path barPath = Paths.get("bar");

        emptyBaseDir = touchFiles(basePath.resolve("empty")).toFile();
        empty2BaseDir = touchFiles(basePath.resolve("empty2")).toFile();
        fooBaseDir = touchFiles(basePath.resolve("foo"), fooPath).toFile();
        foo2BaseDir = touchFiles(basePath.resolve("foo2"), fooPath).toFile();
        barBaseDir = touchFiles(basePath.resolve("bar"), barPath).toFile();

    }

    @Test
    public void provideAbsentAbsent() {

        MCRResourceProvider provider = developerOverrideProvider(emptyBaseDir, empty2BaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideAbsentPresent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(emptyBaseDir, fooBaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void providePresentAbsent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(fooBaseDir, emptyBaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void providePresentPresent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(fooBaseDir, foo2BaseDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooBaseDir, "foo"), resourceUrl.get());

    }

    @Test
    public void provideAbsentButNotEmptyPresent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(fooBaseDir, barBaseDir);

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(barBaseDir, "bar"), resourceUrl.get());

    }

    @Test
    public void provideAllAbsentAbsent() {

        MCRResourceProvider provider = developerOverrideProvider(emptyBaseDir, emptyBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllAbsentPresent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(emptyBaseDir, fooBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));

    }

    @Test
    public void provideAllPresentAbsent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(fooBaseDir, emptyBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));

    }

    @Test
    public void provideAllPresentPresent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(fooBaseDir, foo2BaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooBaseDir, "foo")));
        assertTrue(resourceUrls.contains(toUrl(foo2BaseDir, "foo")));

    }

    @Test
    public void provideAllAbsentButNotEmptyPresent() throws MalformedURLException {

        MCRResourceProvider provider = developerOverrideProvider(fooBaseDir, barBaseDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(barBaseDir, "bar")));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRDeveloperOverrideResourceProvider.class),
        @MCRTestProperty(key = "Test.Mode", string = "RESOURCES")
    })
    public void configuration() {

        MCRConfiguration2.set(DEVELOPER_RESOURCE_OVERRIDE_PROPERTY,
            fooBaseDir.getAbsolutePath() + "," + barBaseDir.getAbsolutePath());

        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRDeveloperOverrideResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, MCRHints.EMPTY);
        Optional<URL> bazResourceUrl = provider.provide(BAZ_PATH, MCRHints.EMPTY);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isPresent());
        assertTrue(bazResourceUrl.isEmpty());

    }

    private static MCRResourceProvider developerOverrideProvider(File... baseDirs) {
        MCRConfiguration2.set(DEVELOPER_RESOURCE_OVERRIDE_PROPERTY,
            Arrays.stream(baseDirs).map(File::getAbsolutePath).collect(Collectors.joining(",")));
        return new MCRDeveloperOverrideResourceProvider("developer override test");
    }

    private URL toUrl(File baseDir, String fileName) throws MalformedURLException {
        return new File(baseDir, fileName).toURI().toURL();
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

}
