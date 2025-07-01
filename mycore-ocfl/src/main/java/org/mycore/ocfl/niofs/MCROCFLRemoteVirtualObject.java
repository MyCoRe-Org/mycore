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
import java.net.URLConnection;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Set;

import io.ocfl.api.io.FixityCheckInputStream;
import io.ocfl.api.model.SizeDigestAlgorithm;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCRPathEventHelper;
import org.mycore.datamodel.niofs.MCRReadOnlyIOException;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.channels.MCROCFLCachingSeekableByteChannel;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;
import org.mycore.ocfl.niofs.channels.MCROCFLReadableByteChannel;
import org.mycore.ocfl.niofs.storage.MCROCFLFileStorage;
import org.mycore.ocfl.niofs.storage.MCROCFLRollingCacheStorage;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalFileStorage;
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

    private final MCROCFLRollingCacheStorage tempStorage;

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersionId the versioned ID of the object.
     * @param transactionalStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLRemoteVirtualObject(MCROCFLRepository repository, ObjectVersionId objectVersionId,
        MCROCFLTransactionalFileStorage transactionalStorage, MCROCFLRollingCacheStorage tempStorage,
        boolean readonly) {
        super(repository, objectVersionId, transactionalStorage, readonly);
        this.tempStorage = tempStorage;
    }

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersion the OCFL object version.
     * @param transactionalStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLRemoteVirtualObject(MCROCFLRepository repository, OcflObjectVersion objectVersion,
        MCROCFLTransactionalFileStorage transactionalStorage, MCROCFLRollingCacheStorage tempStorage,
        boolean readonly) {
        super(repository, objectVersion, transactionalStorage, readonly);
        this.tempStorage = tempStorage;
    }

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param versionId the versioned ID of the object.
     * @param objectVersion the OCFL object version.
     * @param transactionalStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     * @param fileTracker the file tracker.
     * @param directoryTracker the directory tracker.
     */
    protected MCROCFLRemoteVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTransactionalFileStorage transactionalStorage,
        MCROCFLRollingCacheStorage tempStorage, boolean readonly,
        MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLDirectoryTracker directoryTracker) {
        super(repository, versionId, objectVersion, transactionalStorage, readonly, fileTracker, directoryTracker);
        this.tempStorage = tempStorage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(MCRVersionedPath directoryPath) throws IOException {
        MCRVersionedPath lockedDirectory = lockVersion(directoryPath);
        checkPurged(lockedDirectory);
        checkReadOnly();
        if (exists(lockedDirectory)) {
            throw new FileAlreadyExistsException(lockedDirectory.toString());
        }
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.createDirectories(directoryPath);
        }
        this.tempStorage.createDirectories(directoryPath);
        this.directoryTracker.update(lockedDirectory, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocal(MCRVersionedPath path) throws NoSuchFileException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        return !this.markForPurge && getStorage(path).exists(path);
    }

    @Override
    protected MCROCFLClosableCallbackChannel createByteChannel(MCRVersionedPath path,
        Set<? extends OpenOption> options, FileAttribute<?>[] fileAttributes, boolean createNew)
        throws IOException {
        boolean isKeepFile = path.getFileName().toString().equals(KEEP_FILE);
        boolean fireCreateEvent = createNew || Files.notExists(path);
        SeekableByteChannel seekableByteChannel = getByteChannel(path, options, fileAttributes);
        return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
            if (!isKeepFile) {
                trackFileWrite(path, fireCreateEvent ? MCREvent.EventType.CREATE : MCREvent.EventType.UPDATE);
            } else {
                trackEmptyDirectory(path.getParent());
            }
        });
    }

    @Override
    protected SeekableByteChannel readByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        // read from local storage
        MCROCFLFileStorage storage = getStorage(path);
        if (storage.exists(path)) {
            return getByteChannel(path, options, fileAttributes);
        }
        // read from remote ocfl repository
        OcflObjectVersionFile ocflFile = fromOcfl(path);
        createDirectories(path.getParent());
        SeekableByteChannel cachingChannel =
            getByteChannel(path, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                fileAttributes);
        MCROCFLReadableByteChannel readableByteChannel = new MCROCFLReadableByteChannel(ocflFile);
        MCROCFLCachingSeekableByteChannel cachingByteChannel =
            new MCROCFLCachingSeekableByteChannel(readableByteChannel, cachingChannel);
        return new MCROCFLClosableCallbackChannel(cachingByteChannel, () -> {
            // delete partial files from cache
            if (!cachingByteChannel.isFileComplete()) {
                storage.deleteIfExists(path);
            }
        });
    }

    @Override
    protected SeekableByteChannel writeByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        // serve from local storage if exist
        MCROCFLFileStorage storage = getStorage(path);
        if (storage.exists(path)) {
            SeekableByteChannel seekableByteChannel =
                getByteChannel(path, options, fileAttributes);
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
            seekableByteChannel = getByteChannel(path, truncateOptions, fileAttributes);
        } else {
            localCopy(path);
            seekableByteChannel = getByteChannel(path, options, fileAttributes);
        }
        return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
            trackFileWrite(path, MCREvent.EventType.UPDATE);
        });
    }

    public SeekableByteChannel getByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        boolean existsInTransactionalStore = this.transactionalStorage.exists(path);
        // always serve from transactional store if it exists
        if (existsInTransactionalStore) {
            return this.transactionalStorage.newByteChannel(path, options, fileAttributes);
        }
        // it does not exist in transactional store, so we check for read access, if read then we can safely
        // return the rolling store
        boolean read = options.isEmpty() || options.contains(StandardOpenOption.READ);
        if (read) {
            return this.tempStorage.newByteChannel(path, options, fileAttributes);
        }
        // we are writing to the file and it does not exists in transactional store
        boolean existsInRollingStore = this.tempStorage.exists(path);
        boolean transactionActive = MCROCFLFileSystemTransaction.isActive();
        Set<OpenOption> transactionalOptions = new HashSet<>(options);
        if (existsInRollingStore && transactionActive) {
            Path transactionalStoragePhysicalPath = this.transactionalStorage.toPhysicalPath(path);
            Files.createDirectories(transactionalStoragePhysicalPath.getParent());
            boolean append = options.contains(StandardOpenOption.APPEND);
            boolean truncate = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
            if (append) {
                // we are appending -> need to do a copy first
                Path rollingStoragePhysicalPath = this.tempStorage.toPhysicalPath(path);
                Files.copy(rollingStoragePhysicalPath, transactionalStoragePhysicalPath);
            } else if (truncate) {
                // we are truncating, but the file does only exist in rolling store -> no truncate required
                transactionalOptions.remove(StandardOpenOption.TRUNCATE_EXISTING);
                transactionalOptions.add(StandardOpenOption.CREATE);
            }
        }
        return transactionActive ? this.transactionalStorage.newByteChannel(path, transactionalOptions, fileAttributes)
            : this.tempStorage.newByteChannel(path, options, fileAttributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void localCopy(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        if (getStorage(path).exists(lockedPath)) {
            return;
        }
        if (!isFile(lockedPath)) {
            createDirectories(lockedPath);
            return;
        }
        OcflObjectVersionFile ocflFile = fromOcfl(lockedPath);
        try (FixityCheckInputStream stream = ocflFile.getStream()) {
            copy(stream, lockedPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkPurged(lockedPath);
        checkReadOnly();
        if (!exists(lockedPath)) {
            throw new NoSuchFileException(lockedPath.toString());
        }
        if (isDirectory(lockedPath)) {
            if (!isDirectoryEmpty(lockedPath)) {
                throw new DirectoryNotEmptyException(lockedPath.toString());
            }
            deleteIfExists(lockedPath);
            this.directoryTracker.remove(lockedPath);
        } else {
            deleteIfExists(lockedPath);
            this.fileTracker.delete(lockedPath);
        }
        trackEmptyDirectory(lockedPath.getParent());
        MCRPathEventHelper.fireFileDeleteEvent(releaseVersion(lockedPath));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime getCreationTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        boolean added = this.isAdded(lockedPath);
        boolean inLocalStorage = this.isLocal(lockedPath);
        if (added && inLocalStorage) {
            return getStorage(lockedPath).readAttributes(lockedPath, BasicFileAttributes.class).creationTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        return FileTime.from(changeHistory.getOldest().getTimestamp().toInstant());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime getModifiedTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        MCROCFLFileStorage storage = getStorage(lockedPath);
        if (storage.exists(lockedPath)) {
            return storage.readAttributes(lockedPath, BasicFileAttributes.class).lastModifiedTime();
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
        MCROCFLFileStorage storage = getStorage(lockedPath);
        if (storage.exists(lockedPath)) {
            return storage.readAttributes(lockedPath, BasicFileAttributes.class).lastAccessTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        return FileTime.from(changeHistory.getMostRecent().getTimestamp().toInstant());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (isDirectory(lockedPath)) {
            return 0;
        }
        MCROCFLFileStorage storage = getStorage(lockedPath);
        if (storage.exists(lockedPath)) {
            return storage.size(lockedPath);
        }
        OcflObjectVersionFile ocflObjectVersionFile = fromOcfl(lockedPath);
        String sizeAsString = ocflObjectVersionFile.getFixity().get(new SizeDigestAlgorithm());
        return Long.parseLong(sizeAsString);
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
        doCopy(lockedSource, lockedTarget, options);
        trackFileWrite(lockedTarget, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    public void copy(InputStream stream, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.copy(stream, target, options);
        } else {
            this.tempStorage.copy(stream, target, options);
        }
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
        doCopy(lockedSource, target, options);
        virtualTarget.trackFileWrite(target, targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCRVersionedPath lockedSource = lockVersion(source);
        MCRVersionedPath lockedTarget = lockVersion(target);
        checkPurged(lockedSource);
        checkReadOnly();
        if (getStorage(lockedSource).exists(lockedSource)) {
            move(lockedSource, lockedTarget, options);
        } else if (getStorage(lockedTarget).exists(lockedTarget)) {
            deleteIfExists(lockedTarget);
        }
        if (this.isDirectory(lockedSource)) {
            this.renameDirectory(lockedSource, lockedTarget);
        } else {
            this.renameFile(source, lockedTarget);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path toPhysicalPath(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        // checkExists(lockedPath);
        try {
            return getStorage(lockedPath).toPhysicalPath(lockedPath);
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
        MCROCFLFileStorage storage = getStorage(lockedPath);
        if (storage.exists(lockedPath)) {
            return Files.probeContentType(toPhysicalPath(lockedPath));
        }
        return URLConnection.guessContentTypeFromName(lockedPath.getFileName().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() throws IOException {
        if (this.readonly) {
            throw new MCRReadOnlyIOException("Cannot create read-only object: " + this);
        }
        boolean hasFiles = !this.fileTracker.paths().isEmpty();
        boolean hasDirectories = !this.directoryTracker.paths().isEmpty();
        if (hasFiles || hasDirectories) {
            throw new FileAlreadyExistsException("Cannot create already existing object: " + this);
        }
        this.markForPurge = false;
        this.markForCreate = true;
        MCRVersionedPath rootDirectory = this.toMCRPath("/");
        createDirectories(rootDirectory);
        this.directoryTracker.update(rootDirectory, true);
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
            this.transactionalStorage,
            this.tempStorage,
            readonly,
            this.fileTracker.deepClone(),
            this.directoryTracker.deepClone());
        clonedVirtualObject.fileTracker.setDigestCalculator(clonedVirtualObject::calculateDigest);
        return clonedVirtualObject;
    }

    protected void doCopy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            if (this.transactionalStorage.exists(source)) {
                this.transactionalStorage.copy(source, target, options);
            } else {
                Path physicalSource = this.tempStorage.toPhysicalPath(source);
                try (InputStream inputStream = Files.newInputStream(physicalSource)) {
                    this.transactionalStorage.copy(inputStream, target, options);
                }
            }
            return;
        }
        this.tempStorage.copy(source, target, options);
    }

    protected void deleteIfExists(MCRVersionedPath path) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.deleteIfExists(path);
        }
        this.tempStorage.deleteIfExists(path);
    }

    protected void createDirectories(MCRVersionedPath directoryPath, FileAttribute<?>... attrs) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.createDirectories(directoryPath, attrs);
        }
        this.tempStorage.createDirectories(directoryPath, attrs);
    }

    protected void move(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            if (this.transactionalStorage.exists(source)) {
                this.transactionalStorage.move(source, target, options);
            } else {
                Path physicalSource = this.tempStorage.toPhysicalPath(source);
                try (InputStream inputStream = Files.newInputStream(physicalSource)) {
                    this.transactionalStorage.copy(inputStream, target, options);
                }
            }
            return;
        }
        this.tempStorage.move(source, target, options);
    }

    protected MCROCFLFileStorage getStorage(MCRVersionedPath path) {
        boolean activeTransaction = MCROCFLFileSystemTransaction.isActive();
        if (!activeTransaction) {
            return this.tempStorage;
        }
        boolean doesExistInTransaction = this.transactionalStorage.exists(path);
        if (doesExistInTransaction) {
            return this.transactionalStorage;
        }
        return this.tempStorage;
    }

}
