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

import org.mycore.common.digest.MCRDigest;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.storage.MCROCFLTempFileStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

/**
 * Represents a virtual object stored locally in an OCFL repository.
 * <p>
 * This class extends {@link MCROCFLVirtualObject} and provides implementations specific to local storage.
 * It handles file operations such as copying, moving, and deleting files within the local file system,
 * and ensures consistency with the OCFL repository. This class also manages local modifications and
 * synchronizes changes with the OCFL repository.
 * </p>
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
        OcflObjectVersion objectVersion, MCROCFLTempFileStorage localStorage, boolean readonly,
        MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLEmptyDirectoryTracker directoryTracker) {
        super(repository, versionId, objectVersion, localStorage, readonly, fileTracker, directoryTracker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFile(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        checkPurged(source);
        checkReadOnly();
        boolean targetExists = exists(target);
        if (this.localStorage.exists(source)) {
            this.localStorage.copy(source, target, options);
        } else {
            Path localSourcePath = toPhysicalPath(source);
            try (InputStream inputStream = Files.newInputStream(localSourcePath)) {
                this.localStorage.copy(inputStream, target, options);
            }
        }
        trackFileWrite(target, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFileToVirtualObject(MCROCFLVirtualObject virtualTarget, MCRVersionedPath source,
        MCRVersionedPath target, CopyOption... options) throws IOException {
        checkPurged(source);
        virtualTarget.checkReadOnly();
        boolean targetExists = virtualTarget.exists(target);
        Path localSourcePath = toPhysicalPath(source);
        try (InputStream is = Files.newInputStream(localSourcePath)) {
            this.localStorage.copy(is, target, options);
        }
        virtualTarget.trackFileWrite(target, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    /**
     * Opens or creates a byte channel to a file.
     *
     * @param path the path to the file.
     * @param options the options specifying how the file is opened.
     * @param fileAttributes the file attributes to set atomically when creating the file.
     * @return a new seekable byte channel.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected SeekableByteChannel readOrWriteByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        // write
        boolean write = options.contains(StandardOpenOption.WRITE);
        if (write) {
            Set<OpenOption> writeOptions = new HashSet<>(options);
            if (options.contains(StandardOpenOption.APPEND)) {
                // only need local copy if we want to append
                localCopy(path);
            } else {
                // need to add CREATE if it exists in virtual object but not in local storage
                boolean hasCreateOption = options.contains(StandardOpenOption.CREATE);
                boolean existsInVirtualObject = exists(path);
                boolean existsInLocalStorage = this.localStorage.exists(path);
                if (!hasCreateOption && existsInVirtualObject && !existsInLocalStorage) {
                    writeOptions.add(StandardOpenOption.CREATE);
                }
            }
            SeekableByteChannel seekableByteChannel
                = this.localStorage.newByteChannel(path, writeOptions, fileAttributes);
            return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
                trackFileWrite(path, MCREvent.EventType.UPDATE);
            });
        }
        // read
        return this.localStorage.exists(path) ? this.localStorage.newByteChannel(path, options, fileAttributes)
            : Files.newByteChannel(toPhysicalPath(path), options, fileAttributes);
    }

    @Override
    public FileTime getModifiedTime(MCRVersionedPath path) throws IOException {
        checkPurged(path);
        checkExists(path);
        Path physicalPath = toPhysicalPath(path);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).lastModifiedTime();
    }

    @Override
    public FileTime getAccessTime(MCRVersionedPath path) throws IOException {
        checkPurged(path);
        checkExists(path);
        Path physicalPath = toPhysicalPath(path);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).lastAccessTime();
    }

    @Override
    public long getSize(MCRVersionedPath path) throws IOException {
        checkPurged(path);
        checkExists(path);
        if (isDirectory(path)) {
            return 0;
        }
        Path physicalPath = toPhysicalPath(path);
        return Files.size(physicalPath);
    }

    @Override
    public Object getFileKey(MCRVersionedPath path) throws IOException {
        checkPurged(path);
        checkExists(path);
        // TODO the fileKey between the localstorage and the ocfl repository should always be the same
        // this implementation is just a hack for testing
        Path physicalPath = toPhysicalPath(path);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).fileKey();
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
            this.emptyDirectoryTracker.deepClone());
        clonedVirtualObject.fileTracker.setDigestCalculator(clonedVirtualObject::calculateDigest);
        return clonedVirtualObject;
    }

}
