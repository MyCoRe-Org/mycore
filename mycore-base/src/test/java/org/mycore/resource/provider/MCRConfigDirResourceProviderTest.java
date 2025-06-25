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
public class MCRConfigDirResourceProviderTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static Path fooConfigDir;

    @BeforeAll
    public static void prepare() throws IOException {

        Path basePath = getConfigDirResourcesTestBasePath(MCRConfigDirResourceProviderTest.class);

        Path fooPath = Path.of("resources/foo");

        fooConfigDir = touchFiles(basePath.resolve("foo"), fooPath);

    }

    @Test
    public void provideAbsent() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceProvider provider = configDirProvider();

        Optional<URL> resourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(resourceUrl.isEmpty());

    }

    @Test
    public void providePresent() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceProvider provider = configDirProvider();

        Optional<URL> resourceUrl = provider.provide(FOO_PATH, hints);

        assertTrue(resourceUrl.isPresent());
        assertEquals(toResourcesUrl(fooConfigDir, "foo"), resourceUrl.get());

    }

    @Test
    public void provideAllAbsent() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceProvider provider = configDirProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(BAR_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void provideAllPresent() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceProvider provider = configDirProvider();

        List<ProvidedUrl> providedResourceUrls = provider.provideAll(FOO_PATH, hints);
        List<URL> resourceUrls = toUrlList(providedResourceUrls);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(toResourcesUrl(fooConfigDir, "foo")));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRConfigDirResourceProvider.class)
    })
    public void configuration() {

        MCRHints hints = toHints(fooConfigDir);
        MCRResourceProvider provider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRConfigDirResourceProvider.class, "Test.Class");

        Optional<URL> fooResourceUrl = provider.provide(FOO_PATH, hints);
        Optional<URL> barResourceUrl = provider.provide(BAR_PATH, hints);

        assertTrue(fooResourceUrl.isPresent());
        assertTrue(barResourceUrl.isEmpty());

    }

    private static MCRResourceProvider configDirProvider() {
        return new MCRConfigDirResourceProvider("config dir test");
    }

    private URL toResourcesUrl(Path configDir, String fileName) {
        return MCRResourceUtils.toFileUrl(configDir.resolve("resources").resolve(fileName));
    }

    private static List<URL> toUrlList(List<ProvidedUrl> providedUrls) {
        return providedUrls.stream().map(ProvidedUrl::url).toList();
    }

    private static MCRHints toHints(Path configDir) {
        return new MCRHintsBuilder().add(MCRResourceHintKeys.CONFIG_DIR, configDir).build();
    }

}
