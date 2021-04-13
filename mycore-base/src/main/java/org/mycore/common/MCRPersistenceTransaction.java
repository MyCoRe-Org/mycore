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

/**
 * Thread safety: Implementation must not ensure that concurrent access is without side effects.
 */
public interface MCRPersistenceTransaction {

    /**
     * preconditions in the backend are met and this instance should be used for transactions
     * @return true if this instance is ready for transaction handling, e.g. underlaying database is configured
     */
    boolean isReady();

    /**
     * Start a transaction.
     * @throws IllegalStateException if <code>isActive()</code> is true
     */
    void begin();

    /**
     * Commit the current transaction, writing any
     * unflushed changes to the backend.
     * @throws IllegalStateException if <code>isActive()</code> is false
     */
    void commit();

    /**
     * Roll back the current transaction.
     * @throws IllegalStateException if <code>isActive()</code> is false
     */
    void rollback();

    /**
     * Mark the current transaction so that the only
     * possible outcome of the transaction is for the transaction 
     * to be rolled back. 
     * @throws IllegalStateException if <code>isActive()</code> is false
     */
    void setRollbackOnly();

    /**
     * Determine whether the current transaction has been
     * marked for rollback.
     * @return boolean indicating whether the transaction has been
     *         marked for rollback
     * @throws IllegalStateException if <code>isActive()</code> is false
     */
    boolean getRollbackOnly();

    /**
     * Indicate whether a transaction is in progress.
     * @return boolean indicating whether transaction is
     *         in progress
     */
    boolean isActive();
}
