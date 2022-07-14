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
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;

public class MCRTransactionHelper implements Cloneable {

    private static final boolean DB_ACCESS_ENABLED = MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable")
        .orElse(true)
        && ServiceLoader
            .load(MCRPersistenceTransaction.class, MCRClassTools.getClassLoader())
            .stream()
            .findAny()
            .isPresent();

    private static ThreadLocal<List<MCRPersistenceTransaction>> transaction = ThreadLocal
        .withInitial(() -> ServiceLoader
            .load(MCRPersistenceTransaction.class, MCRClassTools.getClassLoader())
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(MCRPersistenceTransaction::isReady)
                    .collect(Collectors.toUnmodifiableList()));



    public static boolean isDatabaseAccessEnabled(){
        return DB_ACCESS_ENABLED;
    }

    /**
     * commits the database transaction. Commit is only done if {@link #isTransactionActive()} returns true.
     */
    public static void commitTransaction() {
        if (isTransactionActive()) {
            transaction.get().forEach(MCRPersistenceTransaction::commit);
            transaction.remove();
        }
        MCRSessionMgr.getCurrentSession().submitOnCommitTasks();
    }

    /**
     * forces the database transaction to roll back. Roll back is only performed if {@link #isTransactionActive()}
     * returns true.
     */
    public static void rollbackTransaction() {
        if (isTransactionActive()) {
            transaction.get().forEach(MCRPersistenceTransaction::rollback);
            transaction.remove();
        }
    }

    /**
     * Is the transaction still alive?
     *
     * @return true if the transaction is still alive
     */
    public static boolean isTransactionActive() {
        return isDatabaseAccessEnabled() && transaction.get().stream().anyMatch(MCRPersistenceTransaction::isActive);
    }


    /**
     * Determine whether the current resource transaction has been marked for rollback.
     * @return boolean indicating whether the transaction has been marked for rollback
     */
    public static boolean transactionRequiresRollback() {
        return isTransactionActive() && transaction.get().stream().anyMatch(MCRPersistenceTransaction::getRollbackOnly);
    }


    /**
     * starts a new database transaction.
     */
    public static void beginTransaction() {
        if (isDatabaseAccessEnabled()) {
            transaction.get().forEach(MCRPersistenceTransaction::begin);
        }
    }
}
