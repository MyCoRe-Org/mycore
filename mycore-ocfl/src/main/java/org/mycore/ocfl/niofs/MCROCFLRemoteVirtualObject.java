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
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.mycore.common.digest.MCRDigest;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.channels.MCROCFLCachingSeekableByteChannel;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;
import org.mycore.ocfl.niofs.channels.MCROCFLReadableByteChannel;
import org.mycore.ocfl.niofs.storage.MCROCFLDefaultRemoteTemporaryStorage;
import org.mycore.ocfl.niofs.storage.MCROCFLRemoteTemporaryStorage;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.OcflObjectVersionFile;

/**
 * Represents a virtual object whose content is stored in a remote OCFL repository (e.g., S3).
 * <p>
 * This class extends {@link MCROCFLVirtualObject} and specializes its behavior to handle the latency and
 * access patterns of remote storage. It orchestrates file access across three distinct storage layers:
 * <ol>
 *   <li>
 *       <b>Remote OCFL Repository:</b> The primary, persistent source of truth. Direct access is network-bound and
 *       slow.
 *   </li>
 *   <li>
 *       <b>Local Temporary Storage ({@link MCROCFLRemoteTemporaryStorage}):</b> A content-addressable cache on the
 *       local disk that stores files downloaded from the remote repository. This avoids repeated downloads of the same
 *       content.
 *   </li>
 *   <li>
 *       <b>Transactional Storage ({@link MCROCFLTransactionalStorage}):</b> A staging area for files that are being
 *       modified within a transaction. Files are copied here from the remote cache or repository before modification.
 *   </li>
 * </ol>
 * <p>
 * Read operations prioritize these layers in reverse order: first checking the transactional storage, then the remote
 * cache, and finally fetching from the remote repository if necessary. When a file is read from the remote repository
 * for the first time, it is streamed into the remote cache for future access.
 *
 * @see MCROCFLVirtualObject
 * @see MCROCFLRemoteTemporaryStorage
 * @see MCROCFLTransactionalStorage
 */
public class MCROCFLRemoteVirtualObject extends MCROCFLVirtualObject {

    private final MCROCFLRemoteTemporaryStorage remoteStorage;

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject} for an object that does not yet exist in the repository
     * or for which the version is not yet loaded.
     *
     * @param repository           the OCFL repository.
     * @param objectVersionId      the versioned ID of the object.
     * @param transactionalStorage the local temporary file storage for modifications.
     * @param remoteStorage        the local cache for remote files.
     * @param digestCalculator     how to calculate the digest.
     * @param readonly             whether the object is read-only.
     */
    public MCROCFLRemoteVirtualObject(MCROCFLRepository repository, ObjectVersionId objectVersionId,
        MCROCFLTransactionalStorage transactionalStorage, MCROCFLRemoteTemporaryStorage remoteStorage,
        MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator, boolean readonly) {
        super(repository, objectVersionId, transactionalStorage, digestCalculator, readonly);
        this.remoteStorage = remoteStorage;
    }

    /**
     * Constructs a new {@code MCROCFLRemoteVirtualObject} with a preloaded OCFL object version.
     *
     * @param repository           the OCFL repository.
     * @param objectVersion        the OCFL object version.
     * @param transactionalStorage the local temporary file storage for modifications.
     * @param remoteStorage        the local cache for remote files.
     * @param digestCalculator     how to calculate the digest.
     * @param readonly             whether the object is read-only.
     */
    public MCROCFLRemoteVirtualObject(MCROCFLRepository repository, OcflObjectVersion objectVersion,
        MCROCFLTransactionalStorage transactionalStorage, MCROCFLRemoteTemporaryStorage remoteStorage,
        MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator, boolean readonly) {
        super(repository, objectVersion, transactionalStorage, digestCalculator, readonly);
        this.remoteStorage = remoteStorage;
    }

    /**
     * Internal constructor used for cloning the virtual object.
     *
     * @param repository           the OCFL repository.
     * @param versionId            the versioned ID of the object.
     * @param objectVersion        the OCFL object version.
     * @param transactionalStorage the local temporary file storage.
     * @param remoteStorage        the local cache for remote files.
     * @param readonly             whether the object is read-only.
     * @param digestCalculator     how to calculate the digest.
     * @param fileTracker          the file tracker.
     * @param directoryTracker     the directory tracker.
     */
    protected MCROCFLRemoteVirtualObject(MCROCFLRepository repository, ObjectVersionId versionId,
        OcflObjectVersion objectVersion, MCROCFLTransactionalStorage transactionalStorage,
        MCROCFLRemoteTemporaryStorage remoteStorage, MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator,
        boolean readonly,
        MCROCFLFileTracker<MCRVersionedPath, MCRDigest> fileTracker,
        MCROCFLDirectoryTracker directoryTracker) {
        super(repository, versionId, objectVersion, transactionalStorage, digestCalculator, readonly, fileTracker,
            directoryTracker);
        this.remoteStorage = remoteStorage;
    }

    /**
     * Provides a {@link SeekableByteChannel} for reading a file.
     * The implementation prioritizes data sources as follows:
     * <ol>
     *   <li>
     *       If the file is modified in the current transaction, it is read from {@link MCROCFLTransactionalStorage}.
     *   </li>
     *   <li>
     *       If the file exists in the {@link MCROCFLRemoteTemporaryStorage} (local cache), it is read from there.
     *   </li>
     *   <li>
     *       Otherwise, it is streamed from the remote OCFL repository. As it is streamed, the content is simultaneously
     *      written to the remote temporary cache for subsequent reads.<
     *   /li>
     * </ol>
     *
     * @param path    the path to the file.
     * @param options the options specifying how the file is opened (must be for reading).
     * @return a new seekable byte channel.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected SeekableByteChannel readByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options)
        throws IOException {

        // read from transactional storage
        if (MCROCFLFileSystemTransaction.isActive() && transactionalStorage.exists(path)) {
            return transactionalStorage.newByteChannel(path, options);
        }

        // read from temporary storage
        MCRDigest digest = getDigest(path);
        if (digest != null && this.remoteStorage.exists(digest)) {
            return this.remoteStorage.readByteChannel(digest);
        }

        // read from remote ocfl repository
        OcflObjectVersionFile ocflFile = fromOcfl(path);
        MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter cacheEntry = this.remoteStorage.newCacheEntry(path);
        MCROCFLReadableByteChannel readableByteChannel = new MCROCFLReadableByteChannel(ocflFile);
        MCROCFLCachingSeekableByteChannel cachingByteChannel =
            new MCROCFLCachingSeekableByteChannel(readableByteChannel, cacheEntry.getChannel());
        return new MCROCFLClosableCallbackChannel(cachingByteChannel, () -> {
            if (cachingByteChannel.isFileComplete()) {
                cacheEntry.commit();
            } else {
                cacheEntry.abort();
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * For remote objects, this method ensures that if a file is being modified (e.g., for an append operation),
     * its current content is first copied from the remote cache to the transactional storage area.
     */
    @Override
    protected SeekableByteChannel writeByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options)
        throws IOException {
        // this part is executed if:
        // * the file is not yet modified in the current transaction
        // * the file is not truncated
        // * the file is already downloaded in the temp storage
        boolean existsInTransactionalStorage = transactionalStorage.exists(path);
        if (!existsInTransactionalStorage && !options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
            MCRDigest digest = getDigest(path);
            if (this.remoteStorage.exists(digest)) {
                // copy from remote store to transactional store
                try (SeekableByteChannel byteChannel = this.remoteStorage.readByteChannel(digest)) {
                    this.transactionalStorage.copy(Channels.newInputStream(byteChannel), path);
                }
            }
        }
        // default write
        return super.writeByteChannel(path, options);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation will first check if the source file is in the transactional or remote cache storage
     * to avoid re-downloading it from the remote repository.
     */
    @Override
    public void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCRVersionedPath lockedSource = lockVersion(source);
        MCRVersionedPath lockedTarget = lockVersion(target);
        checkPurged(lockedSource);
        checkReadOnly();
        boolean targetExists = exists(lockedTarget);
        MCREvent.EventType event = targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE;

        // copy from transactional storage
        if (transactionalStorage.exists(lockedSource)) {
            this.transactionalStorage.copy(lockedSource, lockedTarget, options);
            trackFileWrite(lockedTarget, event);
            return;
        }

        // copy from temporary storage
        MCRDigest digest = getDigest(source);
        if (this.remoteStorage.exists(digest)) {
            this.remoteStorage.copy(digest, lockedTarget, options);
            trackFileWrite(lockedTarget, event);
            return;
        }

        // create a local copy from ocfl -> TODO should we stream this instead of making a local copy?
        localTransactionalCopy(lockedSource);
        this.transactionalStorage.copy(lockedSource, lockedTarget, options);
        trackFileWrite(lockedTarget, event);
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
        MCREvent.EventType event = targetExists ? MCREvent.EventType.UPDATE : MCREvent.EventType.CREATE;

        // copy from transactional storage
        if (transactionalStorage.exists(lockedSource)) {
            this.transactionalStorage.copy(lockedSource, target, options);
            virtualTarget.trackFileWrite(target, event);
            return;
        }

        // copy from temporary storage
        MCRDigest digest = getDigest(source);
        if (this.remoteStorage.exists(digest)) {
            this.remoteStorage.copy(digest, target, options);
            virtualTarget.trackFileWrite(target, event);
            return;
        }

        // create a local copy from ocfl -> TODO should we stream this instead of making a local copy?
        localTransactionalCopy(lockedSource);
        this.transactionalStorage.copy(lockedSource, target, options);
        virtualTarget.trackFileWrite(target, event);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For a remote object, a physical path can only be determined if the file is present in the
     * transactional storage (i.e., it is currently being modified). Otherwise, this will fail as the
     * content only exists remotely.
     */
    @Override
    protected Path toPhysicalPath(MCRVersionedPath path) {
        MCRVersionedPath lockedPath = lockVersion(path);
        try {
            return this.transactionalStorage.toPhysicalPath(lockedPath);
        } catch (Exception e) {
            throw new CannotDeterminePhysicalPathException("Unable to create physical path for " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * For remote repositories, this will always return {@code null} as there is no stable underlying
     * file system inode or key to expose.
     */
    @Override
    public Object getFileKey(MCRVersionedPath path) {
        return null;
    }

    /**
     * {@inheritDoc}
     * If the file is not available locally in either the transactional or remote cache, this implementation
     * falls back to guessing the content type from the file name to avoid downloading the entire file.
     * </p>
     */
    @Override
    public String probeContentType(MCRVersionedPath path) throws IOException {
        MCRVersionedPath lockedPath = lockVersion(path);
        checkExists(lockedPath);

        // probe from transactional storage
        if (MCROCFLFileSystemTransaction.isActive() && this.transactionalStorage.exists(lockedPath)) {
            return this.transactionalStorage.probeContentType(lockedPath);
        }

        // probe from temp storage
        MCRDigest digest = getDigest(path);
        if (this.remoteStorage.exists(digest)) {
            return this.remoteStorage.probeContentType(digest);
        }

        // guess content type by name
        return URLConnection.guessContentTypeFromName(lockedPath.getFileName().toString());
    }

    /**
     * Creates a deep clone of this remote virtual object. The clone will share the same storage
     * instances but will have its own independent tracking state.
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
            this.remoteStorage,
            this.digestCalculator,
            readonly,
            this.fileTracker.deepClone(),
            this.directoryTracker.deepClone());
        clonedVirtualObject.fileTracker.setDigestCalculator(clonedVirtualObject::calculateDigest);
        return clonedVirtualObject;
    }

}
