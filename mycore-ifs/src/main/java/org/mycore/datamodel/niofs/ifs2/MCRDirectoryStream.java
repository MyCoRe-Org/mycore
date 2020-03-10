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

package org.mycore.datamodel.niofs.ifs2;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.function.MCRThrowFunction;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.ifs2.MCRStoredNode;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRMD5AttributeView;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * A {@link SecureDirectoryStream} on internal file system. This implementation uses IFS directly. Do use this class but
 * stick to the interface.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRDirectoryStream {
    static Logger LOGGER = LogManager.getLogger();

    static DirectoryStream<Path> getInstance(MCRDirectory dir, MCRPath path) throws IOException {
        DirectoryStream.Filter<Path> filter = (dir instanceof MCRFileCollection) ? MCRFileCollectionFilter.FILTER
            : AcceptAllFilter.FILTER;
        LOGGER.info("Dir {}, class {}, filter {}", path, dir.getClass(), filter.getClass());
        DirectoryStream<Path> baseDirectoryStream = Files.newDirectoryStream(dir.getLocalPath(), filter);
        LOGGER.info("baseStream {}", baseDirectoryStream.getClass());
        if (baseDirectoryStream instanceof java.nio.file.SecureDirectoryStream) {
            LOGGER.info("Returning SecureDirectoryStream");
            return new SecureDirectoryStream(dir, path,
                (java.nio.file.SecureDirectoryStream<Path>) baseDirectoryStream);
        }
        return new SimpleDirectoryStream(path, baseDirectoryStream);
    }

    private static class AcceptAllFilter
        implements DirectoryStream.Filter<Path> {
        static final MCRDirectoryStream.AcceptAllFilter FILTER = new AcceptAllFilter();

        @Override
        public boolean accept(Path entry) {
            return true;
        }
    }

    private static class MCRFileCollectionFilter
        implements DirectoryStream.Filter<Path> {
        static final MCRDirectoryStream.MCRFileCollectionFilter FILTER = new MCRFileCollectionFilter();

        @Override
        public boolean accept(Path entry) {
            return !entry.getFileName().toString().equals(MCRFileCollection.DATA_FILE);
        }
    }

    private static class SimpleDirectoryStream<T extends DirectoryStream<Path>> implements DirectoryStream<Path> {
        protected final MCRPath dirPath;

        protected final T baseStream;

        boolean isClosed;

        SimpleDirectoryStream(MCRPath dirPath, T baseStream) {
            this.dirPath = dirPath;
            this.baseStream = baseStream;
        }

        @Override
        public Iterator<Path> iterator() {
            return new SimpleDirectoryIterator(dirPath, baseStream);
        }

        @Override
        public void close() throws IOException {
            baseStream.close();
            isClosed = true;
        }

        protected boolean isClosed() {
            return isClosed;
        }
    }

    private static class SecureDirectoryStream extends SimpleDirectoryStream<java.nio.file.SecureDirectoryStream<Path>>
        implements java.nio.file.SecureDirectoryStream<Path> {

        private final MCRDirectory dir;

        SecureDirectoryStream(MCRDirectory dir, MCRPath dirPath,
            java.nio.file.SecureDirectoryStream<Path> baseStream) {
            super(dirPath, baseStream);
            this.dir = dir;
        }

        @Override
        public java.nio.file.SecureDirectoryStream<Path> newDirectoryStream(Path path, LinkOption... options)
            throws IOException {
            checkClosed();
            if (path.isAbsolute()) {
                return (SecureDirectoryStream) Files.newDirectoryStream(path);
            }
            MCRStoredNode nodeByPath = resolve(path);
            if (!nodeByPath.isDirectory()) {
                throw new NotDirectoryException(nodeByPath.getPath());
            }
            MCRDirectory newDir = (MCRDirectory) nodeByPath;
            return (java.nio.file.SecureDirectoryStream<Path>) MCRDirectoryStream.getInstance(newDir,
                getCurrentSecurePath(newDir));
        }

        private MCRStoredNode resolve(Path path) throws IOException {
            checkRelativePath(path);
            return (MCRStoredNode) dir.getNodeByPath(path.toString());
        }

        /**
         * always resolves the path to this directory
         * currently not really secure, but more secure than sticking to <code>dirPath</code>
         * @param node to get Path from
         */
        private MCRPath getCurrentSecurePath(MCRStoredNode node) {
            return MCRAbstractFileSystem.getPath(MCRFileSystemUtils.getOwnerID(node), node.getPath(),
                MCRFileSystemProvider.getMCRIFSFileSystem());
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
            checkClosed();
            if (path.isAbsolute()) {
                return Files.newByteChannel(path, options);
            }
            MCRPath mcrPath = checkRelativePath(path);
            Path resolved = getCurrentSecurePath(dir).resolve(mcrPath);
            return Files.newByteChannel(resolved, options);
        }

        @Override
        public void deleteFile(Path path) throws IOException {
            checkClosed();
            if (path.isAbsolute()) {
                Files.delete(path);
            }
            resolve(path).delete();
        }

        @Override
        public void deleteDirectory(Path path) throws IOException {
            deleteFile(path);
        }

        @Override
        public void move(Path srcpath, java.nio.file.SecureDirectoryStream<Path> targetdir, Path targetpath)
            throws IOException {

        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
            V fileAttributeView = baseStream.getFileAttributeView(type);
            if (fileAttributeView != null) {
                return fileAttributeView;
            }
            if (type == MCRMD5AttributeView.class) {
                BasicFileAttributeView baseView = baseStream.getFileAttributeView(BasicFileAttributeView.class);
                return (V) new MD5FileAttributeViewImpl(baseView, (v) -> dir);
            }
            return null;
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
            Path localRelativePath = MCRFileSystemUtils.toNativePath(dir.getLocalPath().getFileSystem(), path);
            V fileAttributeView = baseStream.getFileAttributeView(localRelativePath, type, options);
            if (fileAttributeView != null) {
                return fileAttributeView;
            }
            if (type == MCRMD5AttributeView.class) {
                BasicFileAttributeView baseView = baseStream.getFileAttributeView(BasicFileAttributeView.class);
                return (V) new MD5FileAttributeViewImpl(baseView, (v) -> resolve(path));
            }
            return null;
        }

        void checkClosed() {
            if (isClosed) {
                throw new ClosedDirectoryStreamException();
            }
        }

        MCRPath checkRelativePath(Path path) {
            if (path.isAbsolute()) {
                throw new IllegalArgumentException(path + " is absolute.");
            }
            return checkFileSystem(path);
        }

        private MCRPath checkFileSystem(Path path) {
            if (!(path.getFileSystem() instanceof MCRIFSFileSystem)) {
                throw new IllegalArgumentException(path + " is not from " + MCRIFSFileSystem.class.getSimpleName());
            }
            return MCRPath.toMCRPath(path);
        }
    }

    private static class SimpleDirectoryIterator implements Iterator<Path> {
        private final Iterator<Path> baseIterator;

        private final MCRPath dir;

        SimpleDirectoryIterator(MCRPath dir, DirectoryStream<Path> baseStream) {
            this.baseIterator = baseStream.iterator();
            this.dir = dir;
        }

        @Override
        public boolean hasNext() {
            return baseIterator.hasNext();
        }

        @Override
        public Path next() {
            Path basePath = baseIterator.next();
            return dir.resolve(basePath.getFileName().toString());
        }
    }

    private static class MD5FileAttributeViewImpl implements
        MCRMD5AttributeView {

        private final BasicFileAttributeView baseAttrView;

        private final MCRThrowFunction<Void, MCRStoredNode, IOException> nodeSupplier;

        MD5FileAttributeViewImpl(BasicFileAttributeView baseAttrView,
            MCRThrowFunction<Void, MCRStoredNode, IOException> nodeSupplier) {
            this.baseAttrView = baseAttrView;
            this.nodeSupplier = nodeSupplier;
        }

        @Override
        public String name() {
            return "md5";
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            return null;
        }

        @Override
        public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
            throws IOException {
            baseAttrView.setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public MCRFileAttributes readAllAttributes() throws IOException {
            MCRStoredNode node = nodeSupplier.apply(null);
            if (node instanceof MCRFile) {
                return MCRFileAttributes.fromAttributes(baseAttrView.readAttributes(),
                    ((MCRFile) node).getMD5());
            }
            return MCRFileAttributes.fromAttributes(baseAttrView.readAttributes(), null);
        }
    }
}
