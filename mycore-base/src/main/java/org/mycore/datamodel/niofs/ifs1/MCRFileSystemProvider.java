/*
 * $Id$
 * $Revision: 5697 $ $Date: Jun 27, 2014 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.niofs.ifs1;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRMD5AttributeView;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileSystemProvider extends FileSystemProvider {

    public static final String SCHEME = "ifs";

    public static final URI FS_URI = URI.create(SCHEME + ":///");

    private static MCRAbstractFileSystem FILE_SYSTEM_INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(MCRFileSystemProvider.class);

    private static final Set<? extends CopyOption> SUPPORTED_COPY_OPTIONS = Collections.unmodifiableSet(EnumSet.of(
        StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING));

    /**
     * 
     */
    public MCRFileSystemProvider() {
        //TODO: One filesystem enough?
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getScheme()
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#newFileSystem(java.net.URI, java.util.Map)
     */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new FileSystemAlreadyExistsException();
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getFileSystem(java.net.URI)
     */
    @Override
    public MCRIFSFileSystem getFileSystem(URI uri) {
        if (FILE_SYSTEM_INSTANCE == null) {
            synchronized (this) {
                if (FILE_SYSTEM_INSTANCE == null) {
                    FILE_SYSTEM_INSTANCE = new MCRIFSFileSystem(this);
                }
            }
        }
        return getMCRIFSFileSystem();
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getPath(java.net.URI)
     */
    @Override
    public Path getPath(final URI uri) {
        if (!FS_URI.getScheme().equals(Objects.requireNonNull(uri).getScheme())) {
            throw new FileSystemNotFoundException("Unkown filesystem: " + uri);
        }
        String path = uri.getPath();
        String owner = null;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == MCRAbstractFileSystem.SEPARATOR) {
                break;
            }
            if (path.charAt(i) == ':') {
                owner = path.substring(0, i);
                path = path.substring(i + 1);
                break;
            }

        }
        return MCRAbstractFileSystem.getPath(owner, path, getFileSystemFromPathURI(FS_URI));
    }

    private MCRIFSFileSystem getFileSystemFromPathURI(final URI uri) {
        return getMCRIFSFileSystem();
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
        MCRPath ifsPath = checkPathAbsolute(path);
        Set<? extends OpenOption> fileOpenOptions = Sets.filter(options, new Predicate<OpenOption>() {
            @Override
            public boolean apply(OpenOption option) {
                return !(option == StandardOpenOption.CREATE || option == StandardOpenOption.CREATE_NEW);
            }
        });
        boolean create = options.contains(StandardOpenOption.CREATE);
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        if (create || createNew) {
            for (OpenOption option : fileOpenOptions) {
                //check before we create any file instance
                MCRFile.checkOpenOption(option);
            }
        }
        MCRFile mcrFile = getMCRFile(ifsPath, create, createNew);
        if (mcrFile == null) {
            throw new NoSuchFileException(path.toString());
        }
        return mcrFile.getFileChannel(fileOpenOptions);
    }

    static MCRDirectory getRootDirectory(MCRPath ifsPath) throws NoSuchFileException {
        LOGGER.info("Get root directory of " + ifsPath.getOwner());
        MCRDirectory root = MCRDirectory.getRootDirectory(ifsPath.getOwner());
        if (root == null) {
            throw new NoSuchFileException(ifsPath.toString(), null, "Could not get root directory.");
        }
        return root;
    }

    private MCRFile getMCRFile(MCRPath ifsPath, boolean create, boolean createNew) throws IOException {
        if (!ifsPath.isAbsolute()) {
            throw new IllegalArgumentException("'path' needs to be absolute.");
        }
        MCRFile file;
        MCRDirectory root = null;
        boolean rootCreated = false;
        try {
            try {
                root = getRootDirectory(ifsPath);
            } catch (NoSuchFileException e) {
                if (create || createNew) {
                    root = new MCRDirectory(ifsPath.getOwner());
                    rootCreated = true;
                } else {
                    throw e;
                }
            }
            MCRPath relativePath = root.toPath().relativize(ifsPath);
            file = getMCRFile(root, relativePath, create, createNew);
        } catch (Exception e) {
            if (rootCreated) {
                LOGGER.error("Exception while getting MCRFile " + ifsPath + ". Removing created filesystem nodes.");
                try {
                    root.delete();
                } catch (Exception de) {
                    LOGGER.fatal("Error while deleting file system node: " + root.getName(), de);
                }
            }
            throw e;
        }
        return file;
    }

    MCRFile getMCRFile(MCRDirectory baseDir, MCRPath relativePath, boolean create, boolean createNew)
        throws IOException {
        MCRPath ifsPath = relativePath;
        if (relativePath.isAbsolute()) {
            if (baseDir.getOwnerID().equals(relativePath.getOwner())) {
                ifsPath = baseDir.toPath().relativize(relativePath);
            } else
                throw new IOException(relativePath + " is absolute does not fit to " + baseDir.toPath());
        }
        Deque<MCRFilesystemNode> created = new LinkedList<>();
        MCRFile file;
        try {
            file = (MCRFile) baseDir.getChildByPath(ifsPath.toString());
            if (file != null && createNew) {
                throw new FileAlreadyExistsException(baseDir.toPath().resolve(ifsPath).toString());
            }
            if (file == null & (create || createNew)) {
                Path normalized = ifsPath.normalize();
                MCRDirectory parent = baseDir;
                int nameCount = normalized.getNameCount();
                int directoryCount = nameCount - 1;
                int i = 0;
                while (i < directoryCount) {
                    String curName = normalized.getName(i).toString();
                    MCRDirectory curDir = (MCRDirectory) parent.getChild(curName);
                    if (curDir == null) {
                        curDir = new MCRDirectory(curName, parent);
                        created.addFirst(curDir);
                    }
                    i++;
                    parent = curDir;
                }
                String fileName = normalized.getFileName().toString();
                file = new MCRFile(fileName, parent);
                created.addFirst(file);
            }
        } catch (Exception e) {
            if (create || createNew) {
                LOGGER.error("Exception while getting MCRFile " + ifsPath + ". Removing created filesystem nodes.");
                while (created.peekFirst() != null) {
                    MCRFilesystemNode node = created.pollFirst();
                    try {
                        node.delete();
                    } catch (Exception de) {
                        LOGGER.fatal("Error while deleting file system node: " + node.getName(), de);
                    }
                }
            }
            throw e;
        }
        return file;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream.Filter)
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        MCRPath mcrPath = checkPathAbsolute(dir);
        MCRDirectory mcrDir = null;
        return new MCRDirectoryStream(mcrDir);
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#createDirectory(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])
     */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        if (attrs.length > 0) {
            throw new UnsupportedOperationException("Setting 'attrs' atomically is unsupported.");
        }
        MCRPath mcrPath = checkPathAbsolute(dir);
        MCRDirectory rootDirectory;
        if (mcrPath.isAbsolute() && mcrPath.getNameCount() == 0) {
            rootDirectory = MCRDirectory.getDirectory(mcrPath.getOwner());
            if (rootDirectory != null) {
                throw new FileAlreadyExistsException(mcrPath.toString());
            }
            rootDirectory = new MCRDirectory(mcrPath.getOwner());
            return;
        }
        rootDirectory = getRootDirectory(mcrPath);
        MCRPath parentPath = mcrPath.getParent();
        MCRPath absolutePath = getAbsolutePathFromRootComponent(parentPath);
        MCRFilesystemNode childByPath = rootDirectory.getChildByPath(absolutePath.toString());
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
        new MCRDirectory(dirName, parentDir);
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
        MCRPath mcrPath = checkPathAbsolute(path);
        MCRDirectory parentDir = getParentDirectory(mcrPath);
        MCRFilesystemNode child = parentDir.getChild(mcrPath.getFileName().toString());
        if (child == null) {
            throw new NoSuchFileException(mcrPath.toString(), null, null);
        }
        if (child instanceof MCRDirectory) {
            if (((MCRDirectory) child).hasChildren()) {
                throw new DirectoryNotEmptyException(mcrPath.toString());
            }
        }
        try {
            child.delete();
        } catch (RuntimeException e) {
            throw new IOException("Could not delete: " + mcrPath.toString(), e);
        }
    }

    private static MCRDirectory getParentDirectory(MCRPath mcrPath) throws NoSuchFileException, NotDirectoryException {
        if (mcrPath.getNameCount() == 0) {
            throw new IllegalArgumentException("Root component has no parent: " + mcrPath.toString());
        }
        MCRDirectory rootDirectory = getRootDirectory(mcrPath);
        if (mcrPath.getNameCount() == 1) {
            return rootDirectory;
        }
        MCRPath parentPath = mcrPath.getParent();
        MCRFilesystemNode parentNode = rootDirectory.getChildByPath(getAbsolutePathFromRootComponent(parentPath)
            .toString());
        if (parentNode == null) {
            throw new NoSuchFileException(rootDirectory.toPath().toString(), getAbsolutePathFromRootComponent(mcrPath)
                .toString(), "Parent directory does not exists.");
        }
        if (parentNode instanceof MCRFile) {
            throw new NotDirectoryException(parentNode.toPath().toString());
        }
        return (MCRDirectory) parentNode;
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
        MCRPath src = checkPathAbsolute(source);
        MCRPath tgt = checkPathAbsolute(target);
        MCRDirectory srcRootDirectory = getRootDirectory(src);
        MCRPath srcAbsolutePath = getAbsolutePathFromRootComponent(src);
        MCRFilesystemNode srcNode = srcRootDirectory.getChildByPath(srcAbsolutePath.toString());
        //checkParent of target;
        if (tgt.getNameCount() == 0 && srcNode instanceof MCRDirectory) {
            MCRDirectory tgtDir = MCRDirectory.getRootDirectory(tgt.getOwner());
            if (tgtDir != null) {
                if (tgtDir.hasChildren() && copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)) {
                    throw new DirectoryNotEmptyException(tgt.toString());
                }
            } else {
                //TODO: handle StandardCopyOption.COPY_ATTRIBUTES
                tgtDir = new MCRDirectory(tgt.getOwner());
            }
            return; //created new root component
        }
        MCRDirectory tgtParentDir = getParentDirectory(tgt);
        if (srcNode instanceof MCRFile) {
            MCRFile srcFile = (MCRFile) srcNode;
            MCRFile targetFile = getMCRFile(tgt, true, createNew);
            targetFile.setContentFrom(srcFile.getContentAsInputStream());
            if (copyOptions.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
                @SuppressWarnings("unchecked")
                MCRMD5AttributeView<String> srcAttrView = Files.getFileAttributeView(src, MCRMD5AttributeView.class,
                    (LinkOption[]) null);
                File targetLocalFile = targetFile.getLocalFile();
                BasicFileAttributeView targetBasicFileAttributeView = Files.getFileAttributeView(
                    targetLocalFile.toPath(), BasicFileAttributeView.class, (LinkOption[]) null);
                MCRFileAttributes<String> srcAttr = srcAttrView.readAllAttributes();
                targetFile.adjustMetadata(srcAttr.lastModifiedTime(), srcFile.getMD5(), srcFile.getSize());
                targetBasicFileAttributeView.setTimes(srcAttr.lastModifiedTime(), srcAttr.lastAccessTime(),
                    srcAttr.creationTime());
            }
        } else if (srcNode instanceof MCRDirectory) {
            MCRFilesystemNode child = tgtParentDir.getChild(tgt.getFileName().toString());
            if (child != null) {
                if (!copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)) {
                    throw new FileAlreadyExistsException(tgtParentDir.toString(), tgt.getFileName().toString(), null);
                }
                if (child instanceof MCRFile) {
                    throw new NotDirectoryException(tgt.toString());
                }
                MCRDirectory tgtDir = (MCRDirectory) child;
                if (tgtDir.hasChildren() && copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)) {
                    throw new DirectoryNotEmptyException(tgt.toString());
                }
                //TODO: handle StandardCopyOption.COPY_ATTRIBUTES
            } else {
                //simply create directory
                @SuppressWarnings("unused")
                MCRDirectory tgtDir = new MCRDirectory(tgt.getFileName().toString(), tgtParentDir);
                //TODO: handle StandardCopyOption.COPY_ATTRIBUTES
            }
        }
    }

    private void checkCopyOptions(CopyOption[] options) {
        for (CopyOption option : options) {
            if (!SUPPORTED_COPY_OPTIONS.contains(option)) {
                throw new UnsupportedOperationException("Unsupported copy option: " + option);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])
     */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#isSameFile(java.nio.file.Path, java.nio.file.Path)
     */
    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return checkPathAbsolute(path).equals(checkPathAbsolute(path2));
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#isHidden(java.nio.file.Path)
     */
    @Override
    public boolean isHidden(Path path) throws IOException {
        checkPathAbsolute(path);
        return false;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getFileStore(java.nio.file.Path)
     */
    @Override
    public FileStore getFileStore(Path path) throws IOException {
        MCRPath mcrPath = checkPathAbsolute(path);
        if (mcrPath.getNameCount() > 0) {
            MCRDirectory parentDirectory = getParentDirectory(mcrPath);
            MCRFilesystemNode node = parentDirectory.getChild(mcrPath.getFileName().toString());
            if (node == null) {
                throw new NoSuchFileException(mcrPath.toString());
            }
            if (node instanceof MCRFile) {
                MCRFile file = (MCRFile) node;
                String storeID = file.getStoreID();
                return MCRFileStore.getInstance(storeID);
            }
        }
        return MCRFileStore.getInstance(MCRContentStoreFactory.getDefaultStore().getID());
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#checkAccess(java.nio.file.Path, java.nio.file.AccessMode[])
     */
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        MCRPath mcrPath = checkPathAbsolute(path);
        if (mcrPath.getNameCount() == 0) {
            //handle root component
            MCRDirectory rootDirectory = MCRDirectory.getRootDirectory(mcrPath.getOwner());
            checkDirectory(rootDirectory, modes);
            return;
        }
        MCRDirectory parent = getParentDirectory(mcrPath);
        MCRFilesystemNode node = parent.getChild(mcrPath.getFileName().toString());
        if (node == null) {
            throw new NoSuchFileException(mcrPath.toString());
        }
        if (node instanceof MCRDirectory) {
            checkDirectory((MCRDirectory) node, modes);
        } else {
            checkFile((MCRFile) node, modes);
        }
    }

    private void checkDirectory(MCRDirectory rootDirectory, AccessMode... modes) throws AccessDeniedException {
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                case WRITE:
                case EXECUTE:
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported AccessMode: " + mode);
            }
        }
    }

    private void checkFile(MCRFile file, AccessMode... modes) throws AccessDeniedException {
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                case WRITE:
                    break;
                case EXECUTE:
                    throw new AccessDeniedException(file.toPath().toString(), null, "Unsupported AccessMode: " + mode);
                default:
                    throw new UnsupportedOperationException("Unsupported AccessMode: " + mode);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getFileAttributeView(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        MCRPath mcrPath = checkPathAbsolute(path);
        if (type == null) {
            throw new NullPointerException();
        }
        Class<?> c = type;
        //must support BasicFileAttributeView
        if (c == BasicFileAttributeView.class) {
            return (V) new BasicFileAttributeViewImpl(mcrPath);
        }
        if (c == MCRMD5AttributeView.class) {
            return (V) new MD5FileAttributeViewImpl(mcrPath);
        }
        return (V) null;
    }

    private static MCRPath checkPathAbsolute(Path path) {
        MCRPath mcrPath = MCRPath.toMCRPath(path);
        if (!(Objects.requireNonNull(mcrPath.getFileSystem(), "'path' requires a associated filesystem.").provider() instanceof MCRFileSystemProvider)) {
            throw new ProviderMismatchException("Path does not match to this provider: " + path.toString());
        }
        if (!mcrPath.isAbsolute()) {
            throw new InvalidPathException(mcrPath.toString(), "'path' must be absolute.");
        }
        return mcrPath;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#readAttributes(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
        throws IOException {
        MCRPath mcrPath = checkPathAbsolute(path);
        MCRFilesystemNode node = resolvePath(mcrPath);
        Class<?> c = type;
        //must support BasicFileAttributeView
        if (c == BasicFileAttributes.class || c == MCRFileAttributes.class) {
            return (A) MCRBasicFileAttributeViewImpl.readAttributes(node);
        }
        return (A) null;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#readAttributes(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[])
     */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        // TODO Auto-generated method stub
        MCRPath mcrPath = checkPathAbsolute(path);
        String[] s = splitAttrName(attributes);
        if (s[0].length() == 0) {
            throw new IllegalArgumentException(attributes);
        }
        BasicFileAttributeViewImpl view = null;
        switch (s[0]) {
            case "basic":
                view = new BasicFileAttributeViewImpl(mcrPath);
                break;
            case "md5":
                view = new MD5FileAttributeViewImpl(mcrPath);
                break;
        }
        if (view == null) {
            throw new UnsupportedOperationException("View '" + s[0] + "' not available");
        }
        return view.getAttributeMap(s[1].split(","));
    }

    private static String[] splitAttrName(String attribute) {
        String[] s = new String[2];
        int pos = attribute.indexOf(':');
        if (pos == -1) {
            s[0] = "basic";
            s[1] = attribute;
        } else {
            s[0] = attribute.substring(0, pos++);
            s[1] = (pos == attribute.length()) ? "" : attribute.substring(pos);
        }
        return s;
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#setAttribute(java.nio.file.Path, java.lang.String, java.lang.Object, java.nio.file.LinkOption[])
     */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

    static MCRIFSFileSystem getMCRIFSFileSystem() {
        return (MCRIFSFileSystem) (FILE_SYSTEM_INSTANCE == null ? FileSystems.getFileSystem(FS_URI)
            : FILE_SYSTEM_INSTANCE);
    }

    private static MCRFilesystemNode resolvePath(MCRPath path) throws NoSuchFileException, NotDirectoryException,
        IOException {
        if (path.getNameCount() == 0) {
            //root components
            MCRDirectory rootDirectory = MCRDirectory.getRootDirectory(path.getOwner());
            if (rootDirectory == null) {
                throw new NoSuchFileException(path.toString());
            }
            return rootDirectory;
        }
        MCRDirectory parent = getParentDirectory(path);
        MCRFilesystemNode child;
        try {
            child = parent.getChild(path.getFileName().toString());
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
        if (child == null) {
            throw new NoSuchFileException(parent.toPath().toString(), path.toString(), null);
        }
        return child;
    }

    static class BasicFileAttributeViewImpl extends MCRBasicFileAttributeViewImpl {
        private static final String SIZE_NAME = "size";

        private static final String CREATION_TIME_NAME = "creationTime";

        private static final String LAST_ACCESS_TIME_NAME = "lastAccessTime";

        private static final String LAST_MODIFIED_TIME_NAME = "lastModifiedTime";

        private static final String FILE_KEY_NAME = "fileKey";

        private static final String IS_DIRECTORY_NAME = "isDirectory";

        private static final String IS_REGULAR_FILE_NAME = "isRegularFile";

        private static final String IS_SYMBOLIC_LINK_NAME = "isSymbolicLink";

        private static final String IS_OTHER_NAME = "isOther";

        private static HashSet<String> allowedAttr = Sets.newHashSet("*", SIZE_NAME, CREATION_TIME_NAME,
            LAST_ACCESS_TIME_NAME, LAST_MODIFIED_TIME_NAME, FILE_KEY_NAME, IS_DIRECTORY_NAME, IS_REGULAR_FILE_NAME,
            IS_SYMBOLIC_LINK_NAME, IS_OTHER_NAME);

        protected MCRPath path;

        public BasicFileAttributeViewImpl(Path path) {
            this.path = MCRPath.toMCRPath(path);
            if (!path.isAbsolute()) {
                throw new InvalidPathException(path.toString(), "'path' must be absolute.");
            }
        }

        protected MCRFilesystemNode resolveNode() throws IOException {
            return resolvePath(this.path);
        }

        public Map<String, Object> getAttributeMap(String... attributes) throws IOException {
            Set<String> allowed = getAllowedAttributes();
            boolean copyAll = false;
            for (String attr : attributes) {
                if (!allowed.contains(attr)) {
                    throw new IllegalArgumentException("'" + attr + "' not recognized");
                }
                if (attr.equals("*")) {
                    copyAll = true;
                }
            }
            Set<String> requested = copyAll ? allowed : Sets.newHashSet(attributes);
            return buildMap(requested, readAttributes());
        }

        protected Map<String, Object> buildMap(Set<String> requested, MCRFileAttributes<String> attrs) {
            HashMap<String, Object> map = new HashMap<>();
            for (String attr : map.keySet()) {
                switch (attr) {
                    case SIZE_NAME:
                        map.put(attr, attrs.size());
                        break;
                    case CREATION_TIME_NAME:
                        map.put(attr, attrs.creationTime());
                        break;
                    case LAST_ACCESS_TIME_NAME:
                        map.put(attr, attrs.lastAccessTime());
                        break;
                    case LAST_MODIFIED_TIME_NAME:
                        map.put(attr, attrs.lastModifiedTime());
                        break;
                    case FILE_KEY_NAME:
                        map.put(attr, attrs.fileKey());
                        break;
                    case IS_DIRECTORY_NAME:
                        map.put(attr, attrs.isDirectory());
                        break;
                    case IS_REGULAR_FILE_NAME:
                        map.put(attr, attrs.isRegularFile());
                        break;
                    case IS_SYMBOLIC_LINK_NAME:
                        map.put(attr, attrs.isSymbolicLink());
                        break;
                    case IS_OTHER_NAME:
                        map.put(attr, attrs.isOther());
                        break;
                    default:
                        //ignored
                        break;
                }
            }
            return map;
        }

        public void setAttribute(String name, Object value) throws IOException {
            Set<String> allowed = getAllowedAttributes();
            if ("*".equals(name) || !allowed.contains(name)) {
                throw new IllegalArgumentException("'" + name + "' not recognized");
            }
            switch (name) {
                case CREATION_TIME_NAME:
                    this.setTimes(null, null, (FileTime) value);
                    break;
                case LAST_ACCESS_TIME_NAME:
                    this.setTimes(null, (FileTime) value, null);
                    break;
                case LAST_MODIFIED_TIME_NAME:
                    this.setTimes((FileTime) value, null, null);
                    break;
                case SIZE_NAME:
                case FILE_KEY_NAME:
                case IS_DIRECTORY_NAME:
                case IS_REGULAR_FILE_NAME:
                case IS_SYMBOLIC_LINK_NAME:
                case IS_OTHER_NAME:
                    throw new IllegalArgumentException("'" + name + "' is a read-only attribute.");
                default:
                    //ignored
                    break;
            }

        }

        protected Set<String> getAllowedAttributes() {
            return allowedAttr;
        }
    }

    private static class MD5FileAttributeViewImpl extends BasicFileAttributeViewImpl implements
        MCRMD5AttributeView<String> {

        private static String MD5_NAME = "md5";

        private static Set<String> allowedAttr = Sets.union(BasicFileAttributeViewImpl.allowedAttr,
            Sets.newHashSet(MD5_NAME));

        public MD5FileAttributeViewImpl(Path path) {
            super(path);
        }

        @Override
        public MCRFileAttributes<String> readAllAttributes() throws IOException {
            return readAttributes();
        }

        @Override
        public String name() {
            return "md5";
        }

        @Override
        protected Set<String> getAllowedAttributes() {
            return allowedAttr;
        }

        @Override
        protected Map<String, Object> buildMap(Set<String> requested, MCRFileAttributes<String> attrs) {
            Map<String, Object> buildMap = super.buildMap(requested, attrs);
            if (requested.contains(MD5_NAME)) {
                buildMap.put(MD5_NAME, attrs.md5sum());
            }
            return buildMap;
        }

        @Override
        public void setAttribute(String name, Object value) throws IOException {
            if (MD5_NAME.equals(name)) {
                MCRFilesystemNode node = resolveNode();
                if (node instanceof MCRDirectory) {
                    throw new IOException("Cannot set md5sum on directories: " + path.toString());
                }
                ((MCRFile) node).adjustMetadata(null, (String) value, node.getSize());
            } else {
                super.setAttribute(name, value);
            }
        }

    }

}
