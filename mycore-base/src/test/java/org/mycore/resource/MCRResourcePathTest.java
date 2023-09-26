/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.resource;

import java.util.Optional;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class MCRResourcePathTest extends MCRTestCase {

    public static final Optional<Object> EMPTY = Optional.empty();

    @Test
    public void nullIsNoResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofPath((String) null));
    }

    @Test
    public void nullIsNoWebResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofWebPath(null));
    }

    @Test
    public void emptyStringIsNoResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofPath(""));
    }

    @Test
    public void emptyStringIsNoWebResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofWebPath(""));
    }

    @Test
    public void directoryIsNoResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofPath("/"));
        assertEquals(EMPTY, MCRResourcePath.ofPath("foo/"));
        assertEquals(EMPTY, MCRResourcePath.ofPath("foo/bar/"));
    }

    @Test
    public void directoryIsNoWebResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofWebPath("/"));
        assertEquals(EMPTY, MCRResourcePath.ofWebPath("foo/"));
        assertEquals(EMPTY, MCRResourcePath.ofWebPath("foo/bar/"));
    }

    @Test
    public void equalsResourcePath() {
        MCRResourcePath path1 = MCRResourcePath.ofPath("/foo/bar/baz").get();
        MCRResourcePath path2 = MCRResourcePath.ofPath("/foo/bar/baz").get();
        assertEquals(path1, path2);
    }

    @Test
    public void equalsWebResourcePath() {
        MCRResourcePath path1 = MCRResourcePath.ofWebPath("/foo/bar/baz").get();
        MCRResourcePath path2 = MCRResourcePath.ofWebPath("/foo/bar/baz").get();
        assertEquals(path1, path2);
    }

    @Test
    public void leadingSlashIsIrrelevantForResourcePath() {
        MCRResourcePath path2 = MCRResourcePath.ofPath("foo/bar/baz").get();
        MCRResourcePath path1 = MCRResourcePath.ofPath("/foo/bar/baz").get();
        assertEquals(path1, path2);
    }

    @Test
    public void leadingSlashIsIrrelevantForWebResourcePath() {
        MCRResourcePath path2 = MCRResourcePath.ofWebPath("foo/bar/baz").get();
        MCRResourcePath path1 = MCRResourcePath.ofWebPath("/foo/bar/baz").get();
        assertEquals(path1, path2);
    }


    @Test(expected = MCRResourceException.class)
    public void resourceCanNotHaveClassExtension() {
        MCRResourcePath.ofWebPath("/foo/bar/baz.class");
    }

    @Test(expected = MCRResourceException.class)
    public void resourceCanNotContainEmptySegment() {
        MCRResourcePath.ofPath("//foo/bar/baz");
    }

    @Test(expected = MCRResourceException.class)
    public void webResourceCanNotContainEmptySegment() {
        MCRResourcePath.ofWebPath("//foo/bar/baz");
    }

    @Test(expected = MCRResourceException.class)
    public void resourceCanNotContainContainSegmentLinkToSelf() {
        MCRResourcePath.ofPath("/./foo/bar/baz");
    }

    @Test(expected = MCRResourceException.class)
    public void webResourceCanNotContainSegmentLinkToSelf() {
        MCRResourcePath.ofWebPath("/./foo/bar/baz");
    }

    @Test(expected = MCRResourceException.class)
    public void resourceCanNotContainSegmentLinkToParent() {
        MCRResourcePath.ofPath("/../foo/bar/baz");
    }

    @Test(expected = MCRResourceException.class)
    public void webResourceCanNotContainSegmentLinkToParent() {
        MCRResourcePath.ofWebPath("/../foo/bar/baz");
    }

    @Test
    public void prefixDistinguishesResourcePathAndWebResourcePath() {
        MCRResourcePath path2 = MCRResourcePath.ofPath("/META-INF/resources/foo/bar/baz").get();
        MCRResourcePath path1 = MCRResourcePath.ofWebPath("/foo/bar/baz").get();
        assertEquals(path1, path2);
    }

    @Test(expected = MCRResourceException.class)
    public void webResourceCanNotHaveMetaInfPrefix() {
        MCRResourcePath.ofWebPath("/META-INF/foo/bar/baz");
    }

    @Test(expected = MCRResourceException.class)
    public void webResourceCanNotHaveWebInfPrefix() {
        MCRResourcePath.ofWebPath("/WEB-INF/foo/bar/baz");
    }

    @Test
    public void resourcePathWithoutPrefixAsResourcePathEqualsOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofPath("/foo/bar/baz").get();
        assertEquals("/foo/bar/baz", path.asAbsolutePath());
        assertEquals("foo/bar/baz", path.asRelativePath());
    }

    @Test
    public void resourcePathWithPrefixAsResourcePathEqualsOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofPath("/META-INF/resources/foo/bar/baz").get();
        assertEquals("/META-INF/resources/foo/bar/baz", path.asAbsolutePath());
        assertEquals("META-INF/resources/foo/bar/baz", path.asRelativePath());
    }

    @Test
    public void webResourcePathWithoutPrefixAsResourcePathAddsPrefixToOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofWebPath("/foo/bar/baz").get();
        assertEquals("/META-INF/resources/foo/bar/baz", path.asAbsolutePath());
        assertEquals("META-INF/resources/foo/bar/baz", path.asRelativePath());
    }

    @Test
    public void resourcePathWithoutPrefixAsWebResourcePathDoesNotExist() {
        MCRResourcePath path = MCRResourcePath.ofPath("/foo/bar/baz").get();
        assertEquals(EMPTY, path.asAbsoluteWebPath());
        assertEquals(EMPTY, path.asRelativeWebPath());
    }

    @Test
    public void resourcePathWithPrefixAsWebResourcePathRemovesPrefixFromOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofPath("/META-INF/resources/foo/bar/baz").get();
        assertEquals("/foo/bar/baz", path.asAbsoluteWebPath().get());
        assertEquals("foo/bar/baz", path.asRelativeWebPath().get());
    }

    @Test
    public void webResourcePathWithoutPrefixAsWebResourcePathEqualsOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofWebPath("/foo/bar/baz").get();
        assertEquals("/foo/bar/baz", path.asAbsoluteWebPath().get());
        assertEquals("foo/bar/baz", path.asRelativeWebPath().get());
    }

}
