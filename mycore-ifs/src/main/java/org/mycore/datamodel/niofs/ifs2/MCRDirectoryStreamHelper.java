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

package org.mycore.datamodel.niofs.ifs2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.common.events.MCRPathEventHelper;
import org.mycore.common.function.MCRThrowFunction;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.ifs2.MCRStoredNode;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRDefaultFileAttributes;
import org.mycore.datamodel.niofs.MCRDigestAttributeView;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * A {@link MCRSecureDirectoryStream} on internal file system. This implementation uses IFS directly.
 * Do use this class but stick to the interface.
 *
 * @author Thomas Scheffler (yagee)
 */
class MCRDirectoryStreamHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    static DirectoryStream<Path> createInstance(MCRDirectory dir, MCRPath path) throws IOException {
        DirectoryStream.Filter<Path> filter = (dir instanceof MCRFileCollection) ? MCRFileCollectionFilter.FILTER
            : AcceptAllFilter.FILTER;
        LOGGER.debug("Dir {}, class {}, filter {}", () -> path, dir::getClass, filter::getClass);
        DirectoryStream<Path> baseDirectoryStream = Files.newDirectoryStream(dir.getLocalPath(), filter);
        LOGGER.debug("baseStream {}", baseDirectoryStream::getClass);
        if (baseDirectoryStream instanceof SecureDirectoryStream secureDirectoryStream) {
            LOGGER.debug("Returning SecureDirectoryStream");
            return new MCRSecureDirectoryStream(dir, path, secureDirectoryStream);
        }
        return new SimpleDirectoryStream<>(path, baseDirectoryStream);
    }

    private static final class AcceptAllFilter
        implements DirectoryStream.Filter<Path> {
        static final MCRDirectoryStreamHelper.AcceptAllFilter FILTER = new AcceptAllFilter();

        @Override
        public boolean accept(Path entry) {
            return true;
        }
    }

    private static final class MCRFileCollectionFilter
        implements DirectoryStream.Filter<Path> {
        static final MCRDirectoryStreamHelper.MCRFileCollectionFilter FILTER = new MCRFileCollectionFilter();

        @Override
        public boolean accept(Path entry) {
            return !MCRFileCollection.DATA_FILE.equals(entry.getFileName().toString());
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

    private static class MCRSecureDirectoryStream extends SimpleDirectoryStream<SecureDirectoryStream<Path>>
        implements SecureDirectoryStream<Path> {

        private final MCRDirectory dir;

        MCRSecureDirectoryStream(MCRDirectory dir, MCRPath dirPath, SecureDirectoryStream<Path> baseStream) {
            super(dirPath, baseStream);
            this.dir = dir;
        }

        @Override
        public SecureDirectoryStream<Path> newDirectoryStream(Path path, LinkOption... options)
            throws IOException {
            checkClosed();
            if (path.isAbsolute()) {
                return (MCRSecureDirectoryStream) Files.newDirectoryStream(path);
            }
            MCRStoredNode nodeByPath = resolve(path);
            if (!nodeByPath.isDirectory()) {
                throw new NotDirectoryException(nodeByPath.getPath());
            }
            MCRDirectory newDir = (MCRDirectory) nodeByPath;
            return (SecureDirectoryStream<Path>) createInstance(newDir, getCurrentSecurePath(newDir));
        }

        private MCRStoredNode resolve(Path path) {
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
            final MCRStoredNode storedNode = resolve(path);
            final MCRPath mcrPath = getCurrentSecurePath(storedNode);
            storedNode.delete();
            MCRPathEventHelper.fireFileDeleteEvent(mcrPath);
        }

        @Override
        public void deleteDirectory(Path path) throws IOException {
            checkClosed();
            if (path.isAbsolute()) {
                Files.delete(path);
            }
            resolve(path).delete();
        }

        @Override
        public void move(Path srcpath, SecureDirectoryStream<Path> targetdir, Path targetpath) throws IOException {
            checkClosed();
            MCRPath src = checkFileSystem(srcpath);
            MCRFile srcFile = srcpath.isAbsolute() ? MCRFileSystemUtils.getMCRFile(src, false, false, true)
                                                   : (MCRFile) resolve(srcpath);
            if (srcFile == null) {
                throw new NoSuchFileException(this.dirPath.toString(), srcpath.toString(), null);
            }
            if (!targetpath.isAbsolute() && targetdir instanceof MCRSecureDirectoryStream that) {
                LOGGER.debug("Move Case #1");
                MCRFile file = getMCRFile(that, targetpath);
                Files.delete(file.getLocalPath()); //delete for move
                if (!srcpath.isAbsolute()) {
                    LOGGER.debug("Move Case #1.1");
                    baseStream.move(toLocalPath(src), that.baseStream, toLocalPath(targetpath));
                } else {
                    LOGGER.debug("Move Case #1.2");
                    baseStream.move(srcFile.getLocalPath(), that.baseStream, toLocalPath(targetpath));
                }
                file.setMD5(srcFile.getMD5()); //restore md5
                final MCRPath targetAbsolutePath = that.getCurrentSecurePath(file);
                final BasicFileAttributes attrs = that.getFileAttributeView(targetpath, BasicFileAttributeView.class)
                    .readAttributes();
                MCRPathEventHelper.fireFileCreateEvent(targetAbsolutePath, attrs);
            } else {
                LOGGER.debug("Move Case #2");
                if (targetpath.isAbsolute()) {
                    LOGGER.debug("Move Case #2.1");
                    Files.move(srcFile.getLocalPath(), targetpath, StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    LOGGER.debug("Move Case #2.2");
                    try (SeekableByteChannel sourceChannel = Files.newByteChannel(srcFile.getLocalPath());
                        SeekableByteChannel targetChannel = targetdir.newByteChannel(targetpath,
                            Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
                        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                        while (sourceChannel.read(buffer) > 0) {
                            buffer.flip();
                            while (buffer.hasRemaining()) {
                                targetChannel.write(buffer);
                            }
                            buffer.clear();
                        }
                    }
                }
            }
            srcFile.delete();
            MCRPathEventHelper.fireFileDeleteEvent(this.dirPath.resolve(src));
        }

        private static MCRFile getMCRFile(MCRSecureDirectoryStream ds, Path relativePath) throws IOException {
            MCRStoredNode storedNode = ds.resolve(relativePath);
            if (storedNode != null) {
                throw new FileAlreadyExistsException(ds.dirPath.resolve(relativePath).toString());
            }
            //does not exist, have to create
            MCRStoredNode parent = ds.dir;
            if (relativePath.getNameCount() > 1) {
                parent = (MCRStoredNode) parent.getNodeByPath(relativePath.getParent().toString());
                if (parent == null) {
                    throw new NoSuchFileException(ds.dirPath.resolve(relativePath.getParent()).toString());
                }
                if (!(parent instanceof MCRDirectory)) {
                    throw new NotDirectoryException(ds.dirPath.resolve(relativePath.getParent()).toString());
                }
            }
            return ((MCRDirectory) parent).createFile(relativePath.getFileName().toString());
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
            V fileAttributeView = baseStream.getFileAttributeView(type);
            if (fileAttributeView != null) {
                return fileAttributeView;
            }
            if (type == MCRDigestAttributeView.class) {
                BasicFileAttributeView baseView = baseStream.getFileAttributeView(BasicFileAttributeView.class);
                return (V) new MD5FileAttributeViewImpl(baseView, (v) -> dir);
            }
            return null;
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
            Path localRelativePath = toLocalPath(path);
            if (type == MCRDigestAttributeView.class) {
                BasicFileAttributeView baseView = baseStream.getFileAttributeView(localRelativePath,
                    BasicFileAttributeView.class, options);
                return (V) new MD5FileAttributeViewImpl(baseView, (v) -> resolve(path));
            }
            return baseStream.getFileAttributeView(localRelativePath, type, options);
        }

        private Path toLocalPath(Path path) {
            return MCRFileSystemUtils.toNativePath(dir.getLocalPath().getFileSystem(), path);
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
            return MCRPath.ofPath(path);
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

    private record MD5FileAttributeViewImpl(BasicFileAttributeView baseAttrView,
        MCRThrowFunction<Void, MCRStoredNode, IOException> nodeSupplier) implements MCRDigestAttributeView {

        @Override
        public String name() {
            return "md5";
        }

        @Override
        public BasicFileAttributes readAttributes() {
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
            if (node instanceof MCRFile file) {
                return MCRDefaultFileAttributes.ofAttributes(baseAttrView.readAttributes(),
                    new MCRMD5Digest(file.getMD5()));
            }
            return MCRDefaultFileAttributes.ofAttributes(baseAttrView.readAttributes(), null);
        }
    }
}
