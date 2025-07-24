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
import static org.mycore.resource.common.MCRResourceUtils.toFileUrl;
import static org.mycore.resource.common.MCRResourceUtils.toJarFileUrl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRHighestComponentPriorityResourceSelectorTest {

    private static final ComponentInfo COMPONENT_25_1 =
        new ComponentInfo("component-25-1", 50, new File("/foo/component-25-1.jar"));

    private static final ComponentInfo COMPONENT_25_2 =
        new ComponentInfo("component-25-2", 50, new File("/foo/component-25-2.jar"));

    private static final ComponentInfo COMPONENT_50_1 =
        new ComponentInfo("component-50-1", 50, new File("/foo/component-50-1.jar"));

    private static final ComponentInfo COMPONENT_50_2 =
        new ComponentInfo("component-50-2", 50, new File("/foo/component-50-2.jar"));

    private static final ComponentInfo COMPONENT_75_1 =
        new ComponentInfo("component-75-1", 50, new File("/foo/component-75-1.jar"));

    private static final ComponentInfo COMPONENT_75_2 =
        new ComponentInfo("component-75-2", 50, new File("/foo/component-75-2.jar"));

    private static URL fileResourceUrl;

    private static URL component251ResourceUrl;

    private static URL component252ResourceUrl;

    private static URL component501ResourceUrl;

    private static URL component502ResourceUrl;

    private static URL component751ResourceUrl;

    private static URL component752ResourceUrl;

    private static URL otherComponent751ResourceUrl;

    private static URL otherLibraryResourceUrl;

    @BeforeAll
    public static void prepare() throws IOException {

        fileResourceUrl = toFileUrl("/foo/bar");
        component251ResourceUrl = toJarFileUrl("/foo/component-50-1.jar", "/foo/bar");
        component252ResourceUrl = toJarFileUrl("/foo/component-50-2.jar", "/foo/bar");
        component501ResourceUrl = toJarFileUrl("/foo/component-50-1.jar", "/foo/bar");
        component502ResourceUrl = toJarFileUrl("/foo/component-50-2.jar", "/foo/bar");
        component751ResourceUrl = toJarFileUrl("/foo/component-50-1.jar", "/foo/bar");
        component752ResourceUrl = toJarFileUrl("/foo/component-50-2.jar", "/foo/bar");
        otherComponent751ResourceUrl = toJarFileUrl("/bar/component-75-1.jar", "/foo/bar");
        otherLibraryResourceUrl = toJarFileUrl("/foo/library.jar", "/foo/bar");

    }

    @Test
    public void nonMatchingUrl() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = highestComponentPriority();

        // no selection can be made (resource URL not part of the libraries) ...
        List<URL> resourceUrls = selector.select(List.of(fileResourceUrl), hints);

        // ... expect all resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileResourceUrl));

    }

    @Test
    public void nonMatchingUrls() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = highestComponentPriority();

        // no selection can be made (resource URLs not part of the libraries) ...
        List<URL> resourceUrls = selector.select(List.of(fileResourceUrl, otherLibraryResourceUrl), hints);

        // ... expect all resource URLs
        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(fileResourceUrl));
        assertTrue(resourceUrls.contains(otherLibraryResourceUrl));

    }

    @Test
    public void nonMatchingUrlWithSameNameAsServletLibrary() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = highestComponentPriority();

        // no selection can be made (resource URL not part of the libraries) ...
        List<URL> resourceUrls = selector.select(List.of(otherComponent751ResourceUrl), hints);

        // ... expect all resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(otherComponent751ResourceUrl));

    }

    @Test
    public void matchingUrl() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = highestComponentPriority();

        // selection can be made (first resource URL part of first library) ...
        List<URL> resourceUrls = selector.select(List.of(component751ResourceUrl, otherLibraryResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(component751ResourceUrl));

    }

    @Test
    public void lowerPriorityMatchingUrl() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = highestComponentPriority();

        // selection can be made (second resource URL part of second library) ...
        List<URL> resourceUrls = selector.select(List.of(otherLibraryResourceUrl, component501ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(component501ResourceUrl));

    }

    @Test
    public void multipleMatchingUrls() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = highestComponentPriority();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(component751ResourceUrl, component501ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(component751ResourceUrl));

    }

    @Test
    public void multipleMatchingUrls2() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = highestComponentPriority();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(component251ResourceUrl, component501ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(component501ResourceUrl));

    }

    @Test
    public void multipleMatchingMultipleUrls() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_75_2,
            COMPONENT_50_1, COMPONENT_50_2, COMPONENT_75_1, COMPONENT_25_2);
        MCRResourceSelector selector = highestComponentPriority();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(component751ResourceUrl, component752ResourceUrl,
            component501ResourceUrl, component502ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(component751ResourceUrl));
        assertTrue(resourceUrls.contains(component752ResourceUrl));

    }

    @Test
    public void multipleMatchingMultipleUrls2() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_75_2,
            COMPONENT_50_1, COMPONENT_50_2, COMPONENT_75_1, COMPONENT_25_2);
        MCRResourceSelector selector = highestComponentPriority();

        // selection can be made (both resource URLs part of libraries) ...
        List<URL> resourceUrls = selector.select(List.of(component252ResourceUrl, component251ResourceUrl,
            component502ResourceUrl, component501ResourceUrl), hints);

        // ... expect specific resource URLs
        assertEquals(2, resourceUrls.size());
        assertTrue(resourceUrls.contains(component501ResourceUrl));
        assertTrue(resourceUrls.contains(component502ResourceUrl));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRHighestComponentPriorityResourceSelector.class)
    })
    public void configuration() {

        MCRHints hints = toHints(COMPONENT_25_1, COMPONENT_50_1, COMPONENT_75_1);
        MCRResourceSelector selector = MCRConfiguration2.getInstanceOfOrThrow(
            MCRHighestComponentPriorityResourceSelector.class, "Test.Class");

        List<URL> resourceUrls = selector.select(List.of(component501ResourceUrl), hints);

        assertEquals(1, resourceUrls.size());
        assertTrue(resourceUrls.contains(component501ResourceUrl));

    }

    private static MCRResourceSelector highestComponentPriority() {
        return new MCRHighestComponentPriorityResourceSelector();
    }

    private static MCRHints toHints(ComponentInfo... componentInfos) {

        Comparator<MCRComponent> comparator = Comparator.comparing(MCRComponent::getPriority)
            .thenComparing(MCRComponent::getName).reversed();

        TreeSet<MCRComponent> components = new TreeSet<>(comparator);
        for (ComponentInfo componentInfo : componentInfos) {
            MCRComponent component = Mockito.mock(MCRComponent.class);
            Mockito.when(component.getName()).thenReturn(componentInfo.name());
            Mockito.when(component.getPriority()).thenReturn(componentInfo.priority());
            Mockito.when(component.getJarFile()).thenReturn(componentInfo.jarFile());
            Mockito.when(component.toString()).thenReturn(componentInfo.name());
            components.add(component);
        }

        return new MCRHintsBuilder().add(MCRResourceHintKeys.COMPONENTS, components).build();

    }

    private record ComponentInfo(String name, int priority, File jarFile) {
    }

}
