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

package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRReadOnlyIOException;
import org.mycore.datamodel.niofs.MCRVersionedFileSystemProvider;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalTempFileStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.model.ObjectVersionId;
import jakarta.inject.Singleton;

/**
 * Singleton class providing the OCFL file system implementation.
 * This class implements the file system provider for the OCFL file system, managing interactions
 * with the underlying OCFL repository and handling file operations such as reading, writing, copying,
 * and deleting files.
 */
@Singleton
public class MCROCFLFileSystemProvider extends MCRVersionedFileSystemProvider {

    public static final String SCHEME = "ocfl";

    public static final URI FS_URI = URI.create(SCHEME + ":///");

    private static volatile MCROCFLFileSystem FILE_SYSTEM;

    private MCROCFLTransactionalTempFileStorage localStorage;

    private MCROCFLVirtualObjectProvider virtualObjectProvider;

    /**
     * Constructs a new {@code MCROCFLFileSystemProvider} and initializes the file system.
     *
     * @throws IOException if an I/O error occurs during initialization.
     */
    public MCROCFLFileSystemProvider() throws IOException {
        init();
    }

    /**
     * Initializes the OCFL file system provider.
     *
     * @throws IOException if an I/O error occurs during initialization.
     */
    void init() throws IOException {
        try {
            String configurationPrefix = getRepositoryProvider().getConfigurationPrefix();
            String tempFileClassProperty = configurationPrefix + "FS.TempStorage";
            this.localStorage = MCRConfiguration2
                .getSingleInstanceOfOrThrow(MCROCFLTransactionalTempFileStorage.class, tempFileClassProperty);
            Files.createDirectories(this.localStorage.getRoot());
            this.localStorage.clearTransactional();
            boolean remote = MCRConfiguration2.getBoolean(configurationPrefix + "FS.Remote")
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(configurationPrefix + "FS.Remote"));
            this.virtualObjectProvider
                = new MCROCFLVirtualObjectProvider(getRepository(), localStorage, remote);
        } catch (Exception exception) {
            throw new IOException("Unable to create MCROCFLFileSystem.", exception);
        }
    }

    /**
     * Clears the cache of the local storage and the virtual object provider.
     * <p>Be careful to call this. This is usually not required except for unit testing.</p>
     *
     * @throws IOException if an I/O error occurs during the cache clearing.
     */
    public void clearCache() throws IOException {
        this.localStorage.clear();
        this.virtualObjectProvider.clear();
    }

    /**
     * Returns the scheme name, which is "ocfl".
     *
     * @return the scheme name.
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    /**
     * Returns the URI of the file system.
     *
     * @return the URI of the file system.
     */
    @Override
    public URI getURI() {
        return FS_URI;
    }

    /**
     * Returns the OCFL file system instance.
     *
     * @return the {@link MCROCFLFileSystem} instance.
     */
    @Override
    public MCROCFLFileSystem getFileSystem() {
        return getMCROCFLFileSystem();
    }

    /**
     * Returns the OCFL file system instance for the specified URI.
     *
     * @param uri the URI of the file system.
     * @return the {@link MCROCFLFileSystem} instance.
     */
    @Override
    public MCROCFLFileSystem getFileSystem(URI uri) {
        if (FILE_SYSTEM == null) {
            synchronized (FS_URI) {
                if (FILE_SYSTEM == null) {
                    FILE_SYSTEM = new MCROCFLFileSystem(this);
                }
            }
        }
        return FILE_SYSTEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        boolean create = options.contains(StandardOpenOption.CREATE);
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        boolean write = options.contains(StandardOpenOption.WRITE);
        boolean read = options.isEmpty() || options.contains(StandardOpenOption.READ);
        if (create || createNew) {
            checkFileName(path.getFileName().toString());
            checkOpenOption(getOpenOptions(options));
        }
        if (write && !path.isHeadVersion()) {
            throw new MCRReadOnlyIOException(
                "It's not allowed to write into a non head version. Path '" + path + " ' is '"
                    + path.getVersion() + "' but head is '" + this.getHeadVersion(path.getOwner())
                    + "'.");
        }
        MCROCFLVirtualObject virtualObject
            = read ? virtualObjectProvider().get(path) : virtualObjectProvider().getWritable(path);
        return virtualObject.newByteChannel(path, options, fileAttributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(MCRVersionedPath dir, Filter<? super Path> filter)
        throws IOException {
        MCROCFLVirtualObject virtualObject = virtualObjectProvider().get(dir);
        return virtualObject.newDirectoryStream(dir, filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(MCRVersionedPath path, FileAttribute<?>... fileAttributes) throws IOException {
        if (!path.isHeadVersion()) {
            throw new MCRReadOnlyIOException(
                "It's not allowed to create a directory on a non head version. Path '" + path + " ' is '"
                    + path.getVersion() + "' but head is '" + this.getHeadVersion(path.getOwner())
                    + "'.");
        }
        MCROCFLVirtualObject virtualObject = virtualObjectProvider().getWritable(path);
        virtualObject.createDirectory(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(MCRVersionedPath path) throws IOException {
        if (!path.isHeadVersion()) {
            throw new MCRReadOnlyIOException("It's not allowed to delete non head version. Path '" + path + " ' is '"
                + path.getVersion() + "' but head is '" + this.getHeadVersion(path.getOwner())
                + "'.");
        }
        MCROCFLVirtualObject virtualObject = virtualObjectProvider().getWritable(path);
        virtualObject.delete(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (isSameFile(source, target)) {
            return;
        }
        checkCopyOptions(options);
        if (!target.isHeadVersion()) {
            throw new MCRReadOnlyIOException(
                "It's not allowed to copy into non head version. Target '" + target + " ' is '"
                    + target.getVersion() + "' but head is '" + this.getHeadVersion(target.getOwner()) + "'.");
        }
        MCROCFLVirtualObject virtualSource = virtualObjectProvider().get(source);
        if (!virtualSource.exists(source)) {
            throw new NoSuchFileException(source.toString());
        }
        if (virtualSource.isDirectory(source)) {
            copyDirectory(target, options);
        } else {
            copyFile(source, target, options);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (isSameFile(source, target)) {
            return;
        }
        checkCopyOptions(options);
        if (!source.isHeadVersion()) {
            throw new MCRReadOnlyIOException(
                "It's not allowed to move from non head version. Source '" + source + " ' is '"
                    + source.getVersion() + "' but head is '" + this.getHeadVersion(source.getOwner()) + "'.");
        }
        if (!target.isHeadVersion()) {
            throw new MCRReadOnlyIOException(
                "It's not allowed to move into non head version. Target '" + target + " ' is '"
                    + target.getVersion() + "' but head is '" + this.getHeadVersion(target.getOwner()) + "'.");
        }
        MCROCFLVirtualObject virtualSource = virtualObjectProvider().getWritable(source);
        MCROCFLVirtualObject virtualTarget = virtualObjectProvider().getWritable(target);

        boolean sourceExists = virtualSource.exists(source);
        boolean targetExists = virtualTarget.exists(target);
        if (!sourceExists) {
            throw new NoSuchFileException(source.toString());
        }
        if (targetExists && !Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        // same virtual object
        if (virtualSource.equals(virtualTarget)) {
            virtualSource.rename(source, target, options);
            return;
        }
        // different virtual object
        copy(source, target, options);
        delete(source);
    }

    private void copyFile(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCROCFLVirtualObject virtualSource = virtualObjectProvider().get(source);
        MCROCFLVirtualObject virtualTarget = virtualObjectProvider().getWritable(target);

        boolean targetExists = virtualTarget.exists(target);
        if (targetExists && !Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        if (virtualSource.equals(virtualTarget)) {
            // same virtual object
            virtualSource.copy(source, target, options);
        } else {
            // different virtual object
            virtualSource.externalCopy(virtualTarget, source, target, options);
        }
    }

    private void copyDirectory(MCRVersionedPath target, CopyOption... options)
        throws IOException {
        MCROCFLVirtualObject virtualTarget = virtualObjectProvider().getWritable(target);
        boolean targetExists = virtualTarget.exists(target);
        boolean replaceExisting = Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING);
        if (targetExists && replaceExisting) {
            boolean targetIsDirectory = virtualTarget.isDirectory(target);
            boolean targetDirectoryIsEmpty = virtualTarget.isDirectoryEmpty(target);
            if (targetIsDirectory && !targetDirectoryIsEmpty) {
                throw new DirectoryNotEmptyException(target.toString());
            }
        }
        createDirectory(target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileStore getFileStore(MCRVersionedPath path) {
        return getFileSystem().getFileStore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAccess(MCRVersionedPath path, AccessMode... accessModes) throws IOException {
        try {
            MCROCFLVirtualObject virtualObject = virtualObjectProvider().get(path);
            if (virtualObject.isMarkedForPurge()) {
                throw new NoSuchFileException(path.toString());
            }
            if (!virtualObject.exists(path)) {
                throw new NoSuchFileException(path.toString());
            }
            if (virtualObject.isDirectory(path)) {
                checkDirectoryAccessModes(path);
            } else {
                checkFileAccessModes(path);
            }
        } catch (NotFoundException notFoundException) {
            throw new NoSuchFileException(path.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... linkOptions) {
        if (!BasicFileAttributeView.class.isAssignableFrom(type)) {
            return null;
        }
        return (V) new MCROCFLBasicFileAttributeView(MCRVersionedPath.toVersionedPath(path));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... linkOptions)
        throws IOException {
        if (!BasicFileAttributes.class.isAssignableFrom(type)) {
            return null;
        }
        return (A) getFileAttributeView(path, MCROCFLBasicFileAttributeView.class, linkOptions).readAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... linkOptions)
        throws IOException {
        String[] s = splitAttrName(attributes);
        if (s[0].isEmpty()) {
            throw new IllegalArgumentException(attributes);
        }
        if (!"basic".equals(s[0])) {
            throw new UnsupportedOperationException("View '" + s[0] + "' not available");
        }
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(path);
        return new MCROCFLBasicFileAttributeView(versionedPath).getAttributeMap(s[1].split(","));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... linkOptions) {
        throw new UnsupportedOperationException("setAttributes is not implemented yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeadVersion(String owner) {
        if (owner == null || owner.isEmpty()) {
            return null;
        }
        try {
            String objectId = MCROCFLObjectIDPrefixHelper.MCRDERIVATE + owner;
            return getRepository().describeObject(objectId).getHeadVersion().getVersionNum().toString();
        } catch (NotFoundException ignore) {
            return null;
        }
    }

    /**
     * Checks if the specified version ID is the head version.
     *
     * @param versionId the version ID.
     * @return {@code true} if the version ID is the head version, {@code false} otherwise.
     */
    public boolean isHeadVersion(ObjectVersionId versionId) {
        if (versionId.getVersionNum() == null) {
            return true;
        }
        String objectId = versionId.getObjectId();
        String owner = MCROCFLObjectIDPrefixHelper.fromDerivateObjectId(objectId);
        return this.isHeadVersion(owner, versionId.getVersionNum().toString());
    }

    /**
     * Returns the repository ID.
     *
     * @return the repository ID.
     */
    public String getRepositoryId() {
        return MCRConfiguration2.getString("MCR.Content.Manager.Repository").orElseThrow();
    }

    /**
     * Returns the repository provider.
     *
     * @return the {@link MCROCFLRepositoryProvider}.
     */
    public MCROCFLRepositoryProvider getRepositoryProvider() {
        return MCROCFLRepositoryProvider.getProvider(getRepositoryId());
    }

    /**
     * Returns the OCFL repository.
     *
     * @return the {@link MCROCFLRepository}.
     * @throws ClassCastException if the repository is not of the expected type.
     */
    public MCROCFLRepository getRepository() throws ClassCastException {
        return getRepositoryProvider().getRepository();
    }

    /**
     * Returns the virtual object provider.
     *
     * @return the {@link MCROCFLVirtualObjectProvider}.
     */
    public MCROCFLVirtualObjectProvider virtualObjectProvider() {
        return virtualObjectProvider;
    }

    /**
     * Returns the local storage.
     *
     * @return the {@link MCROCFLTransactionalTempFileStorage}.
     */
    public MCROCFLTransactionalTempFileStorage localStorage() {
        return this.localStorage;
    }

    /**
     * Returns the OCFL file system instance.
     *
     * @return the {@link MCROCFLFileSystem} instance.
     * @throws FileSystemNotFoundException if the OCFL file system is not available.
     */
    public static MCROCFLFileSystem getMCROCFLFileSystem() throws FileSystemNotFoundException {
        return FILE_SYSTEM != null ? FILE_SYSTEM : (MCROCFLFileSystem) MCRAbstractFileSystem.getInstance(SCHEME);
    }

    /**
     * Returns the OCFL file system provider instance.
     *
     * @return the {@link MCROCFLFileSystemProvider} instance.
     */
    public static MCROCFLFileSystemProvider get() {
        return getMCROCFLFileSystem().provider();
    }

}
