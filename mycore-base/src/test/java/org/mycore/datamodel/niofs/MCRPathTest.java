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
