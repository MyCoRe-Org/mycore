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
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mycore.common.events.MCRPathEventHelper;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.ifs2.MCRStoredNode;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRAbstractFileSystemProvider;
import org.mycore.datamodel.niofs.MCRBasicFileAttributeViewProperties;
import org.mycore.datamodel.niofs.MCRDigestAttributeView;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.common.collect.Sets;

/**
 * MyCoRe IFS2 FileSystemProvider implementation
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRFileSystemProvider extends MCRAbstractFileSystemProvider {

    /**
     * scheme part of the IFS file system URI
     */
    public static final String SCHEME = "ifs2";

    /**
     * base URI of the IFS fiel system
     */
    public static final URI FS_URI = URI.create(SCHEME + ":///");

    private static volatile MCRIFSFileSystem FILE_SYSTEM_INSTANCE;

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getScheme()
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public URI getURI() {
        return FS_URI;
    }

    @Override
    public MCRIFSFileSystem getFileSystem() {
        return getMCRIFSFileSystem();
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getFileSystem(java.net.URI)
     */
    @Override
    public MCRIFSFileSystem getFileSystem(URI uri) {
        if (FILE_SYSTEM_INSTANCE == null) {
            synchronized (FS_URI) {
                if (FILE_SYSTEM_INSTANCE == null) {
                    FILE_SYSTEM_INSTANCE = new MCRIFSFileSystem(this);
                }
            }
        }
        return FILE_SYSTEM_INSTANCE;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#newByteChannel(java.nio.file.Path, java.util.Set, java.nio.file.attribute.FileAttribute[])
     */
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
        throws IOException {
        if (attrs.length > 0) {
            throw new UnsupportedOperationException("Atomically setting of file attributes is not supported.");
        }
        MCRPath ifsPath = MCRFileSystemUtils.checkPathAbsolute(path);
        boolean create = options.contains(StandardOpenOption.CREATE);
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        Set<? extends OpenOption> fileOpenOptions = getOpenOptions(options);
        if (create || createNew) {
            checkFileName(ifsPath.getFileName().toString());
            checkOpenOption(fileOpenOptions);
        }
        boolean channelCreateEvent = createNew || Files.notExists(ifsPath);
        MCRFile mcrFile = MCRFileSystemUtils.getMCRFile(ifsPath, create, createNew, !channelCreateEvent);
        if (mcrFile == null) {
            throw new NoSuchFileException(path.toString());
        }
        boolean write = options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND);
        FileChannel baseChannel = (FileChannel) Files.newByteChannel(mcrFile.getLocalPath(), fileOpenOptions);
        return new MCRFileChannel(ifsPath, mcrFile, baseChannel, write, channelCreateEvent);
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream.Filter)
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(dir);
        MCRStoredNode node = MCRFileSystemUtils.resolvePath(mcrPath);
        if (node instanceof MCRDirectory mcrDirectory) {
            return MCRDirectoryStreamHelper.getInstance(mcrDirectory, mcrPath);
        }
        throw new NotDirectoryException(dir.toString());
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#createDirectory(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])
     */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        if (attrs.length > 0) {
            throw new UnsupportedOperationException("Setting 'attrs' atomically is unsupported.");
        }
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(dir);
        MCRDirectory rootDirectory;
        if (mcrPath.getNameCount() == 0) {
            MCRObjectID derId = MCRObjectID.getInstance(mcrPath.getOwner());
            org.mycore.datamodel.ifs2.MCRFileStore store = MCRFileSystemUtils.getStore(derId.getBase());
            if (store.retrieve(derId.getNumberAsInteger()) != null) {
                throw new FileAlreadyExistsException(mcrPath.toString());
            }
            store.create(derId.getNumberAsInteger());
            return;
        } else {
            //not root directory
            checkFileName(mcrPath.getFileName().toString());
        }
        rootDirectory = MCRFileSystemUtils.getFileCollection(mcrPath.getOwner());
        MCRPath parentPath = mcrPath.getParent();
        MCRPath absolutePath = getAbsolutePathFromRootComponent(parentPath);
        MCRStoredNode childByPath = (MCRStoredNode) rootDirectory.getNodeByPath(absolutePath.toString());
        if (childByPath == null) {
            throw new NoSuchFileException(parentPath.toString(), dir.getFileName().toString(),
                "parent directory does not exist");
        }
        if (childByPath instanceof MCRFile) {
            throw new NotDirectoryException(parentPath.toString());
        }
        MCRDirectory parentDir = (MCRDirectory) childByPath;
        String dirName = mcrPath.getFileName().toString();
        if (parentDir.getChild(dirName) != null) {
            throw new FileAlreadyExistsException(mcrPath.toString());
        }
        parentDir.createDir(dirName);
    }

    static MCRPath getAbsolutePathFromRootComponent(MCRPath mcrPath) {
        if (!mcrPath.isAbsolute()) {
            throw new InvalidPathException(mcrPath.toString(), "'path' must be absolute.");
        }
        String path = mcrPath.toString().substring(mcrPath.getOwner().length() + 1);
        return MCRAbstractFileSystem.getPath(null, path, mcrPath.getFileSystem());
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#delete(java.nio.file.Path)
     */
    @Override
    public void delete(Path path) throws IOException {
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(path);
        MCRStoredNode child = MCRFileSystemUtils.resolvePath(mcrPath);
        if (child instanceof MCRDirectory && child.hasChildren()) {
            throw new DirectoryNotEmptyException(mcrPath.toString());
        }
        try {
            child.delete();
            MCRPathEventHelper.fireFileDeleteEvent(path);
        } catch (RuntimeException e) {
            throw new IOException("Could not delete: " + mcrPath, e);
        }
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])
     */
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        if (isSameFile(source, target)) {
            return; //that was easy
        }
        checkCopyOptions(options);
        HashSet<CopyOption> copyOptions = Sets.newHashSet(options);
        boolean createNew = !copyOptions.contains(StandardCopyOption.REPLACE_EXISTING);
        MCRPath src = MCRFileSystemUtils.checkPathAbsolute(source);
        MCRPath tgt = MCRFileSystemUtils.checkPathAbsolute(target);
        MCRStoredNode srcNode = MCRFileSystemUtils.resolvePath(src);
        //checkParent of target;
        if (tgt.getNameCount() == 0 && srcNode instanceof MCRDirectory srcDir) {
            MCRDirectory tgtDir = MCRFileSystemUtils.getFileCollection(tgt.getOwner());
            if (tgtDir != null) {
                if (tgtDir.hasChildren() && copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)) {
                    throw new DirectoryNotEmptyException(tgt.toString());
                }
            } else {
                MCRObjectID tgtDerId = MCRObjectID.getInstance(tgt.getOwner());
                org.mycore.datamodel.ifs2.MCRFileStore store = MCRFileSystemUtils.getStore(tgtDerId.getBase());
                MCRFileCollection tgtCollection = store.create(tgtDerId.getNumberAsInteger());
                if (copyOptions.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
                    copyDirectoryAttributes(srcDir, tgtCollection);
                }
            }
            return; //created new root component
        }
        if (srcNode instanceof MCRFile srcFile) {
            copyFile(srcFile, tgt, copyOptions, createNew);
        } else if (srcNode instanceof MCRDirectory srcDir) {
            copyDirectory(srcDir, tgt, copyOptions);
        }
    }

    private static void copyFile(MCRFile srcFile, MCRPath target, HashSet<CopyOption> copyOptions, boolean createNew)
        throws IOException {
        boolean fireCreateEvent = createNew || Files.notExists(target);
        MCRFile targetFile = MCRFileSystemUtils.getMCRFile(target, true, createNew, !fireCreateEvent);
        targetFile.setContent(srcFile.getContent());
        if (copyOptions.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
            copyFileAttributes(srcFile, targetFile);
        }
        if (fireCreateEvent) {
            MCRPathEventHelper.fireFileCreateEvent(target, targetFile.getBasicFileAttributes());
        } else {
            MCRPathEventHelper.fireFileUpdateEvent(target, targetFile.getBasicFileAttributes());
        }
    }

    private static void copyDirectory(MCRDirectory srcNode, MCRPath target, HashSet<CopyOption> copyOptions)
        throws IOException {
        MCRDirectory tgtParentDir = MCRFileSystemUtils.resolvePath(target.getParent());
        MCRStoredNode child = (MCRStoredNode) tgtParentDir.getChild(target.getFileName().toString());
        if (child != null) {
            if (!copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)) {
                throw new FileAlreadyExistsException(tgtParentDir.toString(), target.getFileName().toString(), null);
            }
            if (child instanceof MCRFile) {
                throw new NotDirectoryException(target.toString());
            }
            MCRDirectory tgtDir = (MCRDirectory) child;
            if (tgtDir.hasChildren() && copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)) {
                throw new DirectoryNotEmptyException(target.toString());
            }
            if (copyOptions.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
                copyDirectoryAttributes(srcNode, tgtDir);
            }
        } else {
            //simply create directory
            @SuppressWarnings("unused")
            MCRDirectory tgtDir = tgtParentDir.createDir(target.getFileName().toString());
            if (copyOptions.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
                copyDirectoryAttributes(srcNode, tgtDir);
            }
        }
    }

    private static void copyFileAttributes(MCRFile source, MCRFile target)
        throws IOException {
        Path targetLocalFile = target.getLocalPath();
        BasicFileAttributeView targetBasicFileAttributeView = Files.getFileAttributeView(targetLocalFile,
            BasicFileAttributeView.class);
        BasicFileAttributes srcAttr = Files.readAttributes(source.getLocalPath(), BasicFileAttributes.class);
        target.setMD5(source.getMD5());
        targetBasicFileAttributeView.setTimes(srcAttr.lastModifiedTime(), srcAttr.lastAccessTime(),
            srcAttr.creationTime());
    }

    private static void copyDirectoryAttributes(MCRDirectory source, MCRDirectory target)
        throws IOException {
        Path tgtLocalPath = target.getLocalPath();
        Path srcLocalPath = source.getLocalPath();
        BasicFileAttributes srcAttrs = Files
            .readAttributes(srcLocalPath, BasicFileAttributes.class);
        Files.getFileAttributeView(tgtLocalPath, BasicFileAttributeView.class)
            .setTimes(srcAttrs.lastModifiedTime(), srcAttrs.lastAccessTime(), srcAttrs.creationTime());
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])
     */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        HashSet<CopyOption> copyOptions = Sets.newHashSet(options);
        if (copyOptions.contains(StandardCopyOption.ATOMIC_MOVE)) {
            throw new AtomicMoveNotSupportedException(source.toString(), target.toString(),
                "ATOMIC_MOVE not supported yet");
        }
        if (Files.isDirectory(source)) {
            MCRPath src = MCRFileSystemUtils.checkPathAbsolute(source);
            MCRDirectory srcRootDirectory = MCRFileSystemUtils.getFileCollection(src.getOwner());
            if (srcRootDirectory.hasChildren()) {
                throw new IOException("Directory is not empty");
            }
        }
        copy(source, target, options);
        delete(source);
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#isSameFile(java.nio.file.Path, java.nio.file.Path)
     */
    @Override
    public boolean isSameFile(Path path, Path path2) {
        return MCRFileSystemUtils.checkPathAbsolute(path).equals(MCRFileSystemUtils.checkPathAbsolute(path2));
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#isHidden(java.nio.file.Path)
     */
    @Override
    public boolean isHidden(Path path) {
        MCRFileSystemUtils.checkPathAbsolute(path);
        return false;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getFileStore(java.nio.file.Path)
     */
    @Override
    public FileStore getFileStore(Path path) throws IOException {
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(path);
        MCRStoredNode node = MCRFileSystemUtils.resolvePath(mcrPath);
        return MCRFileStore.getInstance(node);
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#checkAccess(java.nio.file.Path, java.nio.file.AccessMode[])
     */
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(path);
        MCRStoredNode node = MCRFileSystemUtils.resolvePath(mcrPath);
        if (node == null) {
            throw new NoSuchFileException(mcrPath.toString());
        }
        if (node instanceof MCRDirectory) {
            checkDirectoryAccessModes(mcrPath, modes);
        } else {
            checkFileAccessModes(mcrPath, modes);
        }
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getFileAttributeView(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(path);
        //must support BasicFileAttributeView
        if (type == BasicFileAttributeView.class) {
            return (V) new BasicFileAttributeViewImpl(mcrPath);
        }
        if (type == MCRDigestAttributeView.class) {
            return (V) new MD5FileAttributeViewImpl(mcrPath);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#readAttributes(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
        throws IOException {
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(path);
        MCRStoredNode node = MCRFileSystemUtils.resolvePath(mcrPath);
        //must support BasicFileAttributeView
        if (type == BasicFileAttributes.class || type == MCRFileAttributes.class) {
            return (A) MCRBasicFileAttributeViewImpl.readAttributes(node);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#readAttributes(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[])
     */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        MCRPath mcrPath = MCRFileSystemUtils.checkPathAbsolute(path);
        String[] s = splitAttrName(attributes);
        if (s[0].isEmpty()) {
            throw new IllegalArgumentException(attributes);
        }
        BaseBasicFileAttributeView view = switch (s[0]) {
            case "basic" -> new BasicFileAttributeViewImpl(mcrPath);
            case "md5" -> new MD5FileAttributeViewImpl(mcrPath);
            default -> throw new UnsupportedOperationException("View '" + s[0] + "' not available");
        };
        return view.getAttributeMap(s[1].split(","));
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#setAttribute(java.nio.file.Path, java.lang.String, java.lang.Object, java.nio.file.LinkOption[])
     */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
        throw new UnsupportedOperationException("setAttributes is not implemented yet.");
    }

    /**
     * @return the MCRIFSFileSystem instance
     */
    public static MCRIFSFileSystem getMCRIFSFileSystem() {
        return (MCRIFSFileSystem) (FILE_SYSTEM_INSTANCE == null ? MCRAbstractFileSystem.getInstance(SCHEME)
            : FILE_SYSTEM_INSTANCE);
    }

    static abstract class BaseBasicFileAttributeView extends MCRBasicFileAttributeViewImpl {

        protected MCRPath path;

        BaseBasicFileAttributeView(Path path) {
            this.path = MCRPath.toMCRPath(path);
            if (!path.isAbsolute()) {
                throw new InvalidPathException(path.toString(), "'path' must be absolute.");
            }
        }

        @Override
        protected MCRStoredNode resolveNode() throws IOException {
            return MCRFileSystemUtils.resolvePath(this.path);
        }

        abstract Map<String, Object> getAttributeMap(String... attributes) throws IOException;

    }

    static class BasicFileAttributeViewImpl extends BaseBasicFileAttributeView {

        protected MCRBasicFileAttributeViewProperties<BasicFileAttributeViewImpl> properties;

        BasicFileAttributeViewImpl(Path path) {
            super(path);
            this.properties = new MCRBasicFileAttributeViewProperties<>(this);
        }

        @Override
        public Map<String, Object> getAttributeMap(String... attributes) throws IOException {
            return this.properties.getAttributeMap(attributes);
        }

    }

    private static class MD5FileAttributeViewImpl extends BaseBasicFileAttributeView
        implements MCRDigestAttributeView<String> {

        protected MD5FileAttributeViewProperties properties;

        MD5FileAttributeViewImpl(Path path) {
            super(path);
            this.properties = new MD5FileAttributeViewProperties(this);
        }

        @Override
        public MCRFileAttributes<String> readAllAttributes() throws IOException {
            return readAttributes();
        }

        @Override
        public Map<String, Object> getAttributeMap(String... attributes) throws IOException {
            return this.properties.getAttributeMap(attributes);
        }

        @Override
        public String name() {
            return "md5";
        }

    }

    private static class MD5FileAttributeViewProperties
        extends MCRBasicFileAttributeViewProperties<MD5FileAttributeViewImpl> {

        private static final String MD5_NAME = "md5";

        private static final Set<String> ALLOWED_ATTRIBUTES = Sets.union(
            MCRBasicFileAttributeViewProperties.ALLOWED_ATTRIBUTES,
            Sets.newHashSet(MD5_NAME));

        MD5FileAttributeViewProperties(MD5FileAttributeViewImpl view) {
            super(view);
        }

        @Override
        protected Map<String, Object> buildMap(Set<String> requested)
            throws IOException {
            Map<String, Object> buildMap = super.buildMap(requested);
            MCRFileAttributes<String> attrs = getView().readAttributes();
            if (requested.contains(MD5_NAME)) {
                buildMap.put(MD5_NAME, attrs.digest());
            }
            return buildMap;
        }

        @Override
        public void setAttribute(String name, Object value) throws IOException {
            if (MD5_NAME.equals(name)) {
                MCRStoredNode node = getView().resolveNode();
                if (node instanceof MCRDirectory) {
                    throw new IOException("Cannot set md5sum on directories: " + node);
                }
                ((MCRFile) node).setMD5((String) value);
                return;
            }
            super.setAttribute(name, value);
        }

        @Override
        public Set<String> getAllowedAttributes() {
            return ALLOWED_ATTRIBUTES;
        }

    }

}
