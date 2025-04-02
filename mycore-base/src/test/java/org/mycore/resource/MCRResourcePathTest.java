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

package org.mycore.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRResourcePathTest {

    public static final Optional<Object> EMPTY = Optional.empty();

    @Test
    @SuppressWarnings("OptionalAssignedToNull")
    public void nullIsNoOptionalResourcePath() {
        assertThrows(NullPointerException.class, () -> MCRResourcePath.ofPath((Optional<String>) null));
    }

    @Test
    public void nullIsNoResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofPath((String) null));
    }

    @Test
    public void nullIsNoResourcePathOrThrow() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.MISSING_OR_EMPTY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPathOrThrow(null)).code);
    }

    @Test
    @SuppressWarnings("OptionalAssignedToNull")
    public void nullIsNoOptionalWebResourcePath() {
        assertThrows(NullPointerException.class, () -> MCRResourcePath.ofWebPath((Optional<String>) null));
    }

    @Test
    public void nullIsNoWebResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofWebPath((String) null));
    }

    @Test
    public void nullIsNoWebResourcePathOrThrow() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.MISSING_OR_EMPTY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPathOrThrow(null)).code);
    }

    @Test
    public void emptyStringIsNoResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofPath(""));
    }

    @Test
    public void emptyStringIsNoResourcePathOrThrow() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.MISSING_OR_EMPTY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPathOrThrow("")).code);
    }

    @Test
    public void emptyStringIsNoWebResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofWebPath(""));
    }

    @Test
    public void emptyStringIsNoWebResourcePathOrThrow() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.MISSING_OR_EMPTY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPathOrThrow("")).code);
    }

    @Test
    public void directoryIsNoResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofPath("/"));
        assertEquals(EMPTY, MCRResourcePath.ofPath("foo/"));
        assertEquals(EMPTY, MCRResourcePath.ofPath("foo/bar/"));
    }

    @Test
    public void directoryIsNoResourcePathOrThrow() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.DIRECTORY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPathOrThrow("/")).code);
        assertEquals(MCRResourcePath.IllegalPathException.Code.DIRECTORY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPathOrThrow("foo/")).code);
        assertEquals(MCRResourcePath.IllegalPathException.Code.DIRECTORY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPathOrThrow("foo/bar/")).code);
    }

    @Test
    public void directoryIsNoWebResourcePath() {
        assertEquals(EMPTY, MCRResourcePath.ofWebPath("/"));
        assertEquals(EMPTY, MCRResourcePath.ofWebPath("foo/"));
        assertEquals(EMPTY, MCRResourcePath.ofWebPath("foo/bar/"));
    }

    @Test
    public void directoryIsNoWebResourcePathOrThrow() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.DIRECTORY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPathOrThrow("/")).code);
        assertEquals(MCRResourcePath.IllegalPathException.Code.DIRECTORY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPathOrThrow("foo/")).code);
        assertEquals(MCRResourcePath.IllegalPathException.Code.DIRECTORY_PATH,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPathOrThrow("foo/bar/")).code);
    }

    @Test
    public void equalsResourcePath() {
        MCRResourcePath path1 = MCRResourcePath.ofPath("/foo/bar/baz").orElseThrow();
        MCRResourcePath path2 = MCRResourcePath.ofPath("/foo/bar/baz").orElseThrow();
        assertEquals(path1, path2);
    }

    @Test
    public void equalsWebResourcePath() {
        MCRResourcePath path1 = MCRResourcePath.ofWebPath("/foo/bar/baz").orElseThrow();
        MCRResourcePath path2 = MCRResourcePath.ofWebPath("/foo/bar/baz").orElseThrow();
        assertEquals(path1, path2);
    }

    @Test
    public void leadingSlashIsIrrelevantForResourcePath() {
        MCRResourcePath path2 = MCRResourcePath.ofPath("foo/bar/baz").orElseThrow();
        MCRResourcePath path1 = MCRResourcePath.ofPath("/foo/bar/baz").orElseThrow();
        assertEquals(path1, path2);
    }

    @Test
    public void leadingSlashIsIrrelevantForWebResourcePath() {
        MCRResourcePath path2 = MCRResourcePath.ofWebPath("foo/bar/baz").orElseThrow();
        MCRResourcePath path1 = MCRResourcePath.ofWebPath("/foo/bar/baz").orElseThrow();
        assertEquals(path1, path2);
    }

    @Test
    public void resourceCanNotHaveClassExtension() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_POINTS_TO_CLASS_RESOURCE,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPath("/foo/bar/baz.class")).code);
    }

    @Test
    public void resourceCanNotContainEmptySegment() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_CONTAINS_EMPTY_SEGMENT,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPath("//foo/bar/baz")).code);
    }

    @Test
    public void webResourceCanNotContainEmptySegment() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_CONTAINS_EMPTY_SEGMENT,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPath("//foo/bar/baz")).code);
    }

    @Test
    public void resourceCanNotContainContainSegmentLinkToSelf() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_CONTAINS_LINK_TO_SELF,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPath("/./foo/bar/baz")).code);
    }

    @Test
    public void webResourceCanNotContainSegmentLinkToSelf() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_CONTAINS_LINK_TO_SELF,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPath("/./foo/bar/baz")).code);
    }

    @Test
    public void resourceCanNotContainSegmentLinkToParent() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_CONTAINS_LINK_TO_PARENT,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofPath("/../foo/bar/baz")).code);
    }

    @Test
    public void webResourceCanNotContainSegmentLinkToParent() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_CONTAINS_LINK_TO_PARENT,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPath("/../foo/bar/baz")).code);

    }

    @Test
    public void prefixDistinguishesResourcePathAndWebResourcePath() {
        MCRResourcePath path2 = MCRResourcePath.ofPath("/META-INF/resources/foo/bar/baz").orElseThrow();
        MCRResourcePath path1 = MCRResourcePath.ofWebPath("/foo/bar/baz").orElseThrow();
        assertEquals(path1, path2);
    }

    @Test
    public void webResourceCanNotHaveMetaInfPrefix() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_POINTS_TO_META_INF_RESOURCE,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPath("/META-INF/foo/bar/baz")).code);
    }

    @Test
    public void webResourceCanNotHaveWebInfPrefix() {
        assertEquals(MCRResourcePath.IllegalPathException.Code.PATH_POINTS_TO_WEB_INF_RESOURCE,
            assertThrows(MCRResourcePath.IllegalPathException.class,
                () -> MCRResourcePath.ofWebPath("/WEB-INF/foo/bar/baz")).code);
    }

    @Test
    public void resourcePathWithoutPrefixAsResourcePathEqualsOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofPath("/foo/bar/baz").orElseThrow();
        assertEquals("/foo/bar/baz", path.asAbsolutePath());
        assertEquals("foo/bar/baz", path.asRelativePath());
    }

    @Test
    public void resourcePathWithPrefixAsResourcePathEqualsOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofPath("/META-INF/resources/foo/bar/baz").orElseThrow();
        assertEquals("/META-INF/resources/foo/bar/baz", path.asAbsolutePath());
        assertEquals("META-INF/resources/foo/bar/baz", path.asRelativePath());
    }

    @Test
    public void webResourcePathWithoutPrefixAsResourcePathAddsPrefixToOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofWebPath("/foo/bar/baz").orElseThrow();
        assertEquals("/META-INF/resources/foo/bar/baz", path.asAbsolutePath());
        assertEquals("META-INF/resources/foo/bar/baz", path.asRelativePath());
    }

    @Test
    public void resourcePathWithoutPrefixAsWebResourcePathDoesNotExist() {
        MCRResourcePath path = MCRResourcePath.ofPath("/foo/bar/baz").orElseThrow();
        assertEquals(EMPTY, path.asAbsoluteWebPath());
        assertEquals(EMPTY, path.asRelativeWebPath());
    }

    @Test
    public void resourcePathWithPrefixAsWebResourcePathRemovesPrefixFromOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofPath("/META-INF/resources/foo/bar/baz").orElseThrow();
        assertEquals("/foo/bar/baz", path.asAbsoluteWebPath().orElseThrow());
        assertEquals("foo/bar/baz", path.asRelativeWebPath().orElseThrow());
    }

    @Test
    public void webResourcePathWithoutPrefixAsWebResourcePathEqualsOriginalPath() {
        MCRResourcePath path = MCRResourcePath.ofWebPath("/foo/bar/baz").orElseThrow();
        assertEquals("/foo/bar/baz", path.asAbsoluteWebPath().orElseThrow());
        assertEquals("foo/bar/baz", path.asRelativeWebPath().orElseThrow());
    }

}
