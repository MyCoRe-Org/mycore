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

package org.mycore.resource.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRNoOpResourceSelectorTest {

    private static URL fooBarResourceUrl;

    private static URL barFooResourceUrl;

    private static List<URL> allResourceUrls;

    @BeforeAll
    public static void prepare() throws IOException {

        fooBarResourceUrl = URI.create("file:/foo/bar").toURL();
        barFooResourceUrl = URI.create("file:/bar/foo").toURL();

        allResourceUrls = List.of(fooBarResourceUrl, barFooResourceUrl);

    }

    @Test
    public void noSelecting() {

        MCRResourceSelector selector = noOpSelector();

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooBarResourceUrl));
        assertTrue(resourceUrls.contains(barFooResourceUrl));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRNoOpResourceSelector.class)
    })
    public void configuration() {

        MCRResourceSelector selector = MCRConfiguration2.getInstanceOfOrThrow(
            MCRNoOpResourceSelector.class, "Test.Class");

        List<URL> resourceUrls = selector.select(allResourceUrls, MCRHints.EMPTY);

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fooBarResourceUrl));
        assertTrue(resourceUrls.contains(barFooResourceUrl));

    }

    private static MCRResourceSelector noOpSelector() {
        return new MCRNoOpResourceSelector();
    }

}
