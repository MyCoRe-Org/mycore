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

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

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

        TestMCRPath(String root, String path) {
            super(root, path);
        }

        @Override
        public MCRAbstractFileSystem getFileSystem() {
            return FILE_SYSTEM;
        }
    }

    private static final MCRAbstractFileSystemProvider FILE_SYSTEM_PROVIDER = new MCRAbstractFileSystemProvider() {
        @Override
        public URI getURI() {
            return null;
        }

        @Override
        public MCRAbstractFileSystem getFileSystem() {
            return FILE_SYSTEM;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public FileSystem getFileSystem(URI uri) {
            return FILE_SYSTEM;
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> set,
            FileAttribute<?>... fileAttributes) throws IOException {
            return null;
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter)
            throws IOException {
            return null;
        }

        @Override
        public void createDirectory(Path path, FileAttribute<?>... fileAttributes) throws IOException {

        }

        @Override
        public void delete(Path path) throws IOException {

        }

        @Override
        public void copy(Path path, Path path1, CopyOption... copyOptions) throws IOException {

        }

        @Override
        public void move(Path path, Path path1, CopyOption... copyOptions) throws IOException {

        }

        @Override
        public boolean isSameFile(Path path, Path path1) throws IOException {
            return false;
        }

        @Override
        public boolean isHidden(Path path) throws IOException {
            return false;
        }

        @Override
        public FileStore getFileStore(Path path) throws IOException {
            return null;
        }

        @Override
        public void checkAccess(Path path, AccessMode... accessModes) throws IOException {

        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> aClass,
            LinkOption... linkOptions) {
            return null;
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> aClass, LinkOption... linkOptions)
            throws IOException {
            return null;
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String s, LinkOption... linkOptions) throws IOException {
            return null;
        }

        @Override
        public void setAttribute(Path path, String s, Object o, LinkOption... linkOptions) throws IOException {

        }
    };

    private static final MCRAbstractFileSystem FILE_SYSTEM = new MCRAbstractFileSystem(null) {
        @Override
        public void createRoot(String owner) {
            //no implementation needed for test
        }

        @Override
        public void removeRoot(String owner) {
            //no implementation needed for test
        }

        @Override
        public MCRAbstractFileSystemProvider provider() {
            return FILE_SYSTEM_PROVIDER;
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
}
