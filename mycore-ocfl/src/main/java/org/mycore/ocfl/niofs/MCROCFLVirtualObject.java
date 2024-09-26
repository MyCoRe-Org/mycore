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

    public static final String KEEP_FILE = ".keep";

    protected final MCROCFLRepository repository;

    protected final ObjectVersionId objectVersionId;

    protected OcflObjectVersion objectVersion;

    protected final MCROCFLTempFileStorage localStorage;

    protected final boolean readonly;

    protected boolean markForCreate;

    protected boolean markForPurge;

    protected MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker;

    protected MCROCFLEmptyDirectoryTracker emptyDirectoryTracker;

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
        MCROCFLEmptyDirectoryTracker directoryTracker) {
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
        MCRVersionedPath rootPath = this.toMCRPath("/");
        if (this.objectVersion == null) {
            this.fileTracker = new MCROCFLFileTracker<>(new HashMap<>(), this::calculateDigest);
            this.emptyDirectoryTracker = new MCROCFLEmptyDirectoryTracker(rootPath, new HashMap<>());
            return;
        }
        Map<MCRVersionedPath, MCRDigest> filePaths = new HashMap<>();
        Map<MCRVersionedPath, Boolean> directoryPaths = new HashMap<>();
        for (OcflObjectVersionFile file : this.objectVersion.getFiles()) {
            MCRVersionedPath filePath = toMCRPath(file.getPath());
            boolean hasKeepFile = filePath.getFileName().toString().equals(KEEP_FILE);
            if (!hasKeepFile) {
                String sha512Digest = file.getFixity().get(DigestAlgorithmRegistry.sha512);
                filePaths.put(filePath, new MCRSHA512Digest(sha512Digest));
            }
            directoryPaths.put(filePath.getParent(), hasKeepFile);
        }
        this.fileTracker = new MCROCFLFileTracker<>(filePaths, this::calculateDigest);
        this.emptyDirectoryTracker = new MCROCFLEmptyDirectoryTracker(rootPath, directoryPaths);
    }

    /**
     * Creates a directory at the specified path.
     *
     * @param directoryPath the path of the directory to create.
     * @throws IOException if an I/O error occurs.
     */
    public void createDirectory(MCRVersionedPath directoryPath) throws IOException {
        checkPurged(directoryPath);
        checkReadOnly();
        if (exists(directoryPath)) {
            throw new FileAlreadyExistsException(directoryPath.toString());
        }
        this.localStorage.createDirectories(directoryPath);
        this.emptyDirectoryTracker.update(directoryPath, true);
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
        if (this.markForPurge) {
            return false;
        }
        if (path.toRelativePath().isEmpty()) {
            return this.markForCreate || this.objectVersion != null;
        }
        return this.fileTracker.exists(path) || this.emptyDirectoryTracker.exists(path);
    }

    /**
     * Checks if the specified path is stored locally.
     *
     * @param path the path to check.
     * @return {@code true} if the path is stored locally, {@code false} otherwise.
     * @throws NoSuchFileException if the path does not exist.
     */
    public boolean isLocal(MCRVersionedPath path) throws NoSuchFileException {
        checkExists(path);
        return !this.markForPurge && this.localStorage.exists(path);
    }

    /**
     * Checks if the specified path is a directory.
     *
     * @param path the path to check.
     * @return {@code true} if the path is a directory, {@code false} otherwise.
     * @throws NoSuchFileException if the path does not exist.
     */
    public boolean isDirectory(MCRVersionedPath path) throws NoSuchFileException {
        checkExists(path);
        return !this.markForPurge && this.emptyDirectoryTracker.exists(path);
    }

    /**
     * Checks if the specified directory is empty.
     *
     * @param directoryPath the path to the directory.
     * @return {@code true} if the directory is empty, {@code false} otherwise.
     * @throws NoSuchFileException if the path does not exist.
     */
    public boolean isDirectoryEmpty(MCRVersionedPath directoryPath) throws NoSuchFileException {
        checkExists(directoryPath);
        return this.emptyDirectoryTracker.isEmpty(directoryPath);
    }

    /**
     * Checks if the specified path is a file.
     *
     * @param path the path to check.
     * @return {@code true} if the path is a file, {@code false} otherwise.
     */
    public boolean isFile(MCRVersionedPath path) {
        return !this.markForPurge && this.fileTracker.exists(path);
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
        checkPurged(path);
        boolean exists = this.exists(path);
        boolean create = options.contains(StandardOpenOption.CREATE);
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        boolean read = options.isEmpty() || options.contains(StandardOpenOption.READ);
        // check read
        if (!read) {
            checkReadOnly();
        }
        // create
        if (createNew && exists) {
            throw new FileAlreadyExistsException(path.toString());
        }
        // check existing
        if (!exists && !(create || createNew)) {
            throw new NoSuchFileException(path.toString());
        }
        if (create || createNew) {
            return createByteChannel(path, options, fileAttributes, createNew);
        } else if (read) {
            return readByteChannel(path, options, fileAttributes);
        } else {
            return writeByteChannel(path, options, fileAttributes);
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
        if (this.localStorage.exists(path)) {
            return;
        }
        if (!isFile(path)) {
            this.localStorage.createDirectories(path);
            return;
        }
        OcflObjectVersionFile ocflFile = fromOcfl(path);
        try (FixityCheckInputStream stream = ocflFile.getStream()) {
            this.localStorage.copy(stream, path);
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
        String ocflFilePath = ocflOriginalPath.toRelativePath();
        OcflObjectVersionFile file = objectVersion.getFile(ocflFilePath);
        if (file == null) {
            throw new NoSuchFileException(ocflOriginalPath.toString());
        }
        return file;
    }

    /**
     * Deletes the specified path.
     *
     * @param path the path to delete.
     * @throws IOException if an I/O error occurs.
     */
    public void delete(MCRVersionedPath path) throws IOException {
        checkPurged(path);
        checkReadOnly();
        if (!exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        if (isDirectory(path)) {
            if (!isDirectoryEmpty(path)) {
                throw new DirectoryNotEmptyException(path.toString());
            }
            this.localStorage.deleteIfExists(path);
            this.emptyDirectoryTracker.remove(path);
        } else {
            this.localStorage.deleteIfExists(path);
            this.fileTracker.delete(path);
        }
        trackEmptyDirectory(path.getParent());
        MCRPathEventHelper.fireFileDeleteEvent(path);
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
        checkPurged(source);
        checkReadOnly();
        if (this.localStorage.exists(source)) {
            this.localStorage.move(source, target, options);
        } else if (this.localStorage.exists(target)) {
            this.localStorage.deleteIfExists(target);
        }
        if (this.isDirectory(source)) {
            this.renameDirectory(source, target);
        } else {
            this.renameFile(source, target);
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
        MCRPathEventHelper.fireFileMoveEvent(source, target, create);
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
        checkPurged(directory);
        if (this.fileTracker.exists(directory)) {
            throw new NotDirectoryException(directory.toString());
        }
        if (!this.emptyDirectoryTracker.exists(directory)) {
            throw new NoSuchFileException(directory.toString());
        }
        final List<Path> paths = list(directory, filter, paths());
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
        List<Path> finalPaths = new ArrayList<>();
        // filter paths for directory
        Set<MCRVersionedPath> pathsInDirectory = paths.stream()
            .filter(path -> isPathInDirectory(directory, path))
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
        checkExists(path);
        if (this.isDirectory(path)) {
            return null;
        }
        return this.fileTracker.getDigest(path);
    }

    /**
     * Returns a file's creation time.
     *
     * @param path versioned path
     * @return time of file creation
     * @throws IOException if an I/O exception occurs
     */
    public FileTime getCreationTime(MCRVersionedPath path) throws IOException {
        checkExists(path);
        boolean added = this.isAdded(path);
        boolean inLocalStorage = this.isLocal(path);
        if (added && inLocalStorage) {
            Path physicalPath = this.localStorage.toPhysicalPath(path);
            return Files.readAttributes(physicalPath, BasicFileAttributes.class).creationTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(path);
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
        checkExists(path);
        if (isDirectory(path)) {
            return 0;
        }
        if (this.localStorage.exists(path)) {
            Path physicalPath = this.localStorage.toPhysicalPath(path);
            return Files.size(physicalPath);
        }
        OcflObjectVersionFile ocflObjectVersionFile = fromOcfl(path);
        String sizeAsString = ocflObjectVersionFile.getFixity().get(new SizeDigestAlgorithm());
        return Long.parseLong(sizeAsString);
    }

    /**
     * Returns the `filekey` of the given path.
     * <p>
     *     Because OCFL is a version file system and the mycore implementation uses transactions this `filekey`
     *     implementation differs from a Unix like system.
     * </p>
     * <p>
     *     The Unix `filekey` is typically derived from the inode and device ID, ensuring uniqueness within the
     *     filesystem. When a file is modified (written, moved...), the Unix `filekey` remains unchanged as long as the
     *     inode remains the same.
     * </p>
     * <p>
     *     In contrast this implementation returns a new `filekey` as soon as a file is written or moved. The `filekey`
     *     then remains constant as long as the transaction is open. After the transaction is committed the `filekey`
     *     may change again.
     * </p>
     * <p>
     *     Implementation detail: Be aware that this method calls {@link #toPhysicalPath(MCRVersionedPath)}. For remote
     *     virtual objects this means that the whole file is copied to the local storage. Because the fileKey is not
     *     accessed frequently this should be acceptable. If this assumption proves to be wrong a special implementation
     *     for remote virtual objects is required!
     * </p>
     *
     * @param path versioned path
     * @return fileKey
     * @throws IOException if an I/O error occurs.
     */
    public Object getFileKey(MCRVersionedPath path) throws IOException {
        Path physicalPath = toPhysicalPath(path);
        return Files.readAttributes(physicalPath, BasicFileAttributes.class).fileKey();
    }

    /**
     * Converts the specified versioned path to a local file system path. The path can either point at the local
     * temporary storage (if it exists) or at the original OCFL file or directory.
     * <p>Use the returned path ONLY for read operations. Its not allowed to write/move or remove the returned path.
     * Because this would create inconsistencies in the OCFL repository or the local storage.</p>
     *
     * @param path the virtual path.
     * @return the physical path.
     * @throws IOException if an I/O error occurs.
     */
    public abstract Path toPhysicalPath(MCRVersionedPath path) throws IOException;

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
            if (versionNum != null) {
                changeHistory = isDirectory
                    ? getRepository().directoryChangeHistory(getObjectId(), logicalPath, versionNum)
                    : getRepository().fileChangeHistory(getObjectId(), logicalPath, versionNum);
            } else {
                changeHistory = isDirectory
                    ? getRepository().directoryChangeHistory(getObjectId(), logicalPath)
                    : getRepository().fileChangeHistory(getObjectId(), logicalPath);
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
     */
    public void create() throws MCRReadOnlyIOException, FileAlreadyExistsException {
        if (this.readonly) {
            throw new MCRReadOnlyIOException("Cannot create read-only object: " + this);
        }
        if (this.objectVersion != null && !this.markForPurge) {
            throw new FileAlreadyExistsException("Cannot create already existing object: " + this);
        }
        this.markForPurge = false;
        this.markForCreate = true;
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
        if (this.readonly || this.markForPurge || !this.exists(path)) {
            return false;
        }
        try {
            return this.isDirectory(path) ? this.emptyDirectoryTracker.isAdded(path) : this.fileTracker.isAdded(path);
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
        // purge object
        if (this.markForPurge) {
            repository.purgeObject(objectVersionId.getObjectId());
            return true;
        }
        // persist
        String type = this.objectVersion == null ? "create" : "update";
        AtomicBoolean updatedFiles = new AtomicBoolean(false);
        AtomicBoolean updatedDirectories = new AtomicBoolean(false);
        repository.updateObject(objectVersionId, new VersionInfo().setMessage(type), (updater) -> {
            try {
                updatedFiles.set(persistFileChanges(updater));
                updatedDirectories.set(persistDirectoryChanges(updater));
            } catch (IOException ioException) {
                throw new UncheckedIOException("Unable to " + type + " " + objectVersionId, ioException);
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
            switch (change.type()) {
                case ADDED_OR_MODIFIED -> {
                    Path localSourcePath = this.localStorage.toPhysicalPath(change.source());
                    long size = Files.size(localSourcePath);
                    updater
                        .addPath(localSourcePath, ocflSourcePath, OcflOption.OVERWRITE)
                        .addFileFixity(ocflSourcePath, new SizeDigestAlgorithm(), String.valueOf(size));
                }
                case DELETED -> updater.removeFile(ocflSourcePath);
                case RENAMED -> {
                    String ocflTargetPath = change.target().toRelativePath();
                    updater.renameFile(ocflSourcePath, ocflTargetPath);
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
        List<MCROCFLEmptyDirectoryTracker.Change> changes = this.emptyDirectoryTracker.changes();
        for (MCROCFLEmptyDirectoryTracker.Change change : changes) {
            String ocflKeepFile = change.keepFile().toRelativePath();
            switch (change.type()) {
                case ADD_KEEP -> {
                    updater
                        .writeFile(InputStream.nullInputStream(), ocflKeepFile)
                        .addFileFixity(ocflKeepFile, new SizeDigestAlgorithm(), "0");
                }
                case REMOVE_KEEP -> {
                    updater.removeFile(ocflKeepFile);
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
     * Tracks a file write operation.
     *
     * @param path the path of the file.
     * @param event the type of event to fire.
     * @throws IOException if an I/O error occurs.
     */
    protected void trackFileWrite(MCRVersionedPath path, MCREvent.EventType event) throws IOException {
        this.fileTracker.write(path);
        this.emptyDirectoryTracker.update(path.getParent(), false);
        if (event != null) {
            switch (event) {
                case CREATE -> MCRPathEventHelper.fireFileCreateEvent(path);
                case UPDATE -> MCRPathEventHelper.fireFileUpdateEvent(path);
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
        VersionNum versionNum = this.objectVersionId.getVersionNum();
        String version = versionNum != null ? versionNum.toString() : null;
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
