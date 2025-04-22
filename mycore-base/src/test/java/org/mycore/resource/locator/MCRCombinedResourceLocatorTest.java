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

package org.mycore.resource.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTestUrlConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRSyntheticResourceSpec;
import org.mycore.test.MCRTestUrlExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRTestUrlExtension.class)
@MCRTestUrlConfiguration(protocols = "test2")
public class MCRCombinedResourceLocatorTest {

    private static final MCRResourcePath FOO_PATH = MCRResourcePath.ofPath("foo").orElseThrow();

    private static final MCRResourcePath BAR_PATH = MCRResourcePath.ofPath("bar").orElseThrow();

    private static final MCRResourcePath BAZ_PATH = MCRResourcePath.ofPath("baz").orElseThrow();

    private static final MCRSyntheticResourceSpec FOO_SPEC = new MCRSyntheticResourceSpec("test:", FOO_PATH);

    private static final MCRSyntheticResourceSpec FOO2_SPEC = new MCRSyntheticResourceSpec("test2:", FOO_PATH);

    private static final MCRSyntheticResourceSpec BAR_SPEC = new MCRSyntheticResourceSpec("test2:", BAR_PATH);

    @Test
    public void locateAbsentAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceLocator locator = combinedLocator(Collections.emptyList(), Collections.emptyList());

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertTrue(resourceUrls.isEmpty());

    }

    @Test
    public void locateAbsentPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceLocator locator = combinedLocator(Collections.emptyList(), specs);

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));

    }

    @Test
    public void locatePresentAbsent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs = List.of(FOO_SPEC);
        MCRResourceLocator locator = combinedLocator(specs, Collections.emptyList());

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));

    }

    @Test
    public void locatePresentPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs1 = List.of(FOO_SPEC);
        List<MCRSyntheticResourceSpec> specs2 = List.of(FOO2_SPEC);
        MCRResourceLocator locator = combinedLocator(specs1, specs2);

        List<URL> resourceUrls = locator.locate(FOO_PATH, hints).toList();

        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(FOO_SPEC.toUrl(factory)));
        assertTrue(resourceUrls.contains(FOO2_SPEC.toUrl(factory)));

    }

    @Test
    public void locateAbsentButNotEmptyPresent(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        List<MCRSyntheticResourceSpec> specs1 = List.of(FOO_SPEC);
        List<MCRSyntheticResourceSpec> specs2 = List.of(BAR_SPEC);
        MCRResourceLocator locator = combinedLocator(specs1, specs2);

        List<URL> resourceUrls = locator.locate(BAR_PATH, hints).toList();

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(BAR_SPEC.toUrl(factory)));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRCombinedResourceLocator.class),
        @MCRTestProperty(key = "Test.Locators.1.Class", classNameOf = MCRSyntheticResourceLocator.class),
        @MCRTestProperty(key = "Test.Locators.1.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Locators.1.Specs.1.Path", string = "foo"),
        @MCRTestProperty(key = "Test.Locators.2.Class", classNameOf = MCRSyntheticResourceLocator.class),
        @MCRTestProperty(key = "Test.Locators.2.Specs.1.Prefix", string = "test:"),
        @MCRTestProperty(key = "Test.Locators.2.Specs.1.Path", string = "bar")
    })
    public void configuration(URLStreamHandlerFactory factory) {

        MCRHints hints = toHints(factory);
        MCRResourceLocator locator = MCRConfiguration2.getInstanceOfOrThrow(
            MCRCombinedResourceLocator.class, "Test.Class");

        List<URL> fooResourceUrl = locator.locate(FOO_PATH, hints).toList();
        List<URL> barResourceUrl = locator.locate(BAR_PATH, hints).toList();
        List<URL> bazResourceUrl = locator.locate(BAZ_PATH, hints).toList();

        assertEquals(1, fooResourceUrl.size());
        assertEquals(1, barResourceUrl.size());
        assertEquals(0, bazResourceUrl.size());

    }

    private static MCRResourceLocator combinedLocator(List<MCRSyntheticResourceSpec> specs1,
        List<MCRSyntheticResourceSpec> specs2) {
        return new MCRCombinedResourceLocator(syntheticLocator(specs1), syntheticLocator(specs2));
    }

    private static MCRResourceLocator syntheticLocator(List<MCRSyntheticResourceSpec> specs) {
        return new MCRSyntheticResourceLocator(specs);
    }

    private static MCRHints toHints(URLStreamHandlerFactory factory) {
        return new MCRHintsBuilder().add(MCRSyntheticResourceSpec.URL_STREAM_HANDLER_FACTORY, factory).build();
    }

}
