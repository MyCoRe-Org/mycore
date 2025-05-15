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
import java.net.URLConnection;
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
import org.mycore.ocfl.niofs.channels.MCROCFLCachingSeekableByteChannel;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;
import org.mycore.ocfl.niofs.channels.MCROCFLReadableByteChannel;
import org.mycore.ocfl.niofs.storage.MCROCFLTempFileStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;

import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.OcflObjectVersionFile;

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
        MCROCFLDirectoryTracker directoryTracker) {
        super(repository, versionId, objectVersion, localStorage, readonly, fileTracker, directoryTracker);
    }

    @Override
    protected SeekableByteChannel readByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        // read from local storage
        if (this.localStorage.exists(path)) {
            return this.localStorage.newByteChannel(path, options, fileAttributes);
        }
        // read from remote ocfl repository
        OcflObjectVersionFile ocflFile = fromOcfl(path);
        this.localStorage.createDirectories(path.getParent());
        SeekableByteChannel cachingChannel =
            this.localStorage.newByteChannel(path, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                fileAttributes);
        MCROCFLReadableByteChannel readableByteChannel = new MCROCFLReadableByteChannel(ocflFile);
        MCROCFLCachingSeekableByteChannel cachingByteChannel =
            new MCROCFLCachingSeekableByteChannel(readableByteChannel, cachingChannel);
        return new MCROCFLClosableCallbackChannel(cachingByteChannel, () -> {
            // delete partial files from cache
            if (!cachingByteChannel.isFileComplete()) {
                this.localStorage.deleteIfExists(path);
            }
        });
    }

    @Override
    protected SeekableByteChannel writeByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        // serve from local storage if exist
        if (this.localStorage.exists(path)) {
            SeekableByteChannel seekableByteChannel = this.localStorage.newByteChannel(path, options, fileAttributes);
            return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
                trackFileWrite(path, MCREvent.EventType.UPDATE);
            });
        }
        // serve from remote
        boolean truncateExisting = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
        SeekableByteChannel seekableByteChannel;
        if (truncateExisting) {
            // if the file is truncated anyway, we don't need to make a local copy
            Set<OpenOption> truncateOptions = new HashSet<>(options);
            truncateOptions.add(StandardOpenOption.CREATE);
            seekableByteChannel = this.localStorage.newByteChannel(path, truncateOptions, fileAttributes);
        } else {
            localCopy(path);
            seekableByteChannel = this.localStorage.newByteChannel(path, options, fileAttributes);
        }
        return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
            trackFileWrite(path, MCREvent.EventType.UPDATE);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime getModifiedTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.localStorage.exists(lockedPath)) {
            return this.localStorage.readAttributes(lockedPath, BasicFileAttributes.class).lastModifiedTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        return FileTime.from(changeHistory.getMostRecent().getTimestamp().toInstant());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime getAccessTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.localStorage.exists(lockedPath)) {
            return this.localStorage.readAttributes(lockedPath, BasicFileAttributes.class).lastAccessTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        return FileTime.from(changeHistory.getMostRecent().getTimestamp().toInstant());
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
        boolean targetExists = exists(lockedTarget);
        localCopy(lockedSource);
        this.localStorage.copy(lockedSource, lockedTarget, options);
        trackFileWrite(lockedTarget, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void externalCopy(MCROCFLVirtualObject virtualTarget, MCRVersionedPath source,
        MCRVersionedPath target, CopyOption... options) throws IOException {
        MCRVersionedPath lockedSource = lockVersion(source);
        checkPurged(lockedSource);
        virtualTarget.checkReadOnly();
        boolean targetExists = virtualTarget.exists(target);
        localCopy(lockedSource);
        localStorage.copy(lockedSource, target, options);
        virtualTarget.trackFileWrite(target, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path toPhysicalPath(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        try {
            return this.localStorage.toPhysicalPath(lockedPath);
        } catch (Exception e) {
            throw new CannotDeterminePhysicalPathException("Unable to create physical path for " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getFileKey(MCRVersionedPath path) {
        return null;
    }

    /**
     * Probes the content type of a file.
     * <p>
     * If the file exists in the local storage the content is probed from the underlying file system. If the file only
     * exists remote, we use {@link URLConnection#guessContentTypeFromName(String)} to avoid downloading the file.
     *
     * @param path versioned path
     * @return the mime type
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public String probeContentType(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.localStorage.exists(lockedPath)) {
            return Files.probeContentType(toPhysicalPath(lockedPath));
        }
        return URLConnection.guessContentTypeFromName(lockedPath.getFileName().toString());
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
            this.directoryTracker.deepClone());
        clonedVirtualObject.fileTracker.setDigestCalculator(clonedVirtualObject::calculateDigest);
        return clonedVirtualObject;
    }

}
