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

package org.mycore.ocfl.niofs;

import static org.mycore.ocfl.util.MCROCFLVersionHelper.MESSAGE_UPDATED;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRVersionedFileSystem;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

import io.ocfl.api.OcflRepository;

/**
 * Implementation of {@link MCRVersionedFileSystem} for OCFL-based file systems.
 * This class provides methods to manage OCFL virtual objects, including creating and removing roots,
 * and listing root directories and file stores.
 */
public class MCROCFLFileSystem extends MCRVersionedFileSystem {

    private static volatile MCROCFLFileStore fileStore;

    /**
     * Constructs a new {@code MCROCFLFileSystem} with the specified OCFL provider.
     *
     * @param ocflProvider the OCFL file system provider.
     */
    public MCROCFLFileSystem(MCROCFLFileSystemProvider ocflProvider) {
        super(ocflProvider);
    }

    /**
     * Returns the OCFL file system provider.
     *
     * @return the {@link MCROCFLFileSystemProvider}.
     */
    @Override
    public MCROCFLFileSystemProvider provider() {
        return (MCROCFLFileSystemProvider) super.provider();
    }

    /**
     * Creates a root directory for the specified owner.
     *
     * @param owner the owner of the root directory.
     * @throws FileSystemException if the root directory cannot be created or already exists.
     */
    @Override
    public void createRoot(String owner) throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = provider().virtualObjectProvider();
        if (virtualObjectProvider.exists(owner)) {
            throw new FileAlreadyExistsException(owner);
        }
        MCROCFLVirtualObject virtualObject = virtualObjectProvider.getWritable(owner);
        try {
            virtualObject.create();
        } catch (IOException ioException) {
            FileSystemException fileSystemException =
                new FileSystemException(null, null, "Cannot create root of '" + owner + "'.");
            fileSystemException.initCause(ioException);
            throw fileSystemException;
        }
    }

    /**
     * Removes the root directory for the specified owner.
     *
     * @param owner the owner of the root directory.
     * @throws FileSystemException if the root directory cannot be removed.
     */
    @Override
    public void removeRoot(String owner) throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = provider().virtualObjectProvider();
        if (!virtualObjectProvider.exists(owner)) {
            return;
        }
        MCROCFLVirtualObject virtualObject = virtualObjectProvider.getWritable(owner);
        try {
            virtualObject.purge();
        } catch (IOException ioException) {
            FileSystemException fileSystemException =
                new FileSystemException(null, null, "Cannot remove root of '" + owner + "'.");
            fileSystemException.initCause(ioException);
            throw fileSystemException;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Be aware that this OCFL implementation works outside the {@link MCROCFLFileSystemTransaction}.
     */
    @Override
    public void restoreRoot(String owner, String version) throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = provider().virtualObjectProvider();
        if (!virtualObjectProvider.exists(owner, version)) {
            throw new NoSuchFileException(null, null, "No such version '" + version + "' for root '" + owner + "'.");
        }
        try {
            MCROCFLRepository repository = provider().getRepository();
            String ocflObjectId = MCROCFLObjectIDPrefixHelper.toDerivateObjectId(owner);
            repository.replicateVersionAsHead(ObjectVersionId.version(ocflObjectId, version),
                new VersionInfo().setMessage(MESSAGE_UPDATED));
        } catch (Exception exc) {
            FileSystemException fileSystemException =
                new FileSystemException(null, null, "Unable to restore '" + owner + "' to version '" + version + "'.");
            fileSystemException.initCause(exc);
            throw fileSystemException;
        }
    }

    /**
     * Returns an iterable over the root directories in the OCFL file system.
     *
     * @return an iterable over the root directories.
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        MCROCFLVirtualObjectProvider virtualObjectProvider = provider().virtualObjectProvider();
        Collection<MCROCFLVirtualObject> writables = virtualObjectProvider.collectWritables();
        // created objects
        Stream<String> createdObjectStream = writables.stream()
            .filter(MCROCFLVirtualObject::isMarkedForCreate)
            .map(MCROCFLVirtualObject::getOwner);
        // deleted objects
        List<String> deletedObjects = writables.stream()
            .filter(MCROCFLVirtualObject::isMarkedForPurge)
            .map(MCROCFLVirtualObject::getOwner)
            .toList();
        // objects from repository
        OcflRepository repository = provider().getRepository();
        Stream<String> repositoryObjectStream = repository.listObjectIds()
            .filter(MCROCFLObjectIDPrefixHelper::isDerivateObjectId)
            .map(MCROCFLObjectIDPrefixHelper::fromDerivateObjectId)
            .filter(MCRObjectID::isValid)
            .map(MCRObjectID::getInstance)
            .map(MCRObjectID::toString)
            .filter(virtualObjectProvider::exists)
            .filter(owner -> !virtualObjectProvider.isDeleted(owner));

        return () -> Stream.concat(createdObjectStream, repositoryObjectStream)
            .filter(owner -> !deletedObjects.contains(owner))
            .map(owner -> Path.of(MCROCFLFileSystemProvider.FS_URI.resolve(owner)))
            .iterator();
    }

    /**
     * Returns an iterable over the file stores in the OCFL file system.
     *
     * @return an iterable over the file stores.
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singleton(getFileStore());
    }

    /**
     * Returns the file store for the OCFL file system.
     *
     * @return the {@link MCROCFLFileStore}.
     */
    public MCROCFLFileStore getFileStore() {
        if (fileStore == null) {
            synchronized (MCROCFLFileSystem.class) {
                if (fileStore == null) {
                    fileStore = buildFileStore();
                }
            }
        }
        return fileStore;
    }

    /**
     * Resets the fileStore.
     * <p>
     * This should only be used in unit testing.
     */
    public void resetFileStore() {
        synchronized (MCROCFLFileSystem.class) {
            fileStore = null;
        }
    }

    private MCROCFLFileStore buildFileStore() {
        MCROCFLFileSystemProvider fileSystemProvider = provider();
        MCROCFLRepository repository = fileSystemProvider.getRepository();
        return repository.isRemote() ? new MCROCFLFileStore(fileSystemProvider)
            : new MCROCFLLocalFileStore(fileSystemProvider);
    }

}
