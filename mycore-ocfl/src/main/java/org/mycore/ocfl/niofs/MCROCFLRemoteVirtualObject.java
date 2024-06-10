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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Set;

import org.mycore.common.digest.MCRDigest;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.storage.MCROCFLTempFileStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;

import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

/**
 * Represents a virtual object stored remotely in an OCFL repository.
 * <p>
 * This class extends {@link MCROCFLVirtualObject} and provides implementations specific to remote storage.
 * It handles file operations such as copying, moving, and deleting files while ensuring consistency with the OCFL
 * repository.
 * This class also manages local modifications and synchronizes changes with the OCFL repository.
 * </p>
 */
public class MCROCFLRemoteVirtualObject extends MCROCFLVirtualObject {

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersionId the versioned ID of the object.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLRemoteVirtualObject(MCROCFLRepository repository, ObjectVersionId objectVersionId,
        MCROCFLTempFileStorage localStorage, boolean readonly) {
        super(repository, objectVersionId, localStorage, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersion the OCFL object version.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLRemoteVirtualObject(MCROCFLRepository repository, OcflObjectVersion objectVersion,
        MCROCFLTempFileStorage localStorage, boolean readonly) {
        super(repository, objectVersion, localStorage, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param versionId the versioned ID of the object.
     * @param objectVersion the OCFL object version.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     * @param fileTracker the file tracker.
     * @param directoryTracker the directory tracker.
     */
    protected MCROCFLRemoteVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTempFileStorage localStorage, boolean readonly,
        MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLEmptyDirectoryTracker directoryTracker) {
        super(repository, versionId, objectVersion, localStorage, readonly, fileTracker, directoryTracker);
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
        boolean write = options.contains(StandardOpenOption.WRITE);
        localCopy(path);
        SeekableByteChannel seekableByteChannel = this.localStorage.newByteChannel(path, options, fileAttributes);
        if (write) {
            return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
                trackFileWrite(path, MCREvent.EventType.UPDATE);
            });
        }
        return seekableByteChannel;
    }

    @Override
    public FileTime getModifiedTime(MCRVersionedPath path) throws IOException {
        checkExists(path);
        if (this.localStorage.exists(path)) {
            Path physicalPath = this.localStorage.toPhysicalPath(path);
            return Files.readAttributes(physicalPath, BasicFileAttributes.class).lastModifiedTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(path);
        return FileTime.from(changeHistory.getMostRecent().getTimestamp().toInstant());
    }

    @Override
    public FileTime getAccessTime(MCRVersionedPath path) throws IOException {
        checkExists(path);
        if (this.localStorage.exists(path)) {
            Path physicalPath = this.localStorage.toPhysicalPath(path);
            return Files.readAttributes(physicalPath, BasicFileAttributes.class).lastAccessTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(path);
        return FileTime.from(changeHistory.getMostRecent().getTimestamp().toInstant());
    }

    @Override
    public long getSize(MCRVersionedPath path) throws IOException {
        checkExists(path);
        if (isDirectory(path)) {
            return 0;
        }
        // TODO right now we just copy and deliver from local storage
        // in future versions we should use the database to get the size
        localCopy(path);
        //if(this.localStorage.exists(path)) {
        Path physicalPath = this.localStorage.toPhysicalPath(path);
        return Files.size(physicalPath);
        //}
    }

    @Override
    public Object getFileKey(MCRVersionedPath path) throws IOException {
        checkExists(path);
        // TODO rm this
        localCopy(path);
        // TODO the fileKey between the localstorage and the ocfl repository should always be the same
        // this implementation is just a hack for testing
        Path physicalPath = toPhysicalPath(path);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).fileKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFile(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        checkPurged(source);
        checkReadOnly();
        boolean targetExists = exists(target);
        localCopy(source);
        this.localStorage.copy(source, target, options);
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
        localCopy(source);
        localStorage.copy(source, target, options);
        virtualTarget.trackFileWrite(target, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    /**
     * {@inheritDoc}
     * This implementation always creates a copy before returning the physical path, guaranteeing
     * that the file exists in the local temporary storage.
     */
    @Override
    public Path toPhysicalPath(MCRVersionedPath path) throws IOException {
        checkExists(path);
        localCopy(path);
        return super.toPhysicalPath(path);
    }

    /**
     * Creates a deep clone of this remote virtual object.
     *
     * @param readonly whether the cloned object should be read-only.
     * @return a newly generated remote virtual object.
     */
    @Override
    public MCROCFLRemoteVirtualObject deepClone(boolean readonly) {
        MCROCFLRemoteVirtualObject clonedVirtualObject = new MCROCFLRemoteVirtualObject(
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
