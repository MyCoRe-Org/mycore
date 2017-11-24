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

package org.mycore.datamodel.niofs.ifs1;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRMD5AttributeView;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * A {@link SecureDirectoryStream} on internal file system. This implementation uses IFS directly. Do use this class but
 * stick to the interface.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRDirectoryStream implements SecureDirectoryStream<Path> {
    static Logger LOGGER = LogManager.getLogger();

    private MCRDirectory dir;

    private MCRPath path;

    private Iterator<Path> iterator;

    /**
     * @throws IOException
     *             if 'path' is not from {@link MCRIFSFileSystem}
     */
    public MCRDirectoryStream(MCRDirectory dir, MCRPath path) throws IOException {
        this.dir = Objects.requireNonNull(dir, "'dir' may not be null");
        this.path = Optional.of(path).orElseGet(dir::toPath);
    }

    @Override
    public Iterator<Path> iterator() {
        checkClosed();
        synchronized (this) {
            if (iterator != null) {
                throw new IllegalStateException("Iterator already obtained");
            }
            iterator = new MCRDirectoryIterator(this);
            return iterator;
        }
    }

    @Override
    public void close() throws IOException {
        dir = null;
    }

    void checkClosed() {
        if (dir == null) {
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

    @Override
    public SecureDirectoryStream<Path> newDirectoryStream(Path path, LinkOption... options) throws IOException {
        checkClosed();
        MCRPath mcrPath = checkFileSystem(path);
        if (mcrPath.isAbsolute()) {
            return (SecureDirectoryStream<Path>) Files.newDirectoryStream(mcrPath);
        }
        MCRFilesystemNode childByPath = dir.getChildByPath(mcrPath.toString());
        if (childByPath == null || childByPath instanceof MCRFile) {
            throw new NoSuchFileException(dir.toString(), path.toString(), "Does not exist or is a file.");
        }
        return new MCRDirectoryStream((MCRDirectory) childByPath, MCRPath.toMCRPath(path.resolve(mcrPath)));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
        throws IOException {
        checkClosed();
        MCRPath mcrPath = checkRelativePath(path);
        if (mcrPath.isAbsolute()) {
            return mcrPath.getFileSystem().provider().newByteChannel(mcrPath, options, attrs);
        }
        Set<? extends OpenOption> fileOpenOptions = options.stream()
            .filter(option -> !(option == StandardOpenOption.CREATE || option == StandardOpenOption.CREATE_NEW))
            .collect(Collectors.toSet());
        boolean create = options.contains(StandardOpenOption.CREATE);
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        if (create || createNew) {
            for (OpenOption option : fileOpenOptions) {
                //check before we create any file instance
                MCRFile.checkOpenOption(option);
            }
        }
        MCRFileSystemProvider provider = (MCRFileSystemProvider) mcrPath.getFileSystem().provider();
        MCRFileSystemUtils.getMCRFile(dir, mcrPath, create, createNew);
        return provider.newByteChannel(this.path.resolve(mcrPath), fileOpenOptions, attrs);
    }

    @Override
    public void deleteFile(Path path) throws IOException {
        deleteFileSystemNode(checkFileSystem(path));
    }

    @Override
    public void deleteDirectory(Path path) throws IOException {
        deleteFileSystemNode(checkFileSystem(path));
    }

    /**
     * Deletes {@link MCRFilesystemNode} if it exists.
     *
     * @param path
     *            relative or absolute
     * @throws IOException
     */
    private void deleteFileSystemNode(MCRPath path) throws IOException {
        checkClosed();
        if (path.isAbsolute()) {
            Files.delete(path);
        }
        MCRFilesystemNode childByPath = dir.getChildByPath(path.toString());
        if (childByPath == null) {
            throw new NoSuchFileException(dir.toPath().toString(), path.toString(), null);
        }
        try {
            childByPath.delete();
        } catch (MCRPersistenceException e) {
            throw new IOException("Error whil deleting file system node.", e);
        }
    }

    @Override
    public void move(Path srcpath, SecureDirectoryStream<Path> targetdir, Path targetpath) throws IOException {
        checkClosed();
        checkFileSystem(srcpath);
        checkFileSystem(targetpath);
        throw new AtomicMoveNotSupportedException(srcpath.toString(), targetpath.toString(),
            "Currently not implemented");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
        return getFileAttributeView(null, type, (LinkOption[]) null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if (path != null) {
            MCRPath file = checkRelativePath(path);
            if (file.getNameCount() != 1) {
                throw new InvalidPathException(path.toString(), "'path' must have one name component.");
            }
        }
        checkClosed();
        if (type == null) {
            throw new NullPointerException();
        }
        //must support BasicFileAttributeView
        if (type == BasicFileAttributeView.class) {
            return (V) new BasicFileAttributeViewImpl(this, path);
        }
        if (type == MCRMD5AttributeView.class) {
            return (V) new MD5FileAttributeViewImpl(this, path);
        }
        return null;
    }

    private static class MCRDirectoryIterator implements Iterator<Path> {

        Path nextPath;

        boolean hasNextCalled;

        private MCRDirectoryStream mcrDirectoryStream;

        MCRFilesystemNode[] children;

        private int pos;

        public MCRDirectoryIterator(MCRDirectoryStream mcrDirectoryStream) {
            this.mcrDirectoryStream = mcrDirectoryStream;
            children = mcrDirectoryStream.dir.getChildren();
            this.nextPath = null;
            hasNextCalled = false;
            pos = -1;
        }

        @Override
        public boolean hasNext() {
            LOGGER.debug(() -> "hasNext() called: " + pos);
            if (mcrDirectoryStream.dir == null) {
                return false; //stream closed
            }
            int nextPos = pos + 1;
            if (nextPos >= children.length) {
                return false;
            }
            //we are OK
            nextPath = getPath(children, nextPos);
            hasNextCalled = true;
            return true;
        }

        private MCRPath getPath(MCRFilesystemNode[] children, int index) {
            try {
                MCRPath path = MCRPath.toMCRPath(mcrDirectoryStream.path.resolve(children[index].getName()));
                LOGGER.debug(() -> "getting path at index " + index + ": " + path);
                return path;
            } catch (RuntimeException e) {
                throw new DirectoryIteratorException(new IOException(e));
            }
        }

        @Override
        public Path next() {
            LOGGER.debug(() -> "next() called: " + pos);
            pos++;
            if (hasNextCalled) {
                hasNextCalled = false;
                return nextPath;
            }
            mcrDirectoryStream.checkClosed();
            if (pos >= children.length) {
                throw new NoSuchElementException();
            }
            return getPath(children, pos);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    static class BasicFileAttributeViewImpl extends MCRBasicFileAttributeViewImpl {

        private MCRDirectoryStream mcrDirectoryStream;

        private Path fileName;

        public BasicFileAttributeViewImpl(MCRDirectoryStream mcrDirectoryStream, Path path) {
            this.mcrDirectoryStream = mcrDirectoryStream;
            if (path.toString().length() <= 2 && (path.toString().equals(".") || path.toString().equals(".."))) {
                throw new InvalidPathException(path.toString(), "'path' must be a valid file name.");
            }
            this.fileName = path;
        }

        @Override
        protected MCRFilesystemNode resolveNode() throws IOException {
            MCRDirectory parent = mcrDirectoryStream.dir;
            mcrDirectoryStream.checkClosed();
            MCRFilesystemNode child;
            try {
                child = parent.getChild(fileName.toString());
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
            if (child == null) {
                throw new NoSuchFileException(parent.toPath().toString(), fileName.toString(), null);
            }
            return child;
        }
    }

    private static class MD5FileAttributeViewImpl extends BasicFileAttributeViewImpl implements
        MCRMD5AttributeView<String> {

        public MD5FileAttributeViewImpl(MCRDirectoryStream mcrDirectoryStream, Path path) {
            super(mcrDirectoryStream, path);
            // TODO Auto-generated constructor stub
        }

        @Override
        public MCRFileAttributes<String> readAllAttributes() throws IOException {
            return readAttributes();
        }

        @Override
        public String name() {
            return "md5";
        }

    }
}
