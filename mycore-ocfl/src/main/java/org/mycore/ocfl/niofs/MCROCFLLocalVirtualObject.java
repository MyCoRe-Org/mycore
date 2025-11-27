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

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Set;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;

import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

/**
 * A concrete implementation of {@link MCROCFLVirtualObject} for OCFL repositories stored on a <b>local filesystem</b>.
 * <p>
 * This class optimizes file operations by leveraging direct access to the files within the OCFL storage root.
 * Unlike its remote counterpart, it can read files directly from their persisted location in the OCFL repository
 * without needing to copy them to a separate cache first.
 * <h2>Key Optimizations:</h2>
 * <ul>
 *   <li>
 *       <b>Direct Read Access:</b> For read operations on unmodified files, this class returns a
 *       {@link SeekableByteChannel} that points directly to the file inside the OCFL object's version directory.
 *       This is highly efficient as it avoids unnecessary file copying.
 *   </li>
 *   <li>
 *       <b>Physical Path Resolution:</b> It can resolve a virtual {@link MCRVersionedPath} to a physical {@link Path}
 *       on the local disk, enabling integration with other tools that operate on standard file paths.
 *   </li>
 *   <li>
 *       <b>Transactional Copy-on-Write:</b> When a file is opened for writing, it is first copied from its persistent
 *       OCFL location into the transactional staging area. Further modifications are made to this copy, preserving
 *       the integrity of the original OCFL version until the transaction is committed.
 *   </li>
 * </ul>
 *
 * @see MCROCFLVirtualObject
 * @see MCROCFLRemoteVirtualObject
 */
public class MCROCFLLocalVirtualObject extends MCROCFLVirtualObject {

    /**
     * Constructs a new {@code MCROCFLLocalVirtualObject} for an object that is not yet loaded.
     *
     * @param repository           the OCFL repository.
     * @param objectVersionId      the versioned ID of the object.
     * @param transactionalStorage the local transactional file storage for modifications.
     * @param digestCalculator     the calculator for generating file digests.
     * @param readonly             whether the object is read-only.
     */
    public MCROCFLLocalVirtualObject(MCROCFLRepository repository, ObjectVersionId objectVersionId,
        MCROCFLTransactionalStorage transactionalStorage, MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator,
        boolean readonly) {
        super(repository, objectVersionId, transactionalStorage, digestCalculator, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLLocalVirtualObject} with a preloaded OCFL object version.
     *
     * @param repository           the OCFL repository.
     * @param objectVersion        the OCFL object version.
     * @param transactionalStorage the local transactional file storage for modifications.
     * @param digestCalculator     the calculator for generating file digests.
     * @param readonly             whether the object is read-only.
     */
    public MCROCFLLocalVirtualObject(MCROCFLRepository repository, OcflObjectVersion objectVersion,
        MCROCFLTransactionalStorage transactionalStorage, MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator,
        boolean readonly) {
        super(repository, objectVersion, transactionalStorage, digestCalculator, readonly);
    }

    /**
     * Internal constructor used for cloning the virtual object.
     *
     * @param repository           the OCFL repository.
     * @param versionId            the versioned ID of the object.
     * @param objectVersion        the OCFL object version.
     * @param transactionalStorage the local transactional file storage.
     * @param readonly             whether the object is read-only.
     * @param digestCalculator     the calculator for generating file digests.
     * @param fileTracker          the file tracker.
     * @param directoryTracker     the directory tracker.
     */
    protected MCROCFLLocalVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTransactionalStorage transactionalStorage,
        MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator, boolean readonly,
        MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLDirectoryTracker directoryTracker) {
        super(repository, versionId, objectVersion, transactionalStorage, digestCalculator, readonly, fileTracker,
            directoryTracker);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs a direct file-to-file copy on the local filesystem.
     * If the source file has not been modified in the current transaction, it is copied directly from its
     * location within the OCFL storage root to the transactional staging area.
     */
    @Override
    public void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCRVersionedPath lockedSource = lockVersion(source);
        MCRVersionedPath lockedTarget = lockVersion(target);
        checkPurged(lockedSource);
        checkReadOnly();
        if (this.transactionalStorage.exists(lockedSource)) {
            this.transactionalStorage.copy(lockedSource, lockedTarget, options);
        } else {
            Path localSourcePath = toPhysicalOcflPath(lockedSource);
            try (InputStream inputStream = Files.newInputStream(localSourcePath)) {
                this.transactionalStorage.copy(inputStream, lockedTarget, options);
            }
        }
        trackFileWrite(lockedTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void externalCopy(MCROCFLVirtualObject virtualTarget, MCRVersionedPath source, MCRVersionedPath target,
        CopyOption... options) throws IOException {
        MCRVersionedPath lockedSource = lockVersion(source);
        checkPurged(lockedSource);
        virtualTarget.checkReadOnly();
        if (this.transactionalStorage.exists(lockedSource)) {
            this.transactionalStorage.copy(lockedSource, target, options);
        } else {
            Path localOcflPath = toPhysicalOcflPath(lockedSource);
            try (InputStream is = Files.newInputStream(localOcflPath)) {
                this.transactionalStorage.copy(is, target, options);
            }
        }
        virtualTarget.trackFileWrite(target);
    }

    /**
     * Provides a {@link SeekableByteChannel} for reading a file.
     * <p>
     * If the file is modified in the current transaction, the channel reads from the copy in the transactional
     * storage. Otherwise, it efficiently opens a channel directly to the file in its persistent location
     * within the OCFL repository's storage root.
     *
     * @param path    the path to the file.
     * @param options the options specifying how the file is opened.
     * @return a new seekable byte channel.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected SeekableByteChannel readByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options)
        throws IOException {
        if (this.transactionalStorage.exists(path)) {
            return this.transactionalStorage.newByteChannel(path, options);
        }
        return Files.newByteChannel(toPhysicalOcflPath(path), options);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For local repositories, this implementation retrieves the last access time directly from the
     * underlying filesystem attributes of the file, whether it is in the OCFL repository or
     * the transactional staging area.
     */
    @Override
    public FileTime getAccessTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.transactionalStorage.exists(lockedPath)) {
            return this.transactionalStorage.readAttributes(lockedPath, BasicFileAttributes.class).lastAccessTime();
        }
        Path physicalPath = toPhysicalOcflPath(lockedPath);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).lastAccessTime();
    }

    /**
     * {@inheritDoc}
     * For local repositories, this returns the file key (often the inode) from the underlying filesystem,
     * which can be used for efficient file comparison.
     */
    @Override
    public Object getFileKey(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.transactionalStorage.exists(lockedPath)) {
            return this.transactionalStorage.readAttributes(lockedPath, BasicFileAttributes.class).fileKey();
        }
        Path physicalPath = toPhysicalOcflPath(lockedPath);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).fileKey();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation resolves the virtual path to its actual physical path on the local disk.
     * It will point to the file in the transactional storage if it has been modified, otherwise it will point
     * directly to the file inside the OCFL storage root.
     */
    @Override
    public Path toPhysicalPath(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        if (this.transactionalStorage.exists(lockedPath)) {
            return this.transactionalStorage.toPhysicalPath(lockedPath);
        }
        return toPhysicalOcflPath(path);
    }

    private Path toPhysicalOcflPath(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        String storageRelativePath = changeHistory.getMostRecent().getStorageRelativePath();
        return getLocalRepositoryPath().resolve(storageRelativePath);
    }

    /**
     * Returns the local OCFL repository path.
     *
     * @return the local repository path.
     */
    protected Path getLocalRepositoryPath() {
        return Path.of(MCRConfiguration2
            .getString("MCR.OCFL.Repository." + repository.getId() + ".RepositoryRoot")
            .orElseThrow());
    }

    /**
     * Creates a deep clone of this local virtual object. The clone will share the same storage
     * instances but will have its own independent tracking state.
     *
     * @param readonly whether the cloned object should be read-only.
     * @return a newly generated local virtual object.
     */
    @Override
    public MCROCFLLocalVirtualObject deepClone(boolean readonly) {
        MCROCFLLocalVirtualObject clonedVirtualObject = new MCROCFLLocalVirtualObject(
            this.repository,
            this.objectVersionId,
            this.objectVersion,
            this.transactionalStorage,
            this.digestCalculator,
            readonly,
            this.fileTracker.deepClone(),
            this.directoryTracker.deepClone());
        clonedVirtualObject.fileTracker.setDigestCalculator(new DigestCalculator(clonedVirtualObject));
        return clonedVirtualObject;
    }

}
