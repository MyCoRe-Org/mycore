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

package org.mycore.common;

/**
 * @deprecated This class is deprecated and should not be used for managing transactions.
 * Use {@link MCRTransactionManager} instead.
 */
@Deprecated
public abstract class MCRTransactionHelper {

    /**
     * commits the database transaction. Commit is only done if {@link #isTransactionActive()} returns true.
     */
    public static void commitTransaction() {
        MCRTransactionManager.commitTransactions();
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
        MCRTransactionManager.commitTransactions(persistenceClass);
    }

    /**
     * Forces the database transaction to roll back. Roll back is only performed if {@link #isTransactionActive()}
     * returns true.
     */
    public static void rollbackTransaction() {
        MCRTransactionManager.rollbackTransactions();
    }

    /**
     * Forces the database transaction to roll back. Roll back is only performed if {@link #isTransactionActive()}
     * returns true.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     */
    public static <T extends MCRPersistenceTransaction> void rollbackTransaction(Class<T> persistenceClass) {
        MCRTransactionManager.rollbackTransactions(persistenceClass);
    }

    /**
     * Mark the current resource transaction so that the only possible outcome of the transaction is for the
     * transaction to be rolled back.
     */
    public static void setRollbackOnly() {
        MCRTransactionManager.setRollbackOnly();
    }

    /**
     * Mark the given transaction so that the only possible outcome of the transaction is for the
     * transaction to be rolled back.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     */
    public static <T extends MCRPersistenceTransaction> void setRollbackOnly(Class<T> persistenceClass) {
        MCRTransactionManager.setRollbackOnly(persistenceClass);
    }

    /**
     * Is the transaction still alive?
     *
     * @return true if the transaction is still alive
     */
    public static boolean isTransactionActive() {
        return MCRTransactionManager.hasActiveTransactions();
    }

    /**
     * Is the transaction still alive?
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @return true if the transaction is still alive
     */
    public static <T extends MCRPersistenceTransaction> boolean isTransactionActive(Class<T> persistenceClass) {
        return MCRTransactionManager.isActive(persistenceClass);
    }

    /**
     * Determine whether any one transaction has been marked for rollback.
     *
     * @return boolean indicating whether the transaction has been marked for rollback
     */
    public static boolean transactionRequiresRollback() {
        return MCRTransactionManager.hasRollbackOnlyTransactions();
    }

    /**
     * Determine whether the given transaction has been marked for rollback.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @return boolean indicating whether the transaction has been marked for rollback
     */
    public static <T extends MCRPersistenceTransaction> boolean transactionRequiresRollback(Class<T> persistenceClass) {
        return MCRTransactionManager.isRollbackOnly(persistenceClass);
    }

    /**
     * Starts all transactions.
     */
    public static void beginTransaction() {
        MCRTransactionManager.beginTransactions();
    }

    /**
     * Starts a new transaction.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @param <T>              The type parameter that extends {@link MCRPersistenceTransaction}, indicating the
     *                         specific type of persistence transaction to begin.
     */
    public static <T extends MCRPersistenceTransaction> void beginTransaction(Class<T> persistenceClass) {
        MCRTransactionManager.beginTransactions(persistenceClass);
    }

    /**
     * Starts a new transaction if it's not already active.
     *
     * @param persistenceClass The class object representing the type of {@link MCRPersistenceTransaction}.
     * @param <T>              The type parameter that extends {@link MCRPersistenceTransaction}, indicating the
     *                         specific type of persistence transaction to begin.
     */
    public static <T extends MCRPersistenceTransaction> void requireTransaction(Class<T> persistenceClass) {
        MCRTransactionManager.requireTransactions(persistenceClass);
    }

}
