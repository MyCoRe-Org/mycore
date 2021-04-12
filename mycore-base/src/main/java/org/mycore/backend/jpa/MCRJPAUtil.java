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

package org.mycore.backend.jpa;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

public class MCRJPAUtil {

    private static void beginTransaction(EntityManager em) {
        em.getTransaction().begin();
    }

    private static EntityTransaction getCurrentTransaction() {
        return MCREntityManagerProvider.getCurrentEntityManager().getTransaction();
    }

    /**
     * starts a new database transaction.
     */
    public static void beginTransaction() {
        MCREntityManagerProvider.getEnabledEntityManager().ifPresent(MCRJPAUtil::beginTransaction);

    }

    /**
     * Determine whether the current resource transaction has been marked for rollback.
     * @return boolean indicating whether the transaction has been marked for rollback
     */
    public static boolean transactionRequiresRollback() {
        return isTransactionActive() && getCurrentTransaction().getRollbackOnly();
    }

    /**
     * commits the database transaction. Commit is only done if {@link #isTransactionActive()} returns true.
     */
    public static void commitTransaction() {
        if (isTransactionActive()) {
            getCurrentTransaction().commit();
        }
    }

    /**
     * forces the database transaction to roll back. Roll back is only performed if {@link #isTransactionActive()}
     * returns true.
     */
    public static void rollbackTransaction() {
        if (isTransactionActive()) {
            getCurrentTransaction().rollback();
        }
    }

    /**
     * Is the transaction still alive?
     *
     * @return true if the transaction is still alive
     */
    public static boolean isTransactionActive() {
        Optional<EntityManager> emOptional = MCREntityManagerProvider.getEnabledEntityManager();
        return emOptional.isPresent() && emOptional.get().getTransaction().isActive();
    }

}
