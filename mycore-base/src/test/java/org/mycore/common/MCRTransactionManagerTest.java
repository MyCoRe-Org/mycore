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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MCRTransactionManagerTest extends MCRTestCase {

    MCRTransactionManager.TransactionLoader originalTransactionLoader;

    @Test
    public void beginTransactions() {
        // check calling begin transaction multiple times
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));
        assertThrows("beginning two times should fail", MCRTransactionException.class,
            MCRTransactionManager::beginTransactions);
        assertTrue("WorkingTransaction should still be active",
            MCRTransactionManager.isActive(WorkingTransaction.class));
        MCRTransactionManager.commitTransactions();

        // check fail on begin
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, FailOnBeginTransaction.class));
        assertThrows("Should fail because FailOnBeginTransaction is included", MCRTransactionException.class,
            MCRTransactionManager::beginTransactions);
        assertFalse("WorkingTransaction should not be active",
            MCRTransactionManager.isActive(WorkingTransaction.class));
    }

    @Test
    public void beginTransactionsWithSpecificClasses() {
        // check calling begin transaction multiple times
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class));
        MCRTransactionManager.beginTransactions(WorkingTransaction.class);
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));
        assertThrows("beginning two times should fail", MCRTransactionException.class,
            () -> MCRTransactionManager.beginTransactions(WorkingTransaction.class));
        assertTrue("WorkingTransaction should still be active",
            MCRTransactionManager.isActive(WorkingTransaction.class));
        MCRTransactionManager.commitTransactions();

        // check fail on begin
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, FailOnBeginTransaction.class));
        assertThrows("Should fail because FailOnBeginTransaction is included", MCRTransactionException.class,
            () -> MCRTransactionManager.beginTransactions(WorkingTransaction.class, FailOnBeginTransaction.class));
        assertFalse("WorkingTransaction should not be active",
            MCRTransactionManager.isActive(WorkingTransaction.class));
    }

    @Test
    public void requireTransactions() {
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class));

        // require WorkingTransaction only
        MCRTransactionManager.requireTransactions();
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));

        // require WorkingTransaction only again
        MCRTransactionManager.requireTransactions();
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));

        // add FailOnBeginTransaction
        MCRTransactionManager.setTransactionLoader(
            new TransactionLoaderMock(WorkingTransaction.class, FailOnBeginTransaction.class));

        // require WorkingTransaction and FailOnBeginTransaction
        assertThrows("Should fail because FailOnBeginTransaction fails on begin",
            MCRTransactionException.class, MCRTransactionManager::requireTransactions);
        assertFalse("WorkingTransaction should not be active after rollback",
            MCRTransactionManager.isActive(WorkingTransaction.class));
    }

    @Test
    public void requireTransactionsWithSpecificClasses() {
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, FailOnBeginTransaction.class));

        // require WorkingTransaction only
        MCRTransactionManager.requireTransactions(WorkingTransaction.class);
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));

        // require WorkingTransaction only again
        MCRTransactionManager.requireTransactions(WorkingTransaction.class);
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));

        // require WorkingTransaction and FailOnBeginTransaction
        assertThrows("Should fail because FailOnBeginTransaction fails on begin",
            MCRTransactionException.class,
            () -> MCRTransactionManager.requireTransactions(WorkingTransaction.class, FailOnBeginTransaction.class));
        assertFalse("WorkingTransaction should not be active after rollback",
            MCRTransactionManager.isActive(WorkingTransaction.class));
    }

    @Test
    public void rollbackTransactions() {
        // rollback single transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));
        MCRTransactionManager.rollbackTransactions();
        assertFalse("WorkingTransaction should not be active",
            MCRTransactionManager.isActive(WorkingTransaction.class));

        // rollback error
        MCRTransactionManager
            .setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class, FailOnRollbackTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));
        assertTrue("FailOnRollbackTransaction should be active",
            MCRTransactionManager.isActive(FailOnRollbackTransaction.class));
        MCRTransactionException exception = assertThrows("Rollback should fail due to FailOnRollbackTransaction",
            MCRTransactionException.class, MCRTransactionManager::rollbackTransactions);
        assertFalse("WorkingTransaction should not be active after rollback",
            MCRTransactionManager.isActive(WorkingTransaction.class));
        assertFalse("FailOnRollbackTransaction should not be active after rollback",
            MCRTransactionManager.isActive(FailOnRollbackTransaction.class));

        // check suppressed exceptions
        Throwable[] suppressed = exception.getSuppressed();
        assertTrue("There should be a suppressed exception", suppressed.length > 0);
    }

    @Test
    public void rollbackTransactionsWithSpecificClasses() {
        // rollback single transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));
        MCRTransactionManager.rollbackTransactions(WorkingTransaction.class);
        assertFalse("WorkingTransaction should not be active",
            MCRTransactionManager.isActive(WorkingTransaction.class));

        // rollback error
        MCRTransactionManager
            .setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class, FailOnRollbackTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertTrue("WorkingTransaction should be active", MCRTransactionManager.isActive(WorkingTransaction.class));
        assertTrue("FailOnRollbackTransaction should be active",
            MCRTransactionManager.isActive(FailOnRollbackTransaction.class));
        MCRTransactionException exception = assertThrows("Rollback should fail due to FailOnRollbackTransaction",
            MCRTransactionException.class, () -> MCRTransactionManager.rollbackTransactions(WorkingTransaction.class,
                FailOnRollbackTransaction.class));
        assertFalse("WorkingTransaction should not be active after rollback",
            MCRTransactionManager.isActive(WorkingTransaction.class));
        assertFalse("FailOnRollbackTransaction should not be active after rollback",
            MCRTransactionManager.isActive(FailOnRollbackTransaction.class));

        // check suppressed exceptions
        Throwable[] suppressed = exception.getSuppressed();
        assertTrue("There should be a suppressed exception", suppressed.length > 0);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Ensure that any active transactions are cleared before each test
        MCRTransactionManager.rollbackTransactions();
        // Store the original transaction loader to restore it after the test
        originalTransactionLoader = MCRTransactionManager.getTransactionLoader();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        MCRTransactionManager.setTransactionLoader(originalTransactionLoader);
        super.tearDown();
    }

    private static class TransactionLoaderMock implements MCRTransactionManager.TransactionLoader {

        private final List<Class<? extends MCRPersistenceTransaction>> transactionClasses;

        @SafeVarargs
        public TransactionLoaderMock(Class<? extends MCRPersistenceTransaction>... transactionClasses) {
            this.transactionClasses = Arrays.stream(transactionClasses).toList();
        }

        @Override
        public List<MCRPersistenceTransaction> load() {
            return transactionClasses.stream()
                .map(this::instantiateTransaction)
                .filter(MCRPersistenceTransaction::isReady)
                .toList();
        }

        private MCRPersistenceTransaction instantiateTransaction(Class<? extends MCRPersistenceTransaction> clazz) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate transaction: " + clazz.getName(), e);
            }
        }
    }

    private abstract static class BaseTransaction implements MCRPersistenceTransaction {

        private boolean rollbackOnly = false;
        private boolean active = false;

        @Override
        public void begin() {
            active = true;
        }

        @Override
        public void commit() {
            active = false;
        }

        @Override
        public void rollback() {
            active = false;
        }

        @Override
        public boolean getRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }
    }

    private static class WorkingTransaction extends BaseTransaction {
    }

    private static class NotReadyTransaction extends BaseTransaction {
        @Override
        public boolean isReady() {
            return false;
        }
    }

    private static class FailOnBeginTransaction extends BaseTransaction {
        @Override
        public void begin() {
            throw new MCRException("fail on begin");
        }
    }

    private static class FailOnCommitTransaction extends BaseTransaction {
        @Override
        public void commit() {
            throw new MCRException("fail on commit");
        }
    }

    private static class FailOnRollbackTransaction extends BaseTransaction {
        @Override
        public void rollback() {
            throw new MCRException("fail on rollback");
        }
    }

}
