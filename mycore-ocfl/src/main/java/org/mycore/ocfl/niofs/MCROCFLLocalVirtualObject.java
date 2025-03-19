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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Set;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;
import org.mycore.ocfl.niofs.storage.MCROCFLTempFileStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;

import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

/**
 * Represents a virtual object that is stored on the same drive as the OCFL repository. This provides the implementation
 * direct access to the files of the OCFL repository when needed. For example a file can be accessed directly for read
 * operations without copying it first to the local storage.
 * <p>
 * This class extends {@link MCROCFLVirtualObject} and provides implementations specific to local storage.
 * It handles file operations such as copying, moving, and deleting files within the local file system,
 * and ensures consistency with the OCFL repository. This class also manages local modifications and
 * synchronizes changes with the OCFL repository.
 */
public class MCROCFLLocalVirtualObject extends MCROCFLVirtualObject {

    /**
     * Constructs a new {@code MCROCFLLocalVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersionId the versioned ID of the object.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLLocalVirtualObject(MCROCFLRepository repository, ObjectVersionId objectVersionId,
        MCROCFLTempFileStorage localStorage, boolean readonly) {
        super(repository, objectVersionId, localStorage, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLLocalVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersion the OCFL object version.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLLocalVirtualObject(MCROCFLRepository repository, OcflObjectVersion objectVersion,
        MCROCFLTempFileStorage localStorage, boolean readonly) {
        super(repository, objectVersion, localStorage, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLLocalVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param versionId the versioned ID of the object.
     * @param objectVersion the OCFL object version.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     * @param fileTracker the file tracker.
     * @param directoryTracker the directory tracker.
     */
    protected MCROCFLLocalVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTempFileStorage localStorage,
        boolean readonly,
        MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLDirectoryTracker directoryTracker) {
        super(repository, versionId, objectVersion, localStorage, readonly, fileTracker, directoryTracker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCRVersionedPath lockedSource = lockVersion(source);
        MCRVersionedPath lockedTarget = lockVersion(target);
        checkPurged(lockedSource);
        checkReadOnly();
        if (this.localStorage.exists(lockedSource)) {
            this.localStorage.copy(lockedSource, lockedTarget, options);
        } else {
            Path localSourcePath = toPhysicalOcflPath(lockedSource);
            try (InputStream inputStream = Files.newInputStream(localSourcePath)) {
                this.localStorage.copy(inputStream, lockedTarget, options);
            }
        }
        boolean targetExists = exists(lockedTarget);
        trackFileWrite(lockedTarget, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
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
        if (this.localStorage.exists(lockedSource)) {
            this.localStorage.copy(lockedSource, target, options);
        } else {
            Path localOcflPath = toPhysicalOcflPath(lockedSource);
            try (InputStream is = Files.newInputStream(localOcflPath)) {
                this.localStorage.copy(is, target, options);
            }
        }
        boolean targetExists = virtualTarget.exists(target);
        virtualTarget.trackFileWrite(target, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    @Override
    protected SeekableByteChannel readByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        if (this.localStorage.exists(path)) {
            return this.localStorage.newByteChannel(path, options, fileAttributes);
        }
        return Files.newByteChannel(toPhysicalOcflPath(path), options, fileAttributes);
    }

    @Override
    protected SeekableByteChannel writeByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        Set<OpenOption> writeOptions = new HashSet<>(options);
        if (options.contains(StandardOpenOption.APPEND)) {
            // only need local copy if we want to append
            localCopy(path);
        } else {
            // need to add CREATE if it exists in virtual object but not in local storage
            boolean existsInVirtualObject = exists(path);
            boolean existsInLocalStorage = this.localStorage.exists(path);
            if (existsInVirtualObject && !existsInLocalStorage) {
                writeOptions.add(StandardOpenOption.CREATE);
            }
        }
        SeekableByteChannel seekableByteChannel
            = this.localStorage.newByteChannel(path, writeOptions, fileAttributes);
        return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
            trackFileWrite(path, MCREvent.EventType.UPDATE);
        });
    }

    @Override
    public FileTime getModifiedTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.localStorage.exists(lockedPath)) {
            return this.localStorage.readAttributes(lockedPath, BasicFileAttributes.class).lastModifiedTime();
        }
        Path physicalPath = toPhysicalOcflPath(lockedPath);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).lastModifiedTime();
    }

    @Override
    public FileTime getAccessTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.localStorage.exists(lockedPath)) {
            return this.localStorage.readAttributes(lockedPath, BasicFileAttributes.class).lastAccessTime();
        }
        Path physicalPath = toPhysicalOcflPath(lockedPath);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).lastAccessTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getFileKey(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.localStorage.exists(lockedPath)) {
            return this.localStorage.readAttributes(lockedPath, BasicFileAttributes.class).fileKey();
        }
        Path physicalPath = toPhysicalOcflPath(lockedPath);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).fileKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toPhysicalPath(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.localStorage.exists(lockedPath)) {
            return this.localStorage.toPhysicalPath(lockedPath);
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
     * Creates a deep clone of this local virtual object.
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
            this.localStorage,
            readonly,
            this.fileTracker.deepClone(),
            this.directoryTracker.deepClone());
        clonedVirtualObject.fileTracker.setDigestCalculator(clonedVirtualObject::calculateDigest);
        return clonedVirtualObject;
    }

}
