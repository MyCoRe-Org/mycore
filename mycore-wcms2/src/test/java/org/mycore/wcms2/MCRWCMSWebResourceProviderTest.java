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

package org.mycore.wcms2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.resource.MCRFileSystemResourceHelper.getConfigDirTestBasePath;
import static org.mycore.resource.MCRFileSystemResourceHelper.touchFiles;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.mycore.resource.provider.MCRResourceProvider;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRWCMSWebResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath WEB_FOO_PATH = MCRResourcePath.ofWebPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath WEB_BAR_PATH = MCRResourcePath.ofWebPath("bar").orElseThrow();

    private static Path fooWcmsDataDir;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirTestBasePath(MCRWCMSWebResourceProviderTest.class);

        Path fooPath = Paths.get("foo");

        fooWcmsDataDir = touchFiles(basePath.resolve("foo"), fooPath);

    }

    @Test
    public void provideAbsent() {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideWebAbsent() {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        Optional<URL> resourceUrl = provider.provide(WEB_BAR_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void providePresent() {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void provideWebPresent() throws MalformedURLException {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        Optional<URL> resourceUrl = provider.provide(WEB_FOO_PATH, MCRHints.EMPTY);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toUrl(fooWcmsDataDir, "foo"), resourceUrl.get());

    }

    @Test
    public void provideAllAbsent() {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllWebAbsent() {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(WEB_BAR_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllPresent() {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllWebPresent() {

        MCRResourceProvider provider = wcmsWebProvider(fooWcmsDataDir);

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(WEB_FOO_PATH, MCRHints.EMPTY);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toUrl(fooWcmsDataDir, "foo")));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRWCMSWebResourceProvider.class)
    })
    public void configuration() {

        MCRConfiguration2.set("MCR.WCMS2.DataDir", fooWcmsDataDir.toAbsolutePath().toString());
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRWCMSWebResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(WEB_FOO_PATH, MCRHints.EMPTY);
        Optional<URL> barResourceUrl = provider.provide(WEB_BAR_PATH, MCRHints.EMPTY);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isEmpty());

    }

    private static MCRResourceProvider wcmsWebProvider(Path wcmsDataDir) {
        MCRConfiguration2.set("MCR.WCMS2.DataDir", wcmsDataDir.toAbsolutePath().toString());
        return new MCRWCMSWebResourceProvider("wcms test");
    }

    private URL toUrl(Path webappDir, String fileName) {
        return MCRResourceUtils.toFileUrl(webappDir.resolve(fileName));
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

    private static MCRHints toHints(Path webappDir) {
        return new MCRHintsBuilder().add(MCRResourceHintKeys.WEBAPP_DIR, webappDir).build();
    }

}
