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

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.util.concurrent.MCRPool;

public class MCRTransactionHelper {

    private static final MCRPool<ServiceLoader<MCRPersistenceTransaction>> SERVICE_LOADER_POOL = new MCRPool<>(
        Runtime.getRuntime().availableProcessors(), () -> ServiceLoader
            .load(MCRPersistenceTransaction.class, MCRClassTools.getClassLoader()));

    private static final ThreadLocal<List<MCRPersistenceTransaction>> TRANSACTION = ThreadLocal
        .withInitial(MCRTransactionHelper::getPersistenceTransactions);

    /**
     * performs a safe operation on the serviceLoader backed by an internal ServiceLoader pool
     * @param f function that runs with the service loader
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
            .collect(Collectors.toUnmodifiableList()), List.of());
    }

    public static boolean isDatabaseAccessEnabled() {
        return MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true)
            && applyServiceLoader(sl -> sl.stream().findAny().isPresent(), false); //impl present
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
     * Mark the current resource transaction so that the only possible outcome of the transaction is for the
     * transaction to be rolled back.
     */
    public static void setRollbackOnly() {
        if (isTransactionActive()) {
            TRANSACTION.get().forEach(MCRPersistenceTransaction::setRollbackOnly);
        }
    }

    /**
     * Is the transaction still alive?
     *
     * @return true if the transaction is still alive
     */
    public static boolean isTransactionActive() {
        return isDatabaseAccessEnabled() && TRANSACTION.get().stream().anyMatch(MCRPersistenceTransaction::isActive);
    }

    /**
     * Determine whether the current resource transaction has been marked for rollback.
     * @return boolean indicating whether the transaction has been marked for rollback
     */
    public static boolean transactionRequiresRollback() {
        return isTransactionActive() && TRANSACTION.get().stream().anyMatch(MCRPersistenceTransaction::getRollbackOnly);
    }

    /**
     * starts a new database transaction.
     */
    public static void beginTransaction() {
        if (isDatabaseAccessEnabled()) {
            TRANSACTION.get().forEach(MCRPersistenceTransaction::begin);
        }
    }
}
