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

package org.mycore.datamodel.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import org.junit.Test;

public class MCRPathTest {

    @Test
    public void startsWith() {
        Path test1 = new TestMCRPath("foo", "/bar/baz");
        Path test2 = new TestMCRPath("foo", "/bar");
        assertTrue(test1.startsWith(test2));
        test2 = new TestMCRPath("foo", "");
        assertTrue(test1.startsWith(test2));
        test2 = test1.resolve("..");
        assertFalse(test1.startsWith(test2));
        assertTrue(test1.startsWith(test2.normalize()));
        test2 = test1.resolve("../bin");
        assertFalse(test1.startsWith(test2));
        test2 = test1.resolve(new TestMCRPath("bin", "/bar"));
        assertFalse(test1.startsWith(test2));
    }

    @Test
    public void normalizeRelativeWithLeadingDotDot() {
        // ../../Test.png should stay unchanged — no preceding elements to cancel
        Path path = new TestMCRPath(null, "../../Test.png");
        assertEquals("../../Test.png", path.normalize().toString());
    }

    @Test
    public void normalizeRelativeSingleDotDot() {
        // ../foo should stay unchanged
        Path path = new TestMCRPath(null, "../foo");
        assertEquals("../foo", path.normalize().toString());
    }

    @Test
    public void normalizeRelativeDotDotBeyondBase() {
        // foo/../../bar should normalize to ../bar
        Path path = new TestMCRPath(null, "foo/../../bar");
        assertEquals("../bar", path.normalize().toString());
    }

    @Test
    public void normalizeRelativeDotDotCancels() {
        // foo/bar/../baz should normalize to foo/baz
        Path path = new TestMCRPath(null, "foo/bar/../baz");
        assertEquals("foo/baz", path.normalize().toString());
    }

    @Test
    public void normalizeRelativeDot() {
        // foo/./bar should normalize to foo/bar
        Path path = new TestMCRPath(null, "foo/./bar");
        assertEquals("foo/bar", path.normalize().toString());
    }

    @Test
    public void normalizeAbsoluteDotDot() {
        // absolute: /foo/bar/../baz should normalize to /foo/baz
        Path path = new TestMCRPath("owner", "/foo/bar/../baz");
        assertEquals("owner:/foo/baz", path.normalize().toString());
    }

    @Test
    public void normalizeAbsoluteDotDotAtRoot() {
        // absolute: /foo/.. should normalize to root
        Path path = new TestMCRPath("owner", "/foo/..");
        assertEquals("owner:/", path.normalize().toString());
    }

    @Test
    public void normalizeNoOpWhenClean() {
        Path path = new TestMCRPath(null, "foo/bar");
        assertEquals("foo/bar", path.normalize().toString());
    }

    private static class TestMCRPath extends MCRPath {

        public static final MCRAbstractFileSystem MCR_ABSTRACT_FILE_SYSTEM = new MCRAbstractFileSystem() {
            @Override
            public void createRoot(String owner) {
                //no implementation needed for test
            }

            @Override
            public void removeRoot(String owner) {
                //no implementation needed for test
            }

            @Override
            public FileSystemProvider provider() {
                return null;
            }

            @Override
            public Iterable<Path> getRootDirectories() {
                return null;
            }

            @Override
            public Iterable<FileStore> getFileStores() {
                return null;
            }
        };

        TestMCRPath(String root, String path) {
            super(root, path);
        }

        @Override
        public MCRAbstractFileSystem getFileSystem() {
            return MCR_ABSTRACT_FILE_SYSTEM;
        }
    }
}
