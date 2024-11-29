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
     * Returns the commit priority of this transaction.
     *
     * <p>This priority determines the order in which transactions are committed
     * when multiple transactions are active. Transactions with higher priority
     * values will be committed before those with lower values.</p>
     *
     * <p>The commit priority can be useful when managing transactions across
     * multiple backends, ensuring that more critical transactions are finalized
     * first to maintain data consistency and integrity.</p>
     *
     * @return an integer representing the commit priority of the transaction,
     *         where higher values indicate higher priority.
     */
    int getCommitPriority();

}
