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

package org.mycore.common;

import org.mycore.util.concurrent.MCRPool;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

public abstract class MCRTransactionHelper {

    private static final MCRPool<ServiceLoader<MCRPersistenceTransaction>> SERVICE_LOADER_POOL = new MCRPool<>(
        Runtime.getRuntime().availableProcessors(), () -> ServiceLoader
        .load(MCRPersistenceTransaction.class, MCRClassTools.getClassLoader()));

    private static final ThreadLocal<List<MCRPersistenceTransaction>> TRANSACTION = ThreadLocal
        .withInitial(MCRTransactionHelper::getPersistenceTransactions);

    /**
     * performs a safe operation on the serviceLoader backed by an internal ServiceLoader pool
     *
     * @param f            function that runs with the service loader
     * @param defaultValue a fallback default if a service loader could not be acquired
     * @return result of operation <code>f</code>
     */
    private static <V> V applyServiceLoader(Function<ServiceLoader<MCRPersistenceTransaction>, V> f, V defaultValue) {
        final ServiceLoader<MCRPersistenceTransaction> serviceLoader;
        try {
            serviceLoader = SERVICE_LOADER_POOL.acquire();
        } catch (InterruptedException e) {
            return defaultValue;
        }
        try {
            return f.apply(serviceLoader);
        } finally {
            SERVICE_LOADER_POOL.release(serviceLoader);
        }
    }

    private static List<MCRPersistenceTransaction> getPersistenceTransactions() {
        return applyServiceLoader(sl -> sl.stream()
            .map(ServiceLoader.Provider::get)
            .filter(MCRPersistenceTransaction::isReady)
            .toList(), List.of());
    }

    public static boolean isAccessEnabled() {
        return applyServiceLoader(sl -> sl.stream().findAny().isPresent(), false);
    }

    /**
     * commits the database transaction. Commit is only done if {@link #isTransactionActive()} returns true.
     */
    public static void commitTransaction() {
        if (isTransactionActive()) {
            TRANSACTION.get().forEach(MCRPersistenceTransaction::commit);
            TRANSACTION.remove();
        }
        MCRSessionMgr.getCurrentSession().submitOnCommitTasks();
    }

    /**
     * <p>
     * Commits the given transaction. Commit is only done if {@link #isTransactionActive()} returns true.
     * </p>
     * <p>
     * Be aware, that in difference to {@link #commitTransaction()}, this does not submits the tasks of the current
     * session!
     * </p>
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     */
    public static <T extends MCRPersistenceTransaction> void commitTransaction(Class<T> persistenceClass) {
        if (isTransactionActive()) {
            Optional.ofNullable(get(persistenceClass)).ifPresent(MCRPersistenceTransaction::commit);
        }
    }

    /**
     * Forces the database transaction to roll back. Roll back is only performed if {@link #isTransactionActive()}
     * returns true.
     */
    public static void rollbackTransaction() {
        if (isTransactionActive()) {
            TRANSACTION.get().forEach(MCRPersistenceTransaction::rollback);
            TRANSACTION.remove();
        }
    }

    /**
     * Forces the database transaction to roll back. Roll back is only performed if {@link #isTransactionActive()}
     * returns true.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     */
    public static <T extends MCRPersistenceTransaction> void rollbackTransaction(Class<T> persistenceClass) {
        if (isTransactionActive()) {
            Optional.ofNullable(get(persistenceClass)).ifPresent(MCRPersistenceTransaction::rollback);
        }
    }

    /**
     * Mark the current resource transaction so that the only possible outcome of the transaction is for the
     * transaction to be rolled back.
     */
    public static void setRollbackOnly() {
        if (isTransactionActive()) {
            TRANSACTION.get().forEach(MCRPersistenceTransaction::setRollbackOnly);
        }
    }

    /**
     * Mark the given transaction so that the only possible outcome of the transaction is for the
     * transaction to be rolled back.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     */
    public static <T extends MCRPersistenceTransaction> void setRollbackOnly(Class<T> persistenceClass) {
        if (isAccessEnabled()) {
            Optional.ofNullable(get(persistenceClass)).ifPresent(MCRPersistenceTransaction::setRollbackOnly);
        }
    }

    /**
     * Is the transaction still alive?
     *
     * @return true if the transaction is still alive
     */
    public static boolean isTransactionActive() {
        return isAccessEnabled() && TRANSACTION.get().stream().anyMatch(MCRPersistenceTransaction::isActive);
    }

    /**
     * Is the transaction still alive?
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @return true if the transaction is still alive
     */
    public static <T extends MCRPersistenceTransaction> boolean isTransactionActive(Class<T> persistenceClass) {
        return isAccessEnabled() && Optional.ofNullable(get(persistenceClass))
            .map(MCRPersistenceTransaction::isActive)
            .orElse(false);
    }

    /**
     * Determine whether any one transaction has been marked for rollback.
     *
     * @return boolean indicating whether the transaction has been marked for rollback
     */
    public static boolean transactionRequiresRollback() {
        return isTransactionActive() && TRANSACTION.get().stream().anyMatch(MCRPersistenceTransaction::getRollbackOnly);
    }

    /**
     * Determine whether the given transaction has been marked for rollback.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @return boolean indicating whether the transaction has been marked for rollback
     */
    public static <T extends MCRPersistenceTransaction> boolean transactionRequiresRollback(Class<T> persistenceClass) {
        return isTransactionActive() &&
            Optional.ofNullable(get(persistenceClass))
                .map(MCRPersistenceTransaction::getRollbackOnly)
                .orElse(false);
    }

    /**
     * Starts all transactions.
     */
    public static void beginTransaction() {
        if (isAccessEnabled()) {
            TRANSACTION.get().forEach(MCRPersistenceTransaction::begin);
        }
    }

    /**
     * Starts a new transaction.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @param <T>              The type parameter that extends {@link MCRPersistenceTransaction}, indicating the
     *                         specific type of persistence transaction to begin.
     */
    public static <T extends MCRPersistenceTransaction> void beginTransaction(Class<T> persistenceClass) {
        if (isAccessEnabled()) {
            Optional.ofNullable(get(persistenceClass)).ifPresent(MCRPersistenceTransaction::begin);
        }
    }

    /**
     * Starts a new transaction if it's not already active.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @param <T>              The type parameter that extends {@link MCRPersistenceTransaction}, indicating the
     *                         specific type of persistence transaction to begin.
     */
    public static <T extends MCRPersistenceTransaction> void requireTransaction(Class<T> persistenceClass) {
        if (isAccessEnabled()) {
            Optional.ofNullable(get(persistenceClass))
                .filter(transaction -> !transaction.isActive())
                .ifPresent(MCRPersistenceTransaction::begin);
        }
    }

    /**
     * Retrieves a specific implementation of {@link MCRPersistenceTransaction} based on the provided class type.
     * This method iterates over the list of currently active persistence transactions stored in a thread-local
     * variable.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction} to be
     *                         retrieved. This class object is used to check if any of the active transactions are
     *                         instances of the specified type.
     * @param <T>              The type parameter that extends {@link MCRPersistenceTransaction}, indicating the
     *                         specific type of persistence transaction to be returned.
     * @return An instance of the specified type <T> if found among the active transactions; otherwise, returns
     * {@code null}.
     */
    public static <T extends MCRPersistenceTransaction> T get(Class<T> persistenceClass) {
        for (MCRPersistenceTransaction persistenceTransaction : TRANSACTION.get()) {
            if (persistenceClass.isInstance(persistenceTransaction)) {
                return persistenceClass.cast(persistenceTransaction);
            }
        }
        return null;
    }

}
