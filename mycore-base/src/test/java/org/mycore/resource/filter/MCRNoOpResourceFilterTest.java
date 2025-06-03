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
public class MCRNoOpResourceFilterTest {

    private static URL fileUrlFoo;

    private static URL fileUrlBar;

    private static List<URL> allResourceUrls;

    @BeforeAll
    public static void prepare() throws IOException {

        fileUrlFoo = URI.create("file:/foo/bar/foo").toURL();
        fileUrlBar = URI.create("file:/foo/bar/bar").toURL();

        allResourceUrls = List.of(fileUrlFoo, fileUrlBar);

    }

    @Test
    public void noFiltering() {

        MCRResourceFilter filter = noOpFilter();

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), MCRHints.EMPTY).toList();

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrlFoo));
        assertTrue(resourceUrls.contains(fileUrlBar));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRNoOpResourceFilter.class)
    })
    public void configuration() {

        MCRResourceFilter filter = MCRConfiguration2.getInstanceOfOrThrow(
            MCRNoOpResourceFilter.class, "Test.Class");

        List<URL> resourceUrls = filter.filter(allResourceUrls.stream(), MCRHints.EMPTY).toList();

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileUrlFoo));
        assertTrue(resourceUrls.contains(fileUrlBar));

    }

    private static MCRResourceFilter noOpFilter() {
        return new MCRNoOpResourceFilter();
    }

}
