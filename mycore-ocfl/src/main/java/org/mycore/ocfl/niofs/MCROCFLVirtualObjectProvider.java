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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalTempFileStorage;
import org.mycore.ocfl.repository.MCROCFLRepository;

import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

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

    private static final int READ_MAP_MAX_SIZE = 1000;

    private final Map<ObjectVersionId, MCROCFLVirtualObject> readMap;

    private final Queue<ObjectVersionId> readQueue;

    private final Map<MCROCFLFileSystemTransaction, Map<ObjectVersionId, MCROCFLVirtualObject>> writeMap;

    private final MCROCFLRepository repository;

    private final MCROCFLTransactionalTempFileStorage localStorage;

    private final boolean remote;

    /**
     * Constructs a new {@code MCROCFLVirtualObjectProvider}.
     *
     * @param repository the OCFL repository.
     * @param localStorage the local temporary file storage.
     * @param remote whether the provider handles remote storage.
     */
    public MCROCFLVirtualObjectProvider(MCROCFLRepository repository,
        MCROCFLTransactionalTempFileStorage localStorage, boolean remote) {
        this.repository = repository;
        this.localStorage = localStorage;
        this.remote = remote;
        this.readMap = new ConcurrentHashMap<>();
        this.readQueue = new ConcurrentLinkedDeque<>();
        this.writeMap = new ConcurrentHashMap<>();
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
        return get(getId(owner, version));
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
        return getWritable(getId(owner, version));
    }

    private MCROCFLVirtualObject get(ObjectVersionId id) throws NotFoundException {
        MCROCFLFileSystemTransaction transaction = MCROCFLFileSystemTransaction.get();
        boolean isHeadVersion = MCROCFLFileSystemProvider.get().isHeadVersion(id);
        if (transaction == null || !transaction.isActive() || !isHeadVersion) {
            return getOrCreateReadable(id);
        }
        return getWritable(id);
    }

    private MCROCFLVirtualObject getWritable(ObjectVersionId id) {
        MCROCFLFileSystemTransaction transaction = MCROCFLFileSystemTransaction.getActive();
        return writeMap
            .computeIfAbsent(transaction, (key) -> new HashMap<>())
            .computeIfAbsent(id, (key) -> {
                try {
                    MCROCFLVirtualObject readableVirtualObject = getOrCreateReadable(id);
                    return readableVirtualObject.deepClone(false);
                } catch (NotFoundException ignore) {
                    return remote ? new MCROCFLRemoteVirtualObject(repository, id, localStorage, false)
                        : new MCROCFLLocalVirtualObject(repository, id, localStorage, false);
                }
            });
    }

    /**
     * Checks if the specified owner exists in the repository.
     *
     * @param owner the owner of the object.
     * @return {@code true} if the owner exists, {@code false} otherwise.
     */
    public boolean exists(String owner) {
        ObjectVersionId head = ObjectVersionId.head(owner);
        if (this.readMap.containsKey(head)) {
            return true;
        }
        MCROCFLFileSystemTransaction transaction = MCROCFLFileSystemTransaction.get();
        if (transaction.isActive() && this.writeMap.containsKey(transaction)) {
            MCROCFLVirtualObject headVirtualObject = this.writeMap.get(transaction).get(head);
            if (headVirtualObject != null) {
                return headVirtualObject.isMarkedForCreate();
            }
        }
        return repository.containsObject(owner);
    }

    /**
     * Returns whether this provider handles a remote OCFL repository.
     *
     * @return {@code true} if the provider handles a remote OCFL repository, {@code false} otherwise.
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * Collects all virtual objects associated with the specified transaction.
     *
     * @param transaction the file system transaction.
     * @return a collection of virtual objects.
     */
    public Collection<MCROCFLVirtualObject> collect(MCROCFLFileSystemTransaction transaction) {
        if (!this.writeMap.containsKey(transaction)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(this.writeMap.get(transaction).values());
    }

    /**
     * Removes all virtual objects associated with the specified transaction.
     *
     * @param transaction the file system transaction.
     */
    public void remove(MCROCFLFileSystemTransaction transaction) {
        this.writeMap.remove(transaction);
    }

    /**
     * Invalidates the cache for the specified owner and version.
     *
     * @param owner the owner of the object.
     * @param version the version of the object.
     */
    public void invalidate(String owner, String version) {
        ObjectVersionId objectVersionId = ObjectVersionId.version(owner, version);
        this.readMap.remove(objectVersionId);
        this.readQueue.remove(objectVersionId);
    }

    private ObjectVersionId getId(String owner, String version) {
        Objects.requireNonNull(owner);
        if (version != null) {
            return ObjectVersionId.version(owner, version);
        }
        String headVersion = MCROCFLFileSystemProvider.get().getHeadVersion(owner);
        return ObjectVersionId.version(owner, headVersion);
    }

    private MCROCFLVirtualObject getOrCreateReadable(ObjectVersionId id) {
        return this.readMap.computeIfAbsent(id, (key) -> {
            updateReadCache(id);
            OcflObjectVersion object = repository.getObject(id);
            return remote ? new MCROCFLRemoteVirtualObject(repository, object, localStorage, true)
                : new MCROCFLLocalVirtualObject(repository, object, localStorage, true);
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
        MCROCFLFileSystemTransaction transaction = MCROCFLFileSystemTransaction.get();
        if (!transaction.isActive()) {
            return Collections.emptyList();
        }
        Map<ObjectVersionId, MCROCFLVirtualObject> writeableMap = this.writeMap.get(transaction);
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
