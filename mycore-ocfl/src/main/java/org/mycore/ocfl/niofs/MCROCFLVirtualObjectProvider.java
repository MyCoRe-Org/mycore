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

import static org.mycore.ocfl.util.MCROCFLVersionHelper.convertMessageToType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.storage.MCROCFLRemoteTemporaryStorage;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.util.MCROCFLMetadataVersion;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.OcflObjectVersionFile;
import io.ocfl.api.model.VersionDetails;

/**
 * Provides and manages virtual objects.
 * <p>
 * This class is responsible for creating, retrieving, and managing instances of {@link MCROCFLVirtualObject}.
 * It handles both read and write operations, maintaining consistency between the local temporary storage and the
 * OCFL repository. The provider supports transaction management, ensuring that changes are properly tracked and
 * committed.
 * </p>
 */
public class MCROCFLVirtualObjectProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int READ_MAP_MAX_SIZE = 1000;

    private final Map<ObjectVersionId, MCROCFLVirtualObject> readMap;

    private final Queue<ObjectVersionId> readQueue;

    private final Map<Long, Map<ObjectVersionId, MCROCFLVirtualObject>> writeMap;

    private final MCROCFLRepository repository;

    private final MCROCFLTransactionalStorage transactionalStorage;

    private final MCROCFLRemoteTemporaryStorage remoteStorage;

    private final MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator;

    /**
     * Constructs a new {@code MCROCFLVirtualObjectProvider}.
     *
     * @param repository the OCFL repository.
     * @param transactionalStorage the local temporary file storage.
     */
    public MCROCFLVirtualObjectProvider(MCROCFLRepository repository,
        MCROCFLTransactionalStorage transactionalStorage, MCROCFLRemoteTemporaryStorage remoteStorage,
        MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator) {
        this.repository = repository;
        this.transactionalStorage = transactionalStorage;
        this.remoteStorage = remoteStorage;
        this.readMap = new ConcurrentHashMap<>();
        this.readQueue = new ConcurrentLinkedDeque<>();
        this.writeMap = new ConcurrentHashMap<>();
        this.digestCalculator = digestCalculator;
    }

    /**
     * Retrieves a virtual object for the specified path.
     *
     * @param path the versioned path.
     * @return the virtual object.
     * @throws NotFoundException if the object is not found.
     */
    public MCROCFLVirtualObject get(MCRVersionedPath path) throws NotFoundException {
        return get(path.getOwner(), path.getVersion());
    }

    /**
     * Retrieves the head version virtual object for the specified owner.
     *
     * @param owner the owner of the object.
     * @return the virtual object.
     * @throws NotFoundException if the object is not found.
     */
    public MCROCFLVirtualObject get(String owner) throws NotFoundException {
        return get(owner, null);
    }

    /**
     * Retrieves a virtual object for the specified owner and version.
     *
     * @param owner the owner of the object.
     * @param version the version of the object.
     * @return the virtual object.
     * @throws NotFoundException if the object is not found.
     */
    public MCROCFLVirtualObject get(String owner, String version) throws NotFoundException {
        return get(resolveObjectVersionId(owner, version));
    }

    /**
     * Retrieves a writable virtual object for the specified path.
     *
     * @param path the versioned path.
     * @return the writable virtual object.
     */
    public MCROCFLVirtualObject getWritable(MCRVersionedPath path) {
        return getWritable(path.getOwner(), path.getVersion());
    }

    /**
     * Retrieves a writable head version virtual object for the specified owner.
     *
     * @param owner the owner of the object.
     * @return the writable virtual object.
     */
    public MCROCFLVirtualObject getWritable(String owner) {
        return getWritable(owner, null);
    }

    /**
     * Retrieves a writable virtual object for the specified owner and version.
     *
     * @param owner the owner of the object.
     * @param version the version of the object.
     * @return the writable virtual object.
     */
    public MCROCFLVirtualObject getWritable(String owner, String version) {
        return getWritable(resolveObjectVersionId(owner, version));
    }

    private MCROCFLVirtualObject get(ObjectVersionId id) throws NotFoundException {
        boolean isActive = MCROCFLFileSystemTransaction.isActive();
        boolean isHeadVersion = MCROCFLFileSystemProvider.get().isHeadVersion(id);
        if (!isActive || !isHeadVersion) {
            return getOrCreateReadable(id);
        }
        return getWritable(id);
    }

    private MCROCFLVirtualObject getWritable(ObjectVersionId id) {
        boolean isActive = MCROCFLFileSystemTransaction.isActive();
        if (!isActive) {
            throw new MCROCFLInactiveTransactionException("OCFL transaction is not active!");
        }
        Long transactionId = MCROCFLFileSystemTransaction.getTransactionId();
        return writeMap
            .computeIfAbsent(transactionId, (key) -> new HashMap<>())
            .computeIfAbsent(id, (key) -> {
                try {
                    MCROCFLVirtualObject readableVirtualObject = getOrCreateReadable(id);
                    return readableVirtualObject.deepClone(false);
                } catch (NotFoundException ignore) {
                    return repository.isRemote() ? new MCROCFLRemoteVirtualObject(repository, id,
                        transactionalStorage, remoteStorage, digestCalculator, false)
                        : new MCROCFLLocalVirtualObject(repository, id, transactionalStorage, digestCalculator, false);
                }
            });
    }

    /**
     * Checks if the specified owner exists in the repository.
     * <p>
     * This also will return true if the owner was marked for purge or was deleted (and not purged),
     * because the object still exists in the OCFL repository.
     *
     * @param owner the owner of the object.
     * @return {@code true} if the owner exists, {@code false} otherwise.
     */
    public boolean exists(String owner) {
        return exists(owner, null);
    }

    /**
     * Checks if the specified owner and version exists in the repository.
     * <p>
     * This also will return true if the owner was marked for purge or was deleted (and not purged),
     * because the object still exists in the OCFL repository.
     *
     * @param owner the owner of the object.
     * @param version the version of the object
     * @return {@code true} if the owner and version exists, {@code false} otherwise.
     */
    public boolean exists(String owner, String version) {
        ObjectVersionId objectVersionId = toObjectVersionId(owner, version);
        if (this.readMap.containsKey(objectVersionId)) {
            return true;
        }
        boolean isActive = MCROCFLFileSystemTransaction.isActive();
        Long transactionId = MCROCFLFileSystemTransaction.getTransactionId();
        if (isActive && this.writeMap.containsKey(transactionId)) {
            MCROCFLVirtualObject headVirtualObject = this.writeMap.get(transactionId).get(objectVersionId);
            if (headVirtualObject != null) {
                return headVirtualObject.isMarkedForCreate();

            }
        }
        if (!repository.containsObject(objectVersionId.getObjectId())) {
            return false;
        }
        try {
            OcflObjectVersion object = repository.getObject(objectVersionId);
            return object.getFiles().stream()
                .map(OcflObjectVersionFile::getPath)
                .anyMatch(path -> path.startsWith(MCROCFLVirtualObject.FILES_DIRECTORY));
        } catch (NotFoundException ignore) {
            return false;
        }
    }

    /**
     * Returns true if the owner was deleted. In the MyCoRe OCFL API you can either delete or purge an object. Where
     * a 'purge' is permanent and a 'delete' isn't.
     * <p>
     * This method will only return true if the object is actually deleted.
     * Therefore, the last commit message is equal to {@link org.mycore.ocfl.util.MCROCFLMetadataVersion#DELETED}.
     *
     * @param owner the owner of the object.
     * @return {@code true} if the owner was deleted, {@code false} otherwise.
     */
    public boolean isDeleted(String owner) {
        ObjectVersionId head = toObjectVersionId(owner, null);
        try {
            VersionDetails headVersionDetails = repository.describeVersion(head);
            String commitMessage = headVersionDetails.getVersionInfo().getMessage();
            char type = convertMessageToType(commitMessage);
            return type == MCROCFLMetadataVersion.DELETED;
        } catch (NotFoundException ignore) {
            return false;
        }
    }

    /**
     * Collects all virtual objects associated with the specified transactionId.
     *
     * @param transactionId the file system transactionId.
     * @return a collection of virtual objects.
     */
    public Collection<MCROCFLVirtualObject> collect(Long transactionId) {
        if (!this.writeMap.containsKey(transactionId)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(this.writeMap.get(transactionId).values());
    }

    /**
     * Removes all virtual objects associated with the specified transactionId.
     *
     * @param transactionId the file system transactionId.
     */
    public void remove(Long transactionId) {
        this.writeMap.remove(transactionId);
        try {
            this.transactionalStorage.purge(transactionId);
        } catch (IOException ioExc) {
            LOGGER.error(() -> "Unable to clean local storage for transaction '" + transactionId + "'.", ioExc);
        }
    }

    /**
     * Invalidates the cache for the specified owner and version.
     *
     * @param owner the owner of the object.
     * @param version the version of the object.
     */
    public void invalidate(String owner, String version) {
        ObjectVersionId objectVersionId = toObjectVersionId(owner, version);
        this.readMap.remove(objectVersionId);
        this.readQueue.remove(objectVersionId);
    }

    private String toObjectId(String owner) {
        return MCROCFLObjectIDPrefixHelper.toDerivateObjectId(owner);
    }

    private ObjectVersionId toObjectVersionId(String owner, String version) {
        String objectId = toObjectId(owner);
        return ObjectVersionId.version(objectId, version);
    }

    private ObjectVersionId resolveObjectVersionId(String owner, String version) {
        Objects.requireNonNull(owner);
        if (version != null) {
            return toObjectVersionId(owner, version);
        }
        String headVersion = MCROCFLFileSystemProvider.get().getHeadVersion(owner);
        return toObjectVersionId(owner, headVersion);
    }

    private MCROCFLVirtualObject getOrCreateReadable(ObjectVersionId id) throws NotFoundException {
        return this.readMap.computeIfAbsent(id, (key) -> {
            updateReadCache(id);
            OcflObjectVersion object = repository.getObject(id);
            return repository.isRemote()
                ? new MCROCFLRemoteVirtualObject(repository, object, transactionalStorage, remoteStorage,
                    digestCalculator, true)
                : new MCROCFLLocalVirtualObject(repository, object, transactionalStorage, digestCalculator, true);
        });
    }

    private void updateReadCache(ObjectVersionId id) {
        readQueue.remove(id);
        readQueue.offer(id);
        if (readQueue.size() > READ_MAP_MAX_SIZE) {
            int purge10Percent = READ_MAP_MAX_SIZE / 10;
            for (int i = 0; i < purge10Percent; i++) {
                ObjectVersionId polledId = readQueue.poll();
                readMap.remove(polledId);
            }
        }
    }

    /**
     * Collects all writable virtual objects associated with the current transaction.
     *
     * @return a collection of writable virtual objects.
     */
    public Collection<MCROCFLVirtualObject> collectWritables() {
        boolean isActive = MCROCFLFileSystemTransaction.isActive();
        if (!isActive) {
            return Collections.emptyList();
        }
        Long transactionId = MCROCFLFileSystemTransaction.getTransactionId();
        Map<ObjectVersionId, MCROCFLVirtualObject> writeableMap = this.writeMap.get(transactionId);
        if (writeableMap == null) {
            return Collections.emptyList();
        }
        return writeableMap.values();
    }

    /**
     * Clears all caches and maps managed by this provider.
     */
    public void clear() {
        this.readMap.clear();
        this.readQueue.clear();
        this.writeMap.clear();
    }

}
