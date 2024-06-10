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
import java.io.UncheckedIOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalTempFileStorage;

/**
 * Manages transactions for the OCFL file system, implementing {@link MCRPersistenceTransaction}.
 * Provides methods to begin, commit, rollback, and check the status of transactions.
 */
public class MCROCFLFileSystemTransaction implements MCRPersistenceTransaction {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final AtomicInteger TRANSACTION_COUNTER = new AtomicInteger(0);

    private boolean active;

    private boolean rollback;

    private Integer transactionId;

    /**
     * Constructs a new {@code MCROCFLFileSystemTransaction} with initial inactive and non-rollback states.
     */
    public MCROCFLFileSystemTransaction() {
        this.active = false;
        this.rollback = false;
        this.transactionId = null;
    }

    /**
     * Gets the ID of the transaction.
     *
     * @return the transaction ID, or {@code null} if the transaction is not active.
     */
    public Integer getId() {
        return transactionId;
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
        if (this.active) {
            throw new IllegalStateException("OCFL File Transaction is already active.");
        }
        if (this.rollback) {
            throw new IllegalStateException("OCFL File Transaction is already marked to be rolled back.");
        }
        this.transactionId = TRANSACTION_COUNTER.incrementAndGet();
        this.active = true;
    }

    /**
     * Commits the transaction, persisting changes to the OCFL file system.
     *
     * @throws IllegalStateException if the transaction is not active or marked for rollback.
     */
    @Override
    public void commit() {
        if (!this.active) {
            throw new IllegalStateException("OCFL File Transaction is not active.");
        }
        if (this.rollback) {
            throw new IllegalStateException("OCFL File Transaction is already marked to be rolled back.");
        }
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        try {
            Collection<MCROCFLVirtualObject> virtualObjects = virtualObjectProvider.collect(this);
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
            this.active = false;
        }
    }

    /**
     * Rolls back the transaction, discarding changes.
     *
     * @throws IllegalStateException if the transaction is not active.
     */
    @Override
    public void rollback() {
        if (!this.active) {
            throw new IllegalStateException("OCFL File Transaction is not active.");
        }
        clean();
        this.active = false;
    }

    /**
     * Cleans up the transaction, removing it from the virtual object provider and purging local storage.
     */
    private void clean() {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        MCROCFLTransactionalTempFileStorage localStorage = MCROCFLFileSystemProvider.get().localStorage();
        virtualObjectProvider.remove(this);
        try {
            localStorage.purge(this);
        } catch (IOException ioExc) {
            LOGGER.error("Unable to clean local storage for transaction '{}'.", this.transactionId, ioExc);
        }
    }

    /**
     * Checks if the transaction is marked for rollback.
     *
     * @return {@code true} if the transaction is marked for rollback, {@code false} otherwise.
     */
    @Override
    public boolean getRollbackOnly() {
        return rollback;
    }

    /**
     * Marks the transaction for rollback.
     *
     * @throws IllegalStateException if the transaction is not active.
     */
    @Override
    public void setRollbackOnly() throws IllegalStateException {
        this.rollback = true;
    }

    /**
     * Checks if the transaction is active.
     *
     * @return {@code true} if the transaction is active, {@code false} otherwise.
     */
    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Gets the current OCFL file system transaction.
     *
     * @return the current {@link MCROCFLFileSystemTransaction}.
     */
    public static MCROCFLFileSystemTransaction get() {
        return MCRTransactionHelper.get(MCROCFLFileSystemTransaction.class);
    }

    /**
     * Gets the current active OCFL file system transaction.
     *
     * @return the current active {@link MCROCFLFileSystemTransaction}.
     * @throws MCRException if there is no current transaction.
     * @throws MCROCFLInactiveTransactionException if the current transaction is not active.
     */
    public static MCROCFLFileSystemTransaction getActive() {
        MCROCFLFileSystemTransaction transaction = get();
        if (transaction == null) {
            throw new MCRException("Unable to create MCROCFLFileSystemTransaction.");
        }
        if (!transaction.isActive()) {
            throw new MCROCFLInactiveTransactionException("OCFL transaction is not active!");
        }
        return transaction;
    }

    /**
     * Resets the transaction counter.
     * <p>This should only be used in unit testing.</p>
     */
    public static void resetTransactionCounter() {
        TRANSACTION_COUNTER.set(0);
    }

}
