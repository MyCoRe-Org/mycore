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
import static org.mycore.ocfl.util.MCROCFLVersionHelper.MESSAGE_DELETED;
import static org.mycore.ocfl.util.MCROCFLVersionHelper.MESSAGE_UPDATED;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.digest.MCRDigest;
import org.mycore.common.digest.MCRSHA512Digest;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCRPathEventHelper;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRReadOnlyIOException;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLException;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.util.MCROCFLDeleteUtils;
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
 * Represents an in-memory view of a single OCFL object, which functions as a virtual file system.
 * <p>
 * This abstract class provides the core functionality for managing the state of an OCFL object
 * (e.g., a MyCoRe derivate) as if it were a file system. It handles file and directory operations such as creation,
 * reading, writing, renaming, and deleting.
 * <h2>State Management:</h2>
 * The class maintains the state of the object's files and directories using two key trackers:
 * <ul>
 *   <li>
 *     <b>{@link MCROCFLFileTracker}:</b> Tracks the state of all files, including their paths and digests. It
 *     identifies additions, deletions, modifications, and renames.
 *   </li>
 *   <li>
 *     <b>{@link MCROCFLDirectoryTracker}:</b> Tracks all directories and whether they are "empty" in the OCFL
 *     sense (i.e., whether they require a {@code .keep} file).
 *   </li>
 * </ul>
 * <h2>Transactional Behavior:</h2>
 * <p>
 * All modifications are performed on a {@link MCROCFLTransactionalStorage}, which is a local staging area.
 * The changes are only written back to the persistent OCFL repository when the {@link #persist()} method is called,
 * typically at the end of a successful {@link MCROCFLFileSystemTransaction}.
 *
 * @see MCROCFLFileSystemProvider
 * @see MCROCFLFileTracker
 * @see MCROCFLDirectoryTracker
 */
public abstract class MCROCFLVirtualObject {

    /**
     * The standard directory within an OCFL object's content where all files for this file system are stored.
     */
    public static final String FILES_DIRECTORY = "files/";

    /**
     * The name of the special file used to ensure empty directories are preserved in OCFL.
     */
    public static final String KEEP_FILE = ".keep";

    protected final MCROCFLRepository repository;

    protected final ObjectVersionId objectVersionId;

    protected OcflObjectVersion objectVersion;

    protected final MCROCFLTransactionalStorage transactionalStorage;

    protected final MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator;

    protected final boolean readonly;

    protected boolean markForCreate;

    protected boolean markForPurge;

    protected MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker;

    protected MCROCFLDirectoryTracker directoryTracker;

    protected Map<MCRVersionedPath, FileChangeHistory> changeHistoryCache;

    /**
     * Constructs a new {@code MCROCFLVirtualObject} for an object not yet loaded from the repository.
     *
     * @param repository           the OCFL repository.
     * @param objectVersionId      the versioned ID of the object.
     * @param transactionalStorage the local transactional file storage.
     * @param digestCalculator     the calculator for generating file digests.
     * @param readonly             whether the object is read-only.
     */
    public MCROCFLVirtualObject(MCROCFLRepository repository, ObjectVersionId objectVersionId,
        MCROCFLTransactionalStorage transactionalStorage, MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator,
        boolean readonly) {
        this(repository, objectVersionId, null, transactionalStorage, digestCalculator, readonly);
    }

    /**
     * Constructs a new {@code MCROCFLVirtualObject} with a preloaded OCFL object version.
     *
     * @param repository           the OCFL repository.
     * @param objectVersion        the OCFL object version.
     * @param transactionalStorage the local transactional file storage.
     * @param digestCalculator     the calculator for generating file digests.
     * @param readonly             whether the object is read-only.
     */
    public MCROCFLVirtualObject(MCROCFLRepository repository, OcflObjectVersion objectVersion,
        MCROCFLTransactionalStorage transactionalStorage, MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator,
        boolean readonly) {
        this(repository, objectVersion.getObjectVersionId(), objectVersion, transactionalStorage, digestCalculator,
            readonly);
    }

    /**
     * Base constructor for a new {@code MCROCFLVirtualObject}, initializing its state.
     *
     * @param repository           the OCFL repository.
     * @param versionId            the versioned ID of the object.
     * @param objectVersion        the OCFL object version (can be null if not loaded).
     * @param transactionalStorage the local transactional file storage.
     * @param digestCalculator     the calculator for generating file digests.
     * @param readonly             whether the object is read-only.
     */
    protected MCROCFLVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTransactionalStorage transactionalStorage,
        MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator, boolean readonly) {
        this(repository, versionId, objectVersion, transactionalStorage, digestCalculator, readonly, null, null);
        initTrackers();
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
    protected MCROCFLVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTransactionalStorage transactionalStorage,
        MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator, boolean readonly,
        MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLDirectoryTracker directoryTracker) {
        Objects.requireNonNull(repository);
        Objects.requireNonNull(versionId);
        Objects.requireNonNull(transactionalStorage);
        Objects.requireNonNull(digestCalculator);
        this.repository = repository;
        this.objectVersionId = versionId;
        this.objectVersion = objectVersion;
        this.transactionalStorage = transactionalStorage;
        this.digestCalculator = digestCalculator;
        this.readonly = readonly;
        this.markForCreate = false;
        this.markForPurge = false;
        this.fileTracker = fileTracker;
        this.directoryTracker = directoryTracker;
        this.changeHistoryCache = new HashMap<>();
    }

    /**
     * Initializes the file and directory trackers based on the loaded {@code objectVersion}.
     * If no version is loaded, empty trackers are created.
     */
    protected void initTrackers() {
        if (this.objectVersion == null) {
            this.fileTracker = new MCROCFLFileTracker<>(new HashMap<>(), new DigestCalculator(this));
            this.directoryTracker = new MCROCFLDirectoryTracker(new HashMap<>());
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
        this.fileTracker = new MCROCFLFileTracker<>(filePaths, new DigestCalculator(this));
        this.directoryTracker = new MCROCFLDirectoryTracker(directoryPaths);
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
        this.transactionalStorage.createDirectories(lockedDirectory);
        this.directoryTracker.update(lockedDirectory, true);
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
        return Stream.of(this.directoryTracker.paths(), this.fileTracker.paths())
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
        return this.fileTracker.exists(lockedPath) || this.directoryTracker.exists(lockedPath);
    }

    /**
     * Checks if the specified path is stored locally.
     *
     * @param path the path to check.
     * @return {@code true} if the path is stored locally, {@code false} otherwise.
     * @throws NoSuchFileException if the path does not exist.
     */
    public boolean existInTransactionalStorage(MCRVersionedPath path) throws NoSuchFileException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        return !this.markForPurge && this.transactionalStorage.exists(lockedPath);
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
        return !this.markForPurge && this.directoryTracker.exists(lockedPath);
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
        return this.directoryTracker.isEmpty(lockedDirectoryPath);
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
            return createByteChannel(lockedPath, options, fileAttributes);
        } else if (read) {
            return readByteChannel(lockedPath, options);
        } else {
            return writeByteChannel(lockedPath, options);
        }
    }

    protected MCROCFLClosableCallbackChannel createByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes)
        throws IOException {
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        boolean isKeepFile = path.getFileName().toString().equals(KEEP_FILE);
        boolean fireCreateEvent = createNew || Files.notExists(path);
        SeekableByteChannel seekableByteChannel =
            this.transactionalStorage.newByteChannel(path, options, fileAttributes);
        return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
            if (!isKeepFile) {
                trackFileWrite(path, fireCreateEvent ? MCREvent.EventType.CREATE : MCREvent.EventType.UPDATE);
            } else {
                trackEmptyDirectory(path.getParent());
            }
        });
    }

    protected abstract SeekableByteChannel readByteChannel(MCRVersionedPath path,
        Set<? extends OpenOption> options) throws IOException;

    protected SeekableByteChannel writeByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options)
        throws IOException {
        Set<OpenOption> writeOptions = new HashSet<>(options);
        if (!options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
            // need a copy if we do not truncate
            localTransactionalCopy(path);
        } else {
            // need to add CREATE if it exists in virtual object but not in transactional storage
            boolean existsInVirtualObject = exists(path);
            boolean existsInTransactionalStorage = this.transactionalStorage.exists(path);
            if (existsInVirtualObject && !existsInTransactionalStorage) {
                writeOptions.add(StandardOpenOption.CREATE);
            }
        }
        SeekableByteChannel seekableByteChannel = this.transactionalStorage.newByteChannel(path, writeOptions);
        return new MCROCFLClosableCallbackChannel(seekableByteChannel, () -> {
            trackFileWrite(path, MCREvent.EventType.UPDATE);
        });
    }

    /**
     * Copies a file from the OCFL repository to the transactional storage.
     * This should be called whenever it is necessary to work on a copy rather than the OCFL file directly.
     * <p>
     * If the requested path is a directory and it does not exist yet, an empty directory is created in the local
     * storage. This guarantees that the given path is always accessible.
     *
     * @param path the path to the file.
     * @throws IOException if an I/O error occurs.
     */
    public void localTransactionalCopy(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        if (this.transactionalStorage.exists(lockedPath)) {
            return;
        }
        if (!isFile(lockedPath)) {
            this.transactionalStorage.createDirectories(lockedPath);
            return;
        }
        OcflObjectVersionFile ocflFile = fromOcfl(lockedPath);
        try (FixityCheckInputStream stream = ocflFile.getStream()) {
            this.transactionalStorage.copy(stream, lockedPath);
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
            this.transactionalStorage.deleteIfExists(lockedPath);
            this.directoryTracker.remove(lockedPath);
        } else {
            this.transactionalStorage.deleteIfExists(lockedPath);
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
        if (this.transactionalStorage.exists(lockedSource)) {
            this.transactionalStorage.move(lockedSource, lockedTarget, options);
        } else if (this.transactionalStorage.exists(lockedTarget)) {
            this.transactionalStorage.deleteIfExists(lockedTarget);
        }
        if (this.isDirectory(lockedSource)) {
            this.renameDirectory(lockedSource, lockedTarget);
        } else {
            this.renameFile(source, lockedTarget);
        }
    }

    protected void renameDirectory(MCRVersionedPath source, MCRVersionedPath target) throws IOException {
        if (!this.directoryTracker.isEmpty(source)) {
            throw new DirectoryNotEmptyException(source.toString());
        }
        this.directoryTracker.rename(source, target);
    }

    protected void renameFile(MCRVersionedPath source, MCRVersionedPath target) throws IOException {
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
        if (!this.directoryTracker.exists(lockedDirectory)) {
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
        boolean activeTransaction = MCROCFLFileSystemTransaction.isActive();
        boolean added = this.isAdded(lockedPath);
        boolean inLocalStorage = this.existInTransactionalStorage(lockedPath);
        if (activeTransaction && added && inLocalStorage) {
            return this.transactionalStorage.readAttributes(lockedPath, BasicFileAttributes.class).creationTime();
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
    public FileTime getModifiedTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (MCROCFLFileSystemTransaction.isActive() && transactionalStorage.exists(path)) {
            return transactionalStorage.readAttributes(lockedPath, BasicFileAttributes.class).lastModifiedTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        return FileTime.from(changeHistory.getMostRecent().getTimestamp().toInstant());
    }

    /**
     * Returns a file's last access time.
     *
     * @param path versioned path
     * @return last access time
     * @throws IOException if an I/O exception occurs
     */
    public FileTime getAccessTime(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);
        if (MCROCFLFileSystemTransaction.isActive() && transactionalStorage.exists(path)) {
            return transactionalStorage.readAttributes(lockedPath, BasicFileAttributes.class).lastAccessTime();
        }
        FileChangeHistory changeHistory = getChangeHistory(lockedPath);
        return FileTime.from(changeHistory.getMostRecent().getTimestamp().toInstant());
    }

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
        // check directory
        if (isDirectory(lockedPath)) {
            return 0;
        }
        // read from transactional storage
        if (MCROCFLFileSystemTransaction.isActive() && transactionalStorage.exists(path)) {
            return transactionalStorage.size(lockedPath);
        }
        // read from ocfl
        OcflObjectVersionFile ocflObjectVersionFile = fromOcfl(lockedPath);
        String sizeAsString = ocflObjectVersionFile.getFixity().get(new SizeDigestAlgorithm());
        return Long.parseLong(sizeAsString);
    }

    /**
     * Returns the `filekey` of the given path.
     * <p>
     * For local repositories the returns the file key of the underlying file system. For remote repositories this will
     * return null.
     *
     * @param path versioned path
     * @return fileKey
     * @throws IOException if an I/O error occurs.
     */
    public abstract Object getFileKey(MCRVersionedPath path) throws IOException;

    /**
     * Probes the content type of a file.
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
    *  Use the returned path ONLY for read operations. It's not allowed to write/move or remove the returned path.
     * Because this would create inconsistencies in the OCFL repository or the local storage.
     *
     * @param path the virtual path.
     * @return the physical path.
     * @throws IOException if an I/O error occurs.
     * @throws CannotDeterminePhysicalPathException the local path can't be created
     */
    protected abstract Path toPhysicalPath(MCRVersionedPath path)
        throws IOException, CannotDeterminePhysicalPathException;

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
     * The purge status can only be removed in the same transaction the purge was applied. If a purge was
     * done and the transaction was committed, the object cannot be created anymore!
     * <p>
     * Implementation detail: A virtual object instance can exist without having an ocfl repository version and
     * without being created.
     *
     * @throws MCRReadOnlyIOException if the object is read-only.
     * @throws FileAlreadyExistsException if the object already exists
     * @throws IOException if an I/O error occurs
     */
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
        this.transactionalStorage.createDirectories(rootDirectory);
        this.directoryTracker.update(rootDirectory, true);
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
        this.directoryTracker.purge();
        this.markForCreate = false;
        this.markForPurge = true;
    }

    /**
     * Checks if this virtual object is modified.
     *
     * @return {@code true} if the object is modified, {@code false} otherwise.
     */
    public boolean isModified() throws IOException {
        if (this.readonly) {
            return false;
        }
        if (this.markForPurge || this.markForCreate) {
            return true;
        }
        return !this.fileTracker.changes().isEmpty() || !this.directoryTracker.changes().isEmpty();
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
            return this.isDirectory(lockedPath) ? this.directoryTracker.isAdded(lockedPath)
                : this.fileTracker.isAdded(lockedPath);
        } catch (NoSuchFileException noSuchFileException) {
            return false;
        }
    }

    /**
     * Persists all tracked changes (additions, modifications, deletions, renames) to the OCFL repository.
     * <p>
     * This method orchestrates the update of the underlying OCFL object. It checks if the object is marked for
     * purge and handles that case first. Otherwise, it constructs a new version in the OCFL repository by applying
     * all file and directory changes recorded by the trackers.
     * <p>
     * This method should only be called by the {@link MCROCFLFileSystemTransaction} during a commit.
     *
     * @return {@code true} if any changes were successfully persisted, creating a new OCFL version.
     *         {@code false} if there were no changes to persist.
     * @throws IOException if an I/O error occurs during the persistence operation.
     */
    public boolean persist() throws IOException {
        if (!isModified()) {
            return false;
        }
        // TODO: This should be removed when the metadata is also included in the transaction system!
        // TODO: we should not always write to head instead we should not accept new versions if a newer one exists.
        String objectId = objectVersionId.getObjectId();
        boolean alwaysWriteToHead = true;
        ObjectVersionId targetVersionId = alwaysWriteToHead ? ObjectVersionId.head(objectId) : objectVersionId;

        // purge object
        if (this.markForPurge) {
            persistPurge(targetVersionId);
            return true;
        }
        // persist
        String type = this.objectVersion == null ? MESSAGE_CREATED : MESSAGE_UPDATED;
        AtomicBoolean updatedFiles = new AtomicBoolean(false);
        AtomicBoolean updatedDirectories = new AtomicBoolean(false);
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
     * Handles the deletion of an OCFL object when marked for purge.
     * <p>
     * If the repository configuration allows purging, the object is completely removed from the repository.
     * Otherwise, a new version is committed to mark the object as deleted while preserving its history.
     *
     * @param targetVersionId the OCFL object version ID that should be purged.
     */
    protected void persistPurge(ObjectVersionId targetVersionId) {
        String objectId = targetVersionId.getObjectId();
        String owner = MCROCFLObjectIDPrefixHelper.fromDerivateObjectId(objectId);
        // purge
        if (MCROCFLDeleteUtils.checkPurgeDerivate(MCRObjectID.getInstance(owner))) {
            repository.purgeObject(objectId);
            return;
        }
        // delete
        repository.updateObject(targetVersionId, new VersionInfo().setMessage(MESSAGE_DELETED), (updater) -> {
            List<MCRVersionedPath> keepFiles = directoryTracker.originalPathsWithKeepFile();
            List<MCRVersionedPath> contentFiles = fileTracker.originalPaths();
            Stream.concat(keepFiles.stream(), contentFiles.stream())
                .map(path -> toOcflFilesPath(path.toRelativePath()))
                .forEach(updater::removeFile);
        });
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
                    Path localSourcePath = toPhysicalPath(change.source());
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
        List<MCROCFLDirectoryTracker.Change> changes = this.directoryTracker.changes();
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
        boolean sameOwner = Objects.equals(pathOwner, owner);
        boolean sameVersion = Objects.equals(pathVersion, version);
        if (sameOwner && sameVersion) {
            return versionedPath;
        }
        if (!sameOwner) {
            throw new MCROCFLVersionMismatchException(
                "Expected owner '" + owner + "' but got '" + pathOwner + "'. Cannot use path '"
                    + versionedPath + "' in virtual object '" + this + "'");
        }
        if (pathVersion != null) {
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
     * This is necessary for the mycore event system. Because the metadata is not yet
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
        this.directoryTracker.update(path.getParent(), false);
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
        this.directoryTracker.update(path, directoryListing.isEmpty());
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
     * Returns a deep clone of this virtual object. The clone shares the same repository and storage
     * configurations but has its own independent state trackers. This allows it to be modified
     * without affecting the original object from which it was cloned.
     *
     * @param readonly If {@code true}, the cloned object will be in read-only mode.
     * @return A new, independent instance of the virtual object.
     */
    public abstract MCROCFLVirtualObject deepClone(boolean readonly);

    @Override
    public String toString() {
        String version = this.getVersion();
        return this.getOwner() + "@" + (version == null ? "head" : version);
    }

    protected static class DigestCalculator implements MCROCFLDigestCalculator<MCRVersionedPath, MCRDigest> {

        private final MCROCFLVirtualObject virtualObject;

        public DigestCalculator(MCROCFLVirtualObject virtualObject) {
            this.virtualObject = virtualObject;
        }

        @Override
        public MCRDigest calculate(byte[] bytes) throws IOException {
            return virtualObject.digestCalculator.calculate(bytes);
        }

        @Override
        public MCRDigest calculate(MCRVersionedPath path) throws IOException {
            if (virtualObject.isDirectory(path)) {
                return null;
            }
            Path physicalPath = virtualObject.toPhysicalPath(path);
            return virtualObject.digestCalculator.calculate(physicalPath);
        }
    }

    /**
     * Exception thrown when a requested {@code MCRVersionedPath} cannot be resolved to a physical path
     * <p>
     * This typically occurs when attempting to access content that is only available remotely and has not
     * been cached or materialized locally.
     * <p>
     * Note that this exception indicates a repository-level constraint, not a general I/O failure.
     */
    protected static class CannotDeterminePhysicalPathException extends MCROCFLException {

        @Serial
        private static final long serialVersionUID = 1L;

        public CannotDeterminePhysicalPathException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
