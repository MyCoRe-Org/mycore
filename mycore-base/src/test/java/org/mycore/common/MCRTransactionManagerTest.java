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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRTransactionManagerTest {

    MCRTransactionManager.TransactionLoader originalTransactionLoader;

    @Test
    public void beginTransactions() {
        // check calling begin transaction multiple times
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        checkCounter(2, 0, 0);
        assertThrows(MCRTransactionException.class, MCRTransactionManager::beginTransactions,
            "beginning two times should fail");
        checkCounter(2, 0, 0);
        MCRTransactionManager.commitTransactions();
        resetCounter();

        // check fail on begin
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnBeginTransaction.class));
        assertThrows(MCRTransactionException.class, MCRTransactionManager::beginTransactions,
            "Should fail because FailOnBeginTransaction is included");
        checkCounter(2, 0, 2);
    }

    @Test
    public void beginTransactionsWithSpecificClasses() {
        // check calling begin transaction multiple times
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.beginTransactions(WorkingTransaction.class, AnotherWorkingTransaction.class);
        checkCounter(2, 0, 0);
        assertThrows(MCRTransactionException.class,
            () -> MCRTransactionManager.beginTransactions(WorkingTransaction.class),
            "beginning two times should fail");
        checkCounter(2, 0, 0);
        MCRTransactionManager.commitTransactions();
        resetCounter();

        // check fail on begin
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnBeginTransaction.class));
        assertThrows(MCRTransactionException.class,
            () -> MCRTransactionManager.beginTransactions(WorkingTransaction.class, AnotherWorkingTransaction.class,
                FailOnBeginTransaction.class),
            "Should fail because FailOnBeginTransaction is included");
        checkCounter(2, 0, 2);
    }

    @Test
    public void requireTransactions() {
        // check calling require transaction multiple times
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.requireTransactions();
        checkCounter(2, 0, 0);
        MCRTransactionManager.requireTransactions();
        checkCounter(2, 0, 0);
        MCRTransactionManager.commitTransactions();
        resetCounter();

        // check fail on begin
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnBeginTransaction.class));
        assertThrows(MCRTransactionException.class, MCRTransactionManager::requireTransactions,
            "Should fail because FailOnBeginTransaction fails on begin");
        checkCounter(2, 0, 2);
    }

    @Test
    public void requireTransactionsWithSpecificClasses() {
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnBeginTransaction.class));
        // check calling require transaction multiple times
        MCRTransactionManager.requireTransactions(WorkingTransaction.class);
        checkCounter(1, 0, 0);
        MCRTransactionManager.requireTransactions(WorkingTransaction.class, AnotherWorkingTransaction.class);
        checkCounter(2, 0, 0);
        MCRTransactionManager.commitTransactions();
        resetCounter();

        // check fail on begin
        assertThrows(MCRTransactionException.class,
            () -> MCRTransactionManager.requireTransactions(WorkingTransaction.class, AnotherWorkingTransaction.class,
                FailOnBeginTransaction.class),
            "Should fail because FailOnBeginTransaction fails on begin");
        checkCounter(2, 0, 2);
    }

    @Test
    public void commitTransactions() {
        // single transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        MCRTransactionManager.commitTransactions();
        checkCounter(2, 2, 0);
        resetCounter();

        // commit fail transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnCommitTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertThrows(MCRTransactionException.class, MCRTransactionManager::commitTransactions,
            "Should fail because FailOnCommitTransaction fails on commit");
        checkCounter(3, 2, 1);
    }

    @Test
    public void commitTransactionsWithSpecificClasses() {
        // single transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        MCRTransactionManager.commitTransactions(WorkingTransaction.class, AnotherWorkingTransaction.class);
        checkCounter(2, 2, 0);
        resetCounter();

        // commit fail transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnCommitTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertThrows(MCRTransactionException.class, () -> MCRTransactionManager.commitTransactions(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnCommitTransaction.class),
            "Should fail because FailOnCommitTransaction fails on commit");
        checkCounter(3, 2, 1);
    }

    @Test
    public void commitTransactionsWithRollbackOnly() {
        // Set up the transaction loader with two working transactions
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));

        // Begin transactions
        MCRTransactionManager.beginTransactions();
        checkCounter(2, 0, 0);

        // Mark one transaction as rollback-only
        MCRTransactionManager.setRollbackOnly(WorkingTransaction.class);

        // Attempt to commit transactions and expect an exception
        assertThrows(MCRTransactionException.class, MCRTransactionManager::commitTransactions,
            "Commit should fail because WorkingTransaction is marked as rollback-only");

        // Check that no transactions were committed
        checkCounter(2, 0, 2);
    }

    @Test
    public void rollbackTransactions() {
        // rollback single transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        MCRTransactionManager.rollbackTransactions();
        checkCounter(2, 0, 2);
        resetCounter();

        // rollback error
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnRollbackTransaction.class));
        MCRTransactionManager.beginTransactions();
        MCRTransactionException exception = assertThrows(MCRTransactionException.class,
            MCRTransactionManager::rollbackTransactions, "Rollback should fail due to FailOnRollbackTransaction");
        checkCounter(3, 0, 2);

        // check suppressed exceptions
        Throwable[] suppressed = exception.getSuppressed();
        assertTrue(suppressed.length > 0, "There should be a suppressed exception");
    }

    @Test
    public void rollbackTransactionsWithSpecificClasses() {
        // rollback single transaction
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        MCRTransactionManager.rollbackTransactions(WorkingTransaction.class, AnotherWorkingTransaction.class);
        checkCounter(2, 0, 2);
        resetCounter();

        // rollback error
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnRollbackTransaction.class));
        MCRTransactionManager.beginTransactions();
        MCRTransactionException exception = assertThrows(MCRTransactionException.class,
            () -> MCRTransactionManager.rollbackTransactions(
                WorkingTransaction.class, AnotherWorkingTransaction.class, FailOnRollbackTransaction.class),
            "Rollback should fail due to FailOnRollbackTransaction");
        checkCounter(3, 0, 2);

        // check suppressed exceptions
        Throwable[] suppressed = exception.getSuppressed();
        assertTrue(suppressed.length > 0, "There should be a suppressed exception");
    }

    @Test
    public void isActive() {
        // test commit
        MCRTransactionManager
            .setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class, AnotherWorkingTransaction.class));
        MCRTransactionManager.beginTransactions();
        assertTrue(MCRTransactionManager.isActive(WorkingTransaction.class),
            "WorkingTransaction should be active after begin");
        assertTrue(MCRTransactionManager.isActive(AnotherWorkingTransaction.class),
            "AnotherWorkingTransaction should be active after begin");
        MCRTransactionManager.commitTransactions(WorkingTransaction.class);
        assertFalse(MCRTransactionManager.isActive(WorkingTransaction.class),
            "WorkingTransaction should not be active after commit");
        assertTrue(MCRTransactionManager.isActive(AnotherWorkingTransaction.class),
            "AnotherWorkingTransaction should still be active after commit of WorkingTransaction");

        // test rollback
        MCRTransactionManager.rollbackTransactions(AnotherWorkingTransaction.class);
        assertFalse(MCRTransactionManager.isActive(AnotherWorkingTransaction.class),
            "AnotherWorkingTransaction should not be active after rollback");

        // Verify that both transactions are active again after begin
        MCRTransactionManager.beginTransactions();
        assertTrue(MCRTransactionManager.isActive(WorkingTransaction.class),
            "WorkingTransaction should be active after second begin");
        assertTrue(MCRTransactionManager.isActive(AnotherWorkingTransaction.class),
            "AnotherWorkingTransaction should be active after second begin");

        // Rollback all transactions
        MCRTransactionManager.rollbackTransactions();
        assertFalse(MCRTransactionManager.isActive(WorkingTransaction.class),
            "WorkingTransaction should not be active after rollback");
        assertFalse(MCRTransactionManager.isActive(AnotherWorkingTransaction.class),
            "AnotherWorkingTransaction should not be active after rollback");
    }

    @Test
    public void isReady() {
        MCRTransactionManager
            .setTransactionLoader(new TransactionLoaderMock(WorkingTransaction.class, NotReadyTransaction.class));
        assertTrue(MCRTransactionManager.isReady(WorkingTransaction.class), "WorkingTransaction should be ready");
        assertFalse(MCRTransactionManager.isReady(NotReadyTransaction.class),
            "NotReadyTransaction should not be ready");
    }

    @Test
    public void listActiveTransactions() {
        // Set up the transaction loader with two working transactions
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));

        // Begin transactions
        MCRTransactionManager.beginTransactions();

        // Get the list of active transactions
        List<Class<? extends MCRPersistenceTransaction>> activeTransactions =
            MCRTransactionManager.listActiveTransactions();

        // Verify that both transactions are in the list
        assertEquals(2, activeTransactions.size(), "There should be two active transactions");
        assertTrue(activeTransactions.contains(WorkingTransaction.class),
            "Active transactions should contain WorkingTransaction");
        assertTrue(activeTransactions.contains(AnotherWorkingTransaction.class),
            "Active transactions should contain AnotherWorkingTransaction");

        // Commit transactions
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void listRollbackOnlyTransactions() {
        // Set up the transaction loader with two working transactions
        MCRTransactionManager.setTransactionLoader(new TransactionLoaderMock(
            WorkingTransaction.class, AnotherWorkingTransaction.class));

        // Begin transactions
        MCRTransactionManager.beginTransactions();

        // Mark one transaction as rollback-only
        MCRTransactionManager.setRollbackOnly(WorkingTransaction.class);

        // Get the list of rollback-only transactions
        List<Class<? extends MCRPersistenceTransaction>> rollbackOnlyTransactions =
            MCRTransactionManager.listRollbackOnlyTransactions();

        // Verify that only the marked transaction is in the list
        assertEquals(1, rollbackOnlyTransactions.size(), "There should be one rollback-only transaction");
        assertTrue(rollbackOnlyTransactions.contains(WorkingTransaction.class),
            "Rollback-only transactions should contain WorkingTransaction");
        assertFalse(rollbackOnlyTransactions.contains(AnotherWorkingTransaction.class),
            "Rollback-only transactions should not contain AnotherWorkingTransaction");

        // Rollback transactions
        MCRTransactionManager.rollbackTransactions();
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Ensure that any active transactions are cleared before each test
        MCRTransactionManager.rollbackTransactions();
        // Store the original transaction loader to restore it after the test
        originalTransactionLoader = MCRTransactionManager.getTransactionLoader();
        // reset counter
        BaseTransaction.resetCounter();
    }

    @AfterEach
    public void tearDown() throws Exception {
        MCRTransactionManager.setTransactionLoader(originalTransactionLoader);
    }

    private static void checkCounter(int begin, int commit, int rollback) {
        assertEquals(begin, BaseTransaction.BEGIN_COUNTER.get(), "begin counter should be equals");
        assertEquals(commit, BaseTransaction.COMMIT_COUNTER.get(), "commit counter should be equals");
        assertEquals(rollback, BaseTransaction.ROLLBACK_COUNTER.get(), "rollback counter should be equals");
    }

    private static void resetCounter() {
        BaseTransaction.resetCounter();
    }

    private static class TransactionLoaderMock implements MCRTransactionManager.TransactionLoader {

        private final List<Class<? extends MCRPersistenceTransaction>> transactionClasses;

        @SafeVarargs
        TransactionLoaderMock(Class<? extends MCRPersistenceTransaction>... transactionClasses) {
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
                throw new MCRTransactionException("Failed to instantiate transaction: " + clazz.getName(), e);
            }
        }
    }

    private abstract static class BaseTransaction implements MCRPersistenceTransaction {

        public static final AtomicInteger BEGIN_COUNTER = new AtomicInteger(0);
        public static final AtomicInteger COMMIT_COUNTER = new AtomicInteger(0);
        public static final AtomicInteger ROLLBACK_COUNTER = new AtomicInteger(0);

        @Override
        public void begin() {
            BEGIN_COUNTER.incrementAndGet();
        }

        @Override
        public void commit() {
            COMMIT_COUNTER.incrementAndGet();
        }

        @Override
        public void rollback() {
            ROLLBACK_COUNTER.incrementAndGet();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        public static void resetCounter() {
            BEGIN_COUNTER.set(0);
            COMMIT_COUNTER.set(0);
            ROLLBACK_COUNTER.set(0);
        }

        @Override
        public int getCommitPriority() {
            return 0;
        }

    }

    private static class WorkingTransaction extends BaseTransaction {

        @Override
        public int getCommitPriority() {
            return 5;
        }

    }

    private static class AnotherWorkingTransaction extends BaseTransaction {

        @Override
        public int getCommitPriority() {
            return 4;
        }

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
