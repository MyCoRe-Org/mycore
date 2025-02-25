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

import static org.mycore.ocfl.util.MCROCFLVersionHelper.MESSAGE_CREATED;
import static org.mycore.ocfl.util.MCROCFLVersionHelper.MESSAGE_UPDATED;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRUtils;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.digest.MCRSHA512Digest;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCRPathEventHelper;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRReadOnlyIOException;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;
import org.mycore.ocfl.niofs.storage.MCROCFLTempFileStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

import io.ocfl.api.DigestAlgorithmRegistry;
import io.ocfl.api.OcflObjectUpdater;
import io.ocfl.api.OcflOption;
import io.ocfl.api.io.FixityCheckInputStream;
import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.OcflObjectVersionFile;
import io.ocfl.api.model.SizeDigestAlgorithm;
import io.ocfl.api.model.VersionInfo;
import io.ocfl.api.model.VersionNum;

/**
 * Represents a virtual object in an OCFL repository.
 * <p>
 * This abstract class provides the core functionality for managing OCFL virtual objects within a repository.
 * It handles various file system operations such as creating directories, reading and writing files, renaming,
 * and deleting files, while ensuring consistency between the local storage and the OCFL repository.
 * The class also tracks changes to files and directories, supports local modifications, and facilitates
 * synchronization with the repository.
 * </p>
 */
public abstract class MCROCFLVirtualObject {

    public static final String FILES_DIRECTORY = "files/";

    public static final String KEEP_FILE = ".keep";

    protected final MCROCFLRepository repository;

    protected final ObjectVersionId objectVersionId;

    protected OcflObjectVersion objectVersion;

    protected final MCROCFLTempFileStorage localStorage;

    protected final boolean readonly;

    protected boolean markForCreate;

    protected boolean markForPurge;

    protected MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker;

    protected MCROCFLDirectoryTracker emptyDirectoryTracker;

    protected Map<MCRVersionedPath, FileChangeHistory> changeHistoryCache;

    /**
     * Constructs a new {@code MCROCFLVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersionId the versioned ID of the object.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLVirtualObject(MCROCFLRepository repository, ObjectVersionId objectVersionId,
        MCROCFLTempFileStorage localStorage, boolean readonly) {
        this(repository, objectVersionId, null, localStorage, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param objectVersion the OCFL object version.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    public MCROCFLVirtualObject(MCROCFLRepository repository, OcflObjectVersion objectVersion,
        MCROCFLTempFileStorage localStorage, boolean readonly) {
        this(repository, objectVersion.getObjectVersionId(), objectVersion, localStorage, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param versionId the versioned ID of the object.
     * @param objectVersion the OCFL object version.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     */
    protected MCROCFLVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTempFileStorage localStorage,
        boolean readonly) {
        this(repository, versionId, objectVersion, localStorage, readonly, null, null);
        initTrackers();
    }

    /**
     * Constructs a new {@code MCROCFLVirtualObject}.
     *
     * @param repository the OCFL repository.
     * @param versionId the versioned ID of the object.
     * @param objectVersion the OCFL object version.
     * @param localStorage the local temporary file storage.
     * @param readonly whether the object is read-only.
     * @param fileTracker the file tracker.
     * @param directoryTracker the directory tracker.
     */
    protected MCROCFLVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTempFileStorage localStorage,
        boolean readonly, MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLDirectoryTracker directoryTracker) {
        Objects.requireNonNull(repository);
        Objects.requireNonNull(versionId);
        Objects.requireNonNull(localStorage);
        this.repository = repository;
        this.objectVersionId = versionId;
        this.objectVersion = objectVersion;
        this.localStorage = localStorage;
        this.readonly = readonly;
        this.markForCreate = false;
        this.markForPurge = false;
        this.fileTracker = fileTracker;
        this.emptyDirectoryTracker = directoryTracker;
        this.changeHistoryCache = new HashMap<>();
    }

    /**
     * Initializes the file and directory trackers.
     */
    protected void initTrackers() {
        if (this.objectVersion == null) {
            this.fileTracker = new MCROCFLFileTracker<>(new HashMap<>(), this::calculateDigest);
            this.emptyDirectoryTracker = new MCROCFLDirectoryTracker(new HashMap<>());
            return;
        }
        Map<MCRVersionedPath, MCRDigest> filePaths = new HashMap<>();
        Map<MCRVersionedPath, Boolean> directoryPaths = new HashMap<>();
        for (OcflObjectVersionFile file : this.objectVersion.getFiles()) {
            String ocflFilesPath = file.getPath();
            if (!ocflFilesPath.startsWith(FILES_DIRECTORY)) {
                continue;
            }
            String ocflPath = fromOcflFilesPath(ocflFilesPath);
            MCRVersionedPath filePath = toMCRPath(ocflPath);
            boolean hasKeepFile = filePath.getFileName().toString().equals(KEEP_FILE);
            if (!hasKeepFile) {
                String sha512Digest = file.getFixity().get(DigestAlgorithmRegistry.sha512);
                filePaths.put(filePath, new MCRSHA512Digest(sha512Digest));
            }
            directoryPaths.put(filePath.getParent(), hasKeepFile);
        }
        this.fileTracker = new MCROCFLFileTracker<>(filePaths, this::calculateDigest);
        this.emptyDirectoryTracker = new MCROCFLDirectoryTracker(directoryPaths);
    }

    /**
     * Creates a directory at the specified path.
     *
     * @param directoryPath the path of the directory to create.
     * @throws IOException if an I/O error occurs.
     */
    public void createDirectory(MCRVersionedPath directoryPath) throws IOException {
        MCRVersionedPath lockedDirectory = lockVersion(directoryPath);
        checkPurged(lockedDirectory);
        checkReadOnly();
        if (exists(lockedDirectory)) {
            throw new FileAlreadyExistsException(lockedDirectory.toString());
        }
        this.localStorage.createDirectories(lockedDirectory);
        this.emptyDirectoryTracker.update(lockedDirectory, true);
    }

    /**
     * Returns a list of all paths in this virtual object.
     *
     * @return a list of all paths.
     */
    public List<MCRVersionedPath> paths() {
        if (this.markForPurge) {
            return Collections.emptyList();
        }
        return Stream.of(this.emptyDirectoryTracker.paths(), this.fileTracker.paths())
            .flatMap(Collection::stream)
            .toList();
    }

    /**
     * Checks if the specified path exists.
     *
     * @param path the path to check.
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    public boolean exists(MCRVersionedPath path) {
        MCRVersionedPath lockedPath = lockVersion(path);
        if (this.markForPurge) {
            return false;
        }
        return this.fileTracker.exists(lockedPath) || this.emptyDirectoryTracker.exists(lockedPath);
    }

    /**
     * Checks if the specified path is stored locally.
     *
     * @param path the path to check.
     * @return {@code true} if the path is stored locally, {@code false} otherwise.
     * @throws NoSuchFileException if the path does not exist.
     */
    public boolean isLocal(MCRVersionedPath path) throws NoSuchFileException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        return !this.markForPurge && this.localStorage.exists(lockedPath);
    }

    /**
     * Checks if the specified path is a directory.
     *
     * @param path the path to check.
     * @return {@code true} if the path is a directory, {@code false} otherwise.
     * @throws NoSuchFileException if the path does not exist.
     */
    public boolean isDirectory(MCRVersionedPath path) throws NoSuchFileException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        return !this.markForPurge && this.emptyDirectoryTracker.exists(lockedPath);
    }

    /**
     * Checks if the specified directory is empty.
     *
     * @param directoryPath the path to the directory.
     * @return {@code true} if the directory is empty, {@code false} otherwise.
     * @throws NoSuchFileException if the path does not exist.
     */
    public boolean isDirectoryEmpty(MCRVersionedPath directoryPath) throws NoSuchFileException {
        MCRVersionedPath lockedDirectoryPath = lockVersion(directoryPath);
        checkExists(lockedDirectoryPath);
        return this.emptyDirectoryTracker.isEmpty(lockedDirectoryPath);
    }

    /**
     * Checks if the specified path is a file.
     *
     * @param path the path to check.
     * @return {@code true} if the path is a file, {@code false} otherwise.
     */
    public boolean isFile(MCRVersionedPath path) {
        MCRVersionedPath lockedPath = lockVersion(path);
        return !this.markForPurge && this.fileTracker.exists(lockedPath);
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
    public SeekableByteChannel newByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkPurged(lockedPath);
        boolean exists = this.exists(lockedPath);
        boolean create = options.contains(StandardOpenOption.CREATE);
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        boolean read = options.isEmpty() || options.contains(StandardOpenOption.READ);
        // check read
        if (!read) {
            checkReadOnly();
        }
        // create
        if (createNew && exists) {
            throw new FileAlreadyExistsException(lockedPath.toString());
        }
        // check existing
        if (!exists && !(create || createNew)) {
            throw new NoSuchFileException(lockedPath.toString());
        }
        if (create || createNew) {
            return createByteChannel(lockedPath, options, fileAttributes, createNew);
        } else if (read) {
            return readByteChannel(lockedPath, options, fileAttributes);
        } else {
            return writeByteChannel(lockedPath, options, fileAttributes);
        }
    }

    protected MCROCFLClosableCallbackChannel createByteChannel(MCRVersionedPath path,
        Set<? extends OpenOption> options, FileAttribute<?>[] fileAttributes, boolean createNew)
        throws IOException {
        boolean isKeepFile = path.getFileName().toString().equals(KEEP_FILE);
        boolean fireCreateEvent = createNew || Files.notExists(path);
        SeekableByteChannel seekableByteChannel = this.localStorage.newByteChannel(path, options, fileAttributes);
        return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
            if (!isKeepFile) {
                trackFileWrite(path, fireCreateEvent ? MCREvent.EventType.CREATE : MCREvent.EventType.UPDATE);
            } else {
                trackEmptyDirectory(path.getParent());
            }
        });
    }

    protected abstract SeekableByteChannel readByteChannel(MCRVersionedPath path,
        Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException;

    protected abstract SeekableByteChannel writeByteChannel(MCRVersionedPath path,
        Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException;

    /**
     * <p>
     * Copies a file from the OCFL repository to the local storage. This should be called whenever it is necessary to
     * work on a copy rather than the OCFL file directly.
     * </p>
     * <p>
     *     If the requested path is a directory and it does not exist yet, an empty directory is created in the local
     *     storage. This guarantees that the given path is always accessible.
     * </p>
     *
     * @param path the path to the file.
     * @throws IOException if an I/O error occurs.
     */
    public void localCopy(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        if (this.localStorage.exists(lockedPath)) {
            return;
        }
        if (!isFile(lockedPath)) {
            this.localStorage.createDirectories(lockedPath);
            return;
        }
        OcflObjectVersionFile ocflFile = fromOcfl(lockedPath);
        try (FixityCheckInputStream stream = ocflFile.getStream()) {
            this.localStorage.copy(stream, lockedPath);
        }
    }

    /**
     * Returns the OCFL object version file corresponding to the specified path.
     *
     * @param path the path to the file.
     * @return the OCFL object version file.
     * @throws NoSuchFileException if the file does not exist.
     */
    protected OcflObjectVersionFile fromOcfl(MCRVersionedPath path) throws NoSuchFileException {
        if (objectVersion == null) {
            throw new NoSuchFileException("'" + getObjectId() + "' is not yet stored in the repository.");
        }
        MCRVersionedPath ocflOriginalPath = this.fileTracker.findPath(path);
        String ocflFilePath = toOcflFilesPath(ocflOriginalPath.toRelativePath());
        OcflObjectVersionFile file = objectVersion.getFile(ocflFilePath);
        if (file == null) {
            throw new NoSuchFileException(ocflOriginalPath.toString());
        }
        return file;
    }

    protected String toOcflFilesPath(String ocflFile) {
        return FILES_DIRECTORY + ocflFile;
    }

    protected String fromOcflFilesPath(String ocflFilesPath) {
        return ocflFilesPath.substring(FILES_DIRECTORY.length());
    }

    /**
     * Deletes the specified path.
     *
     * @param path the path to delete.
     * @throws IOException if an I/O error occurs.
     */
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
            this.localStorage.deleteIfExists(lockedPath);
            this.emptyDirectoryTracker.remove(lockedPath);
        } else {
            this.localStorage.deleteIfExists(lockedPath);
            this.fileTracker.delete(lockedPath);
        }
        trackEmptyDirectory(lockedPath.getParent());
        MCRPathEventHelper.fireFileDeleteEvent(releaseVersion(lockedPath));
    }

    /**
     * Copies a file from the source path to the target path.
     * <p>This copy operation only works if both the source and
     * the target are within the same {@link MCROCFLVirtualObject}. Use
     * {@link #externalCopy(MCROCFLVirtualObject, MCRVersionedPath, MCRVersionedPath, CopyOption...)} if that is not
     * the case.</p>
     *
     * @param source the source path.
     * @param target the target path.
     * @param options the options specifying how the copy is performed.
     * @throws IOException if an I/O error occurs.
     */
    public abstract void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options)
        throws IOException;

    /**
     * Copies a file from this virtual object to another virtual object.
     *
     * @param virtualTarget the target virtual object.
     * @param source the source path.
     * @param target the target path.
     * @param options the options specifying how the copy is performed.
     * @throws IOException if an I/O error occurs.
     */
    public abstract void externalCopy(MCROCFLVirtualObject virtualTarget, MCRVersionedPath source,
        MCRVersionedPath target, CopyOption... options) throws IOException;

    /**
     * Renames a file or directory from the source path to the target path.
     *
     * @param source the source path.
     * @param target the target path.
     * @param options the options specifying how the rename is performed.
     * @throws IOException if an I/O error occurs.
     */
    public void rename(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCRVersionedPath lockedSource = lockVersion(source);
        MCRVersionedPath lockedTarget = lockVersion(target);
        checkPurged(lockedSource);
        checkReadOnly();
        if (this.localStorage.exists(lockedSource)) {
            this.localStorage.move(lockedSource, lockedTarget, options);
        } else if (this.localStorage.exists(lockedTarget)) {
            this.localStorage.deleteIfExists(lockedTarget);
        }
        if (this.isDirectory(lockedSource)) {
            this.renameDirectory(lockedSource, lockedTarget);
        } else {
            this.renameFile(source, lockedTarget);
        }
    }

    private void renameDirectory(MCRVersionedPath source, MCRVersionedPath target) throws IOException {
        if (!this.emptyDirectoryTracker.isEmpty(source)) {
            throw new DirectoryNotEmptyException(source.toString());
        }
        this.emptyDirectoryTracker.rename(source, target);
    }

    private void renameFile(MCRVersionedPath source, MCRVersionedPath target) throws IOException {
        boolean create = !exists(target);
        this.fileTracker.rename(source, target);
        MCRVersionedPath releasedSourcePath = releaseVersion(source);
        MCRVersionedPath releasedTargetPath = releaseVersion(target);
        MCRPathEventHelper.fireFileMoveEvent(releasedSourcePath, releasedTargetPath, create);
    }

    /**
     * Creates a new directory stream for the specified directory.
     *
     * @param directory the directory to open.
     * @return a new directory stream.
     * @throws IOException if an I/O error occurs.
     */
    public DirectoryStream<Path> newDirectoryStream(MCRVersionedPath directory) throws IOException {
        return newDirectoryStream(directory, path -> true);
    }

    /**
     * Creates a new directory stream for the specified directory.
     *
     * @param directory the directory to open.
     * @param filter the directory stream filter.
     * @return a new directory stream.
     * @throws IOException if an I/O error occurs.
     */
    public DirectoryStream<Path> newDirectoryStream(MCRVersionedPath directory, Filter<? super Path> filter)
        throws IOException {
        MCRVersionedPath lockedDirectory = lockVersion(directory);
        checkPurged(lockedDirectory);
        if (this.fileTracker.exists(lockedDirectory)) {
            throw new NotDirectoryException(lockedDirectory.toString());
        }
        if (!this.emptyDirectoryTracker.exists(lockedDirectory)) {
            throw new NoSuchFileException(lockedDirectory.toString());
        }
        final List<Path> paths = list(lockedDirectory, filter, paths());
        return new DirectoryStream<>() {
            @Override
            public Iterator<Path> iterator() {
                return paths.iterator();
            }

            @Override
            public void close() throws IOException {
                // nothing to close
            }
        };
    }

    /**
     * Returns a list of paths within the specified directory.
     *
     * @param directory the directory whose paths are to be listed.
     * @return a list of paths within the specified directory.
     * @throws IOException if an I/O error occurs while listing the directory.
     */
    public List<Path> list(MCRVersionedPath directory) throws IOException {
        return list(directory, path -> true, paths());
    }

    protected List<Path> list(MCRVersionedPath directory, Collection<MCRVersionedPath> paths) throws IOException {
        return list(directory, path -> true, paths);
    }

    protected List<Path> list(MCRVersionedPath directory, Filter<? super Path> filter,
        Collection<MCRVersionedPath> paths) throws IOException {
        MCRVersionedPath lockedDirectory = lockVersion(directory);
        List<Path> finalPaths = new ArrayList<>();
        // filter paths for directory
        Set<MCRVersionedPath> pathsInDirectory = paths.stream()
            .filter(path -> isPathInDirectory(lockedDirectory, path))
            .collect(Collectors.toSet());
        // use for loop to catch IOException
        for (MCRVersionedPath path : pathsInDirectory) {
            if (filter.accept(path)) {
                finalPaths.add(path);
            }
        }
        return finalPaths;
    }

    protected boolean isPathInDirectory(MCRVersionedPath dir, MCRVersionedPath path) {
        if (path.equals(dir)) {
            return false;
        }
        if (!path.startsWith(dir)) {
            return false;
        }
        MCRPath relativePath = dir.relativize(path);
        return relativePath.getNameCount() == 1;
    }

    /**
     * Returns the digest (hash) of the file at the specified path.
     *
     * @param path the path to the file whose digest is to be calculated.
     * @return the digest of the file, or {@code null} if the path is a directory.
     * @throws IOException if an I/O error occurs while retrieving the digest.
     */
    public MCRDigest getDigest(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (this.isDirectory(lockedPath)) {
            return null;
        }
        return this.fileTracker.getDigest(lockedPath);
    }

    /**
     * Returns a file's creation time.
     *
     * @param path versioned path
     * @return time of file creation
     * @throws IOException if an I/O exception occurs
     */
    public FileTime getCreationTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        boolean added = this.isAdded(lockedPath);
        boolean inLocalStorage = this.isLocal(lockedPath);
        if (added && inLocalStorage) {
            return this.localStorage.readAttributes(lockedPath, BasicFileAttributes.class).creationTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        return FileTime.from(changeHistory.getOldest().getTimestamp().toInstant());
    }

    /**
     * Returns a file's last modified time.
     * 
     * @param path versioned path
     * @return last modified time
     * @throws IOException if an I/O exception occurs
     */
    public abstract FileTime getModifiedTime(MCRVersionedPath path) throws IOException;

    /**
     * Returns a file's last access time.
     *
     * @param path versioned path
     * @return last access time
     * @throws IOException if an I/O exception occurs
     */
    public abstract FileTime getAccessTime(MCRVersionedPath path) throws IOException;

    /**
     * Returns the size of the file (in bytes). The size may differ from the actual size on the file system due to
     * compression, support for sparse files, or other reasons.
     *
     * @param path the path
     * @return size in bytes
     * @throws IOException if an I/O exception occurs
     */
    public long getSize(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (isDirectory(lockedPath)) {
            return 0;
        }
        if (this.localStorage.exists(lockedPath)) {
            return this.localStorage.size(lockedPath);
        }
        OcflObjectVersionFile ocflObjectVersionFile = fromOcfl(lockedPath);
        String sizeAsString = ocflObjectVersionFile.getFixity().get(new SizeDigestAlgorithm());
        return Long.parseLong(sizeAsString);
    }

    /**
     * Returns the `filekey` of the given path.
     * <p>
     * Because OCFL is a version file system and the mycore implementation uses transactions this `filekey`
     * implementation differs from a Unix like system.
     * <p>
     * The Unix `filekey` is typically derived from the inode and device ID, ensuring uniqueness within the
     * filesystem. When a file is modified (written, moved...), the Unix `filekey` remains unchanged as long as the
     * inode remains the same.
     * <p>
     * In contrast, this implementation returns a new `filekey` as soon as a file is written or moved. The `filekey`
     * then remains constant as long as the transaction is open. After the transaction is committed the `filekey`
     * may change again.
     * <p>
     * Implementation detail: Be aware that for remote virtual objects the whole file is copied to the local
     * storage. Because the fileKey is not accessed frequently this should be acceptable. If this assumption proves
     * to be wrong a special implementation for remote virtual objects is required!
     *
     * @param path versioned path
     * @return fileKey
     * @throws IOException if an I/O error occurs.
     */
    public abstract Object getFileKey(MCRVersionedPath path) throws IOException;

    /**
     * Identifies the MIME type of a give path.
     * <p>
     * Implementation detail: Be aware that for remote virtual objects the whole file is copied to the local
     * storage. Because the MIME type is not accessed frequently this should be acceptable. If this assumption proves
     * to be wrong a special implementation for remote virtual objects is required! 
     *
     * @param path versioned path
     * @return the mime type
     * @throws IOException  if an I/O error occurs.
     */
    public String probeContentType(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        return Files.probeContentType(toPhysicalPath(lockedPath));
    }

    /**
     * Converts the specified versioned path to a local file system path. The path can either point at the local
     * temporary storage (if it exists) or at the original OCFL file or directory.
     * <p>
    *     Use the returned path ONLY for read operations. It's not allowed to write/move or remove the returned path.
     * Because this would create inconsistencies in the OCFL repository or the local storage.
     *
     * @param path the virtual path.
     * @return the physical path.
     * @throws IOException if an I/O error occurs.
     */
    protected abstract Path toPhysicalPath(MCRVersionedPath path) throws IOException;

    /**
     * Returns the change history of a path.
     *
     * @param path the path
     * @return file change history
     */
    protected FileChangeHistory getChangeHistory(MCRVersionedPath path) throws NoSuchFileException {
        boolean isDirectory = isDirectory(path);
        MCRVersionedPath resolvedPath = isDirectory ? path : this.fileTracker.findPath(path);
        FileChangeHistory changeHistory = this.changeHistoryCache.get(resolvedPath);
        if (changeHistory == null) {
            VersionNum versionNum = this.objectVersionId.getVersionNum();
            String logicalPath = resolvedPath.toRelativePath();
            String filesLogicalPath = toOcflFilesPath(logicalPath);
            if (versionNum != null) {
                changeHistory = isDirectory
                    ? getRepository().directoryChangeHistory(getObjectId(), filesLogicalPath, versionNum)
                    : getRepository().fileChangeHistory(getObjectId(), filesLogicalPath, versionNum);
            } else {
                changeHistory = isDirectory
                    ? getRepository().directoryChangeHistory(getObjectId(), filesLogicalPath)
                    : getRepository().fileChangeHistory(getObjectId(), filesLogicalPath);
            }
            this.changeHistoryCache.put(resolvedPath, changeHistory);
        }
        return changeHistory;
    }

    /**
     * Explicitly marks this virtual object for creation. A virtual object can only be marked for creation
     * if it does not exist yet in the repository or if it was marked for purge (removes the purge status).
     * <p>
     *     Implementation detail: A virtual object instance can exist without having a ocfl repository version and
     *     without being created.
     * </p>
     *
     * @throws MCRReadOnlyIOException if the object is read-only.
     * @throws FileAlreadyExistsException if the object already exist
     * @throws IOException if an I/O error occurs
     */
    public void create() throws IOException {
        if (this.readonly) {
            throw new MCRReadOnlyIOException("Cannot create read-only object: " + this);
        }
        boolean hasFiles = !this.fileTracker.paths().isEmpty();
        boolean hasDirectories = !this.emptyDirectoryTracker.paths().isEmpty();
        if (hasFiles || hasDirectories) {
            throw new FileAlreadyExistsException("Cannot create already existing object: " + this);
        }
        this.markForPurge = false;
        this.markForCreate = true;
        MCRVersionedPath rootDirectory = this.toMCRPath("/");
        this.localStorage.createDirectories(rootDirectory);
        this.emptyDirectoryTracker.update(rootDirectory, true);
    }

    /**
     * Marks this virtual object for purging.
     *
     * @throws MCRReadOnlyIOException if the object is read-only.
     */
    public void purge() throws MCRReadOnlyIOException {
        if (this.readonly) {
            throw new MCRReadOnlyIOException("Cannot purge read-only object: " + this);
        }
        this.fileTracker.purge();
        this.emptyDirectoryTracker.purge();
        this.markForCreate = false;
        this.markForPurge = true;
    }

    /**
     * Checks if this virtual object is modified.
     *
     * @return {@code true} if the object is modified, {@code false} otherwise.
     */
    public boolean isModified() {
        if (this.readonly) {
            return false;
        }
        if (this.markForPurge || this.markForCreate) {
            return true;
        }
        return !this.fileTracker.changes().isEmpty() || !this.emptyDirectoryTracker.changes().isEmpty();
    }

    /**
     * Checks if the given path is newly added.
     *
     * @param path versioned path
     * @return {@code true} if the path was added, {@code false} otherwise.
     */
    public boolean isAdded(MCRVersionedPath path) {
        MCRVersionedPath lockedPath = lockVersion(path);
        if (this.readonly || this.markForPurge || !this.exists(lockedPath)) {
            return false;
        }
        try {
            return this.isDirectory(lockedPath) ? this.emptyDirectoryTracker.isAdded(lockedPath)
                : this.fileTracker.isAdded(lockedPath);
        } catch (NoSuchFileException noSuchFileException) {
            return false;
        }
    }

    /**
     * Persists changes to this virtual object.
     *
     * @return {@code true} if changes were persisted, {@code false} otherwise.
     * @throws IOException if an I/O error occurs.
     */
    public boolean persist() throws IOException {
        if (!isModified()) {
            return false;
        }
        String objectId = objectVersionId.getObjectId();
        // purge object
        if (this.markForPurge) {
            repository.purgeObject(objectId);
            return true;
        }
        // persist
        String type = this.objectVersion == null ? MESSAGE_CREATED : MESSAGE_UPDATED;
        AtomicBoolean updatedFiles = new AtomicBoolean(false);
        AtomicBoolean updatedDirectories = new AtomicBoolean(false);
        // TODO: This should be removed when the metadata is also included in the transaction system!
        // TODO: we should not always write to head instead we should not accept new versions if a newer one exists.
        boolean alwaysWriteToHead = true;
        ObjectVersionId targetVersionId = alwaysWriteToHead ? ObjectVersionId.head(objectId) : objectVersionId;
        repository.updateObject(targetVersionId, new VersionInfo().setMessage(type), (updater) -> {
            try {
                updatedFiles.set(persistFileChanges(updater));
                updatedDirectories.set(persistDirectoryChanges(updater));
            } catch (IOException ioException) {
                throw new UncheckedIOException("Unable to " + type + " " + targetVersionId, ioException);
            }
        });
        return updatedFiles.get() || updatedDirectories.get();
    }

    /**
     * Persists file changes to the updater.
     *
     * @param updater the OCFL object updater.
     * @return {@code true} if changes were persisted, {@code false} otherwise.
     */
    protected boolean persistFileChanges(OcflObjectUpdater updater) throws IOException {
        List<MCROCFLFileTracker.Change<MCRVersionedPath>> changes = this.fileTracker.changes();
        for (MCROCFLFileTracker.Change<MCRVersionedPath> change : changes) {
            String ocflSourcePath = change.source().toRelativePath();
            String ocflFilesSourcePath = toOcflFilesPath(ocflSourcePath);
            switch (change.type()) {
                case ADDED_OR_MODIFIED -> {
                    Path localSourcePath = this.localStorage.toPhysicalPath(change.source());
                    long size = Files.size(localSourcePath);
                    updater
                        .addPath(localSourcePath, ocflFilesSourcePath, OcflOption.OVERWRITE)
                        .addFileFixity(ocflFilesSourcePath, new SizeDigestAlgorithm(), String.valueOf(size));
                }
                case DELETED -> updater.removeFile(ocflFilesSourcePath);
                case RENAMED -> {
                    String ocflTargetPath = change.target().toRelativePath();
                    String ocflFilesTargetPath = toOcflFilesPath(ocflTargetPath);
                    updater.renameFile(ocflFilesSourcePath, ocflFilesTargetPath);
                }
                default -> throw new IllegalStateException("Unexpected value: " + change.type());
            }
        }
        return !changes.isEmpty();
    }

    /**
     * Persists directory changes to the updater.
     *
     * @param updater the OCFL object updater.
     * @return {@code true} if changes were persisted, {@code false} otherwise.
     */
    protected boolean persistDirectoryChanges(OcflObjectUpdater updater) {
        List<MCROCFLDirectoryTracker.Change> changes = this.emptyDirectoryTracker.changes();
        for (MCROCFLDirectoryTracker.Change change : changes) {
            String ocflKeepFile = change.keepFile().toRelativePath();
            String ocflFilesKeepFile = toOcflFilesPath(ocflKeepFile);
            switch (change.type()) {
                case ADD_KEEP -> {
                    updater
                        .writeFile(InputStream.nullInputStream(), ocflFilesKeepFile)
                        .addFileFixity(ocflFilesKeepFile, new SizeDigestAlgorithm(), "0");
                }
                case REMOVE_KEEP -> {
                    updater.removeFile(ocflFilesKeepFile);
                }
                default -> throw new IllegalStateException("Unexpected value: " + change.type());
            }
        }
        return !changes.isEmpty();
    }

    /**
     * Checks if the specified path exists.
     *
     * @param path the path to check.
     * @throws NoSuchFileException if the path does not exist.
     */
    protected void checkExists(MCRVersionedPath path) throws NoSuchFileException {
        checkPurged(path);
        if (!exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
    }

    /**
     * Checks if the object is marked for purge.
     *
     * @param path the path to check.
     * @throws NoSuchFileException if the object is marked for purge.
     */
    protected void checkPurged(MCRVersionedPath path) throws NoSuchFileException {
        if (this.markForPurge) {
            throw new NoSuchFileException(path.toString());
        }
    }

    /**
     * Checks if the object is read-only.
     *
     * @throws MCRReadOnlyIOException if the object is read-only.
     */
    protected void checkReadOnly() throws MCRReadOnlyIOException {
        if (this.readonly) {
            throw new MCRReadOnlyIOException("OCFL Virtual Object '" + objectVersionId + "' is readonly.");
        }
    }

    /**
     * Locks a versioned path by ensuring that the owner and version of the given
     * {@link MCRVersionedPath} match the expected values for this virtual object.
     *
     * <p>If the owner and version match, the same {@code MCRVersionedPath} is returned.
     * If they do not match, an {@link MCROCFLVersionMismatchException} is thrown.</p>
     *
     * <p>Note: If the {@code MCRVersionedPath} does not specify a version (i.e.,
     * {@link MCRVersionedPath#getVersion()}  == null}), the virtual object's version is assumed without error.</p>
     *
     * @param versionedPath the {@code MCRVersionedPath} to validate and lock.
     * @return the same {@code MCRVersionedPath} if the owner and version match, or a new
     *         {@code MCRVersionedPath} with the correct owner and version.
     * @throws MCROCFLVersionMismatchException if the owner or version does not match
     *         and the path version is specified.
     */
    protected MCRVersionedPath lockVersion(MCRVersionedPath versionedPath) {
        String owner = getOwner();
        String version = getVersion();
        String pathOwner = versionedPath.getOwner();
        String pathVersion = versionedPath.getVersion();
        if (Objects.equals(owner, pathOwner) && Objects.equals(version, pathVersion)) {
            return versionedPath;
        }
        if (!Objects.equals(pathOwner, owner)) {
            throw new MCROCFLVersionMismatchException(
                "Expected owner '" + owner + "' but got '" + pathOwner + "'. Cannot use path '"
                    + versionedPath + "' in virtual object '" + this + "'");
        }
        if (pathVersion != null && !Objects.equals(pathVersion, version)) {
            throw new MCROCFLVersionMismatchException(
                "Expected version '" + version + "' but got '" + pathVersion + "'. Cannot use path '"
                    + versionedPath + "' in virtual object '" + this + "'");
        }
        return MCROCFLFileSystemProvider.get().getPath(owner, version, versionedPath.getOwnerRelativePath());
    }

    /**
     * Releases the version on a given {@link MCRVersionedPath}, returning the path
     * that points to the latest (head) version of the virtual object.
     * <p>
     * This is necessary for the mycore event system. Due to the fact that the metadata is not yet
     * included in the transaction system. Therefore, another version of the ocfl object could have
     * been created, leaving to paths in {@link org.mycore.common.MCRSession#onCommit(Runnable)} pointing to
     * the wrong version.
     * <p>
     * TODO: This should be removed when the metadata is also included in the transaction system!
     *
     * @param versionedPath the {@link MCRVersionedPath} to release, retaining the same owner.
     * @return a {@link MCRVersionedPath} that points to the head (latest) version of
     *         the path for the specified owner.
     */
    protected MCRVersionedPath releaseVersion(MCRVersionedPath versionedPath) {
        String owner = getOwner();
        String ownerRelativePath = versionedPath.getOwnerRelativePath();
        return MCROCFLFileSystemProvider.get().head(owner, ownerRelativePath);
    }

    /**
     * Tracks a file write operation.
     *
     * @param path the path of the file.
     * @param event the type of event to fire.
     * @throws IOException if an I/O error occurs.
     */
    protected void trackFileWrite(MCRVersionedPath path, MCREvent.EventType event) throws IOException {
        this.fileTracker.write(path);
        this.emptyDirectoryTracker.update(path.getParent(), false);
        MCRVersionedPath releasedVersionPath = releaseVersion(path);
        if (event != null) {
            switch (event) {
                case CREATE -> MCRPathEventHelper.fireFileCreateEvent(releasedVersionPath);
                case UPDATE -> MCRPathEventHelper.fireFileUpdateEvent(releasedVersionPath);
                default -> {
                }
            }
        }
    }

    protected void trackEmptyDirectory(MCRVersionedPath path) throws IOException {
        if (path == null) {
            return;
        }
        List<Path> directoryListing = list(path);
        this.emptyDirectoryTracker.update(path, directoryListing.isEmpty());
    }

    /**
     * Converts the specified OCFL path to an {@link MCRVersionedPath}.
     *
     * @param ocflPath the OCFL path.
     * @return the versioned path.
     */
    public MCRVersionedPath toMCRPath(String ocflPath) {
        String owner = getOwner();
        String version = getVersion();
        String path = MCRAbstractFileSystem.SEPARATOR_STRING + ocflPath;
        return MCROCFLFileSystemProvider.get().getPath(owner, version, path);
    }

    /**
     * Returns the owner of this virtual object.
     *
     * @return the owner of this virtual object.
     */
    public String getOwner() {
        String objectId = this.objectVersionId.getObjectId();
        return MCROCFLObjectIDPrefixHelper.fromDerivateObjectId(objectId);
    }

    /**
     * Returns the ocfl objectId of this virtual object.
     *
     * @return the ocfl objectId of this virtual object.
     */
    public String getObjectId() {
        return this.objectVersionId.getObjectId();
    }

    /**
     * Returns the version of this virtual object.
     *
     * @return the version of this virtual object, or {@code null} if no version is set.
     */
    public String getVersion() {
        VersionNum versionNum = this.objectVersionId.getVersionNum();
        return versionNum != null ? versionNum.toString() : null;
    }

    /**
     * Checks if this virtual object is read-only.
     *
     * @return {@code true} if this virtual object is read-only, {@code false} otherwise.
     */
    public boolean isReadonly() {
        return readonly;
    }

    /**
     * Checks if this virtual object is marked for purge.
     *
     * @return {@code true} if this virtual object is marked for purge, {@code false} otherwise.
     */
    public boolean isMarkedForPurge() {
        return markForPurge;
    }

    /**
     * Checks if this virtual object is marked for creation.
     *
     * @return {@code true} if this virtual object is marked for creation, {@code false} otherwise.
     */
    public boolean isMarkedForCreate() {
        return markForCreate;
    }

    /**
     * Returns the repository of this virtual object.
     *
     * @return the repository of this virtual object.
     */
    public MCROCFLRepository getRepository() {
        return this.repository;
    }

    /**
     * Creates a deep clone of this virtual object.
     *
     * @return a newly generated virtual object.
     */
    public MCROCFLVirtualObject deepClone() {
        return deepClone(this.readonly);
    }

    /**
     * Creates a deep clone of this virtual object.
     *
     * @return a newly generated virtual object.
     */
    public abstract MCROCFLVirtualObject deepClone(boolean readonly);

    @Override
    public String toString() {
        String version = this.getVersion();
        return this.getOwner() + "@" + (version == null ? "head" : version);
    }

    protected MCRDigest calculateDigest(MCRVersionedPath path) {
        try {
            if (isDirectory(path)) {
                return null;
            }
            Path physicalPath = toPhysicalPath(path);
            String digestValue = MCRUtils.getDigest(MCRSHA512Digest.ALGORITHM, Files.newInputStream(physicalPath));
            return new MCRSHA512Digest(digestValue);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Unable to calculate digest for path '" + path + "'.", ioException);
        }
    }
}
