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
import java.io.UncheckedIOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRTransactionManager;

/**
 * Manages transactions for the OCFL (Oxford Common File Layout) file system within MyCoRe,
 * implementing {@link MCRPersistenceTransaction}. This class coordinates the lifecycle of a transaction,
 * including beginning, committing, and rolling back changes, ensuring data consistency in the OCFL file system.
 * <p>
 * Each transaction is uniquely identified by an ID, tracked in a thread-local variable for
 * thread-safe operations.
 */
public class MCROCFLFileSystemTransaction implements MCRPersistenceTransaction {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final AtomicLong TRANSACTION_COUNTER = new AtomicLong(0);

    private static final ThreadLocal<Long> TRANSACTION_ID = new ThreadLocal<>();

    /**
     * Gets the ID of the transaction.
     *
     * @return the transaction ID, or {@code null} if the transaction is not active.
     */
    public Long getId() {
        return TRANSACTION_ID.get();
    }

    /**
     * Checks if the OCFL file system is supported.
     *
     * @return {@code true} if the OCFL file system is supported, {@code false} otherwise.
     */
    @Override
    public boolean isReady() {
        try {
            MCROCFLFileSystemProvider.getMCROCFLFileSystem();
            return true;
        } catch (FileSystemNotFoundException fileSystemNotFoundException) {
            LOGGER.error("Cannot create OCFL transaction. FileSystem not found.", fileSystemNotFoundException);
            return false;
        }
    }

    /**
     * Begins the transaction.
     *
     * @throws IllegalStateException if the transaction is already active or marked for rollback.
     */
    @Override
    public void begin() {
        TRANSACTION_ID.set(TRANSACTION_COUNTER.getAndIncrement());
    }

    /**
     * Commits the transaction, persisting changes to the OCFL file system.
     *
     * @throws IllegalStateException if the transaction is not active or marked for rollback.
     */
    @Override
    public void commit() {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        try {
            Long transactionId = getTransactionId();
            Collection<MCROCFLVirtualObject> virtualObjects = virtualObjectProvider.collect(transactionId);
            if (virtualObjects.isEmpty()) {
                return;
            }
            List<MCROCFLVirtualObject> modifiedObjects = new ArrayList<>();
            for (MCROCFLVirtualObject virtualObject : virtualObjects) {
                try {
                    if (virtualObject.persist()) {
                        modifiedObjects.add(virtualObject);
                    }
                } catch (IOException ioException) {
                    throw new UncheckedIOException("Unable to persist '" + virtualObject + "'.", ioException);
                }
            }
            for (MCROCFLVirtualObject modifiedObject : modifiedObjects) {
                virtualObjectProvider.invalidate(modifiedObject.getOwner(), modifiedObject.getVersion());
            }
        } finally {
            clean();
        }
    }

    /**
     * Rolls back the transaction, discarding changes.
     *
     * @throws IllegalStateException if the transaction is not active.
     */
    @Override
    public void rollback() {
        clean();
    }

    /**
     * Cleans up the transaction, removing it from the virtual object provider and purging local storage.
     */
    private void clean() {
        Long transactionId = getTransactionId();

        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        virtualObjectProvider.remove(transactionId);
        TRANSACTION_ID.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCommitPriority() {
        return 5000;
    }

    /**
     * Retrieves the current transaction ID, if any.
     *
     * @return the transaction ID, or {@code null} if no transaction is active.
     */
    public static Long getTransactionId() {
        return TRANSACTION_ID.get();
    }

    /**
     * Checks if a transaction is currently active.
     *
     * @return {@code true} if a transaction is active, {@code false} otherwise.
     */
    public static boolean isActive() {
        return MCRTransactionManager.isActive(MCROCFLFileSystemTransaction.class);
    }

    /**
     * Resets the transaction counter.
     * <p>This should only be used in unit testing.</p>
     */
    public static void resetTransactionCounter() {
        TRANSACTION_COUNTER.set(0);
    }

}
