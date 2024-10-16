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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mycore.util.concurrent.MCRPool;

/**
 * Manages transactions for multiple persistence backends in a thread-safe manner.
 *
 * <p>The {@code MCRTransactionManager} class provides methods to begin, commit, and rollback
 * transactions involving implementations of {@link MCRPersistenceTransaction}. Transactions
 * are managed per thread using a {@link ThreadLocal} to keep track of active transactions.</p>
 *
 * <p>Transactions are loaded using a {@link TransactionLoader}, allowing for flexible addition
 * of persistence backends. The class handles exceptions during transaction operations by
 * attempting to rollback any partially completed transactions and collecting rollback
 * failures as suppressed exceptions.</p>
 *
 * <p>This class is designed to be used in a multi-threaded environment where each thread
 * may have its own set of active transactions.</p>
 *
 * TODO: rm active & rollbackonly from MCRPersistenceTransaction, add MCRPersistenceTransaction#failAllOnError
 * TODO: equals vs isInstance?
 */
public abstract class MCRTransactionManager {

    private static TransactionLoader transactionLoader = new PooledSystemTransactionLoader();

    private static final ThreadLocal<List<MCRPersistenceTransaction>> ACTIVE_TRANSACTIONS
        = ThreadLocal.withInitial(ArrayList::new);

    private static final ThreadLocal<List<MCRPersistenceTransaction>> ROLLBACK_ONLY_TRANSACTIONS
        = ThreadLocal.withInitial(ArrayList::new);

    /**
     * Sets the {@link TransactionLoader} to be used by the {@link MCRTransactionManager}.
     *
     * <p>This method allows setting a custom transaction loader, which can be useful in testing
     * or in situations where transactions are loaded in different ways (e.g., dynamic loading).</p>
     *
     * @param transactionLoader the custom {@link TransactionLoader} to set.
     */
    public static void setTransactionLoader(TransactionLoader transactionLoader) {
        MCRTransactionManager.transactionLoader = transactionLoader;
    }

    /**
     * Retrieves the currently configured {@link TransactionLoader}.
     *
     * <p>If a custom transaction loader has been set via {@link #setTransactionLoader(TransactionLoader)},
     * that loader is returned. Otherwise, the default loader is used.</p>
     *
     * @return the currently active {@link TransactionLoader}.
     */
    public static TransactionLoader getTransactionLoader() {
        return transactionLoader;
    }

    /**
     * Begins all available {@link MCRPersistenceTransaction} instances that are active.
     *
     * <p>It throws an exception if there are already active transactions in the current thread.</p>
     *
     * <p>If an exception occurs during the transaction begin process, a rollback is attempted
     * for any transactions that were successfully started before the exception.</p>
     *
     * @throws MCRTransactionException if there are already active transactions, or if an
     * error occurs while loading or beginning transactions.
     */
    public static void beginTransactions() {
        List<MCRPersistenceTransaction> activeTransactions = getActiveTransactions();
        if (!activeTransactions.isEmpty()) {
            throw new MCRTransactionException("Cannot beginTransaction while there are active transactions: " +
                activeTransactions);
        }
        List<MCRPersistenceTransaction> availableTransactions;
        try {
            availableTransactions = transactionLoader.load();
        } catch (Exception e) {
            throw new MCRTransactionException("Error while loading persistence transactions", e);
        }
        try {
            availableTransactions.forEach(transaction -> {
                activeTransactions.add(transaction);
                transaction.begin();
            });
        } catch (Exception e) {
            throwAndRollback(new MCRTransactionException("Error while beginTransaction", e));
        }
    }

    /**
     * Begins the specified types of {@link MCRPersistenceTransaction} transactions.
     *
     * <p>This method checks whether the specified transactions are already active. If any of the
     * requested transactions are active, an {@link MCRTransactionException} is thrown. Otherwise,
     * the transactions are loaded and begun.</p>
     *
     * <p>If an exception occurs during the transaction begin process, a rollback is attempted
     * for any transactions that were successfully started before the exception.
     * This includes even transactions which were started before this method call.</p>
     *
     * @param transactionClasses the classes of the transactions to begin.
     * @throws MCRTransactionException if any of the specified transaction types are already active,
     * or if an error occurs while beginning transactions.
     */
    @SafeVarargs
    public static void beginTransactions(Class<? extends MCRPersistenceTransaction>... transactionClasses) {
        List<MCRPersistenceTransaction> activeTransactions = getActiveTransactions();
        for (Class<? extends MCRPersistenceTransaction> transactionClass : transactionClasses) {
            boolean isActive = activeTransactions.stream().anyMatch(transactionClass::isInstance);
            if (isActive) {
                throw new MCRTransactionException("A transaction of type " + transactionClass.getSimpleName() +
                    " is already active. Cannot begin a new one.");
            }
        }
        beginTransactions(List.of(transactionClasses));
    }

    /**
     * Ensures that all available {@link MCRPersistenceTransaction} instances are active.
     *
     * <p>This method checks whether each available transaction is currently active. If any
     * of the loaded transactions are not active, they are begun and added to the active transactions list.</p>
     *
     * <p>If an exception occurs during the transaction begin process, a rollback is attempted
     * for all active transactions, including those that were active before this method was called.</p>
     *
     * @throws MCRTransactionException if an error occurs while beginning any missing transactions.
     */
    public static void requireTransactions() {
        List<MCRPersistenceTransaction> activeTransactions = getActiveTransactions();
        List<MCRPersistenceTransaction> loadedTransactions = transactionLoader.load();
        Set<Class<? extends MCRPersistenceTransaction>> activeTransactionClasses = activeTransactions.stream()
            .map(MCRPersistenceTransaction::getClass)
            .collect(Collectors.toSet());
        try {
            for (MCRPersistenceTransaction transaction : loadedTransactions) {
                if (!activeTransactionClasses.contains(transaction.getClass())) {
                    transaction.begin();
                    activeTransactions.add(transaction);
                }
            }
        } catch (Exception e) {
            throwAndRollback(new MCRTransactionException("Error while requireTransaction.", e));
        }
    }

    /**
     * Ensures that the specified types of {@link MCRPersistenceTransaction} transactions are active.
     *
     * <p>This method checks whether each of the specified transaction types is currently active. If any
     * of the requested transaction types are not active, they are begun.</p>
     *
     * <p>If an exception occurs during the transaction begin process, a rollback is attempted
     * for any transactions that were successfully started before the exception.
     * This includes even transactions which were started before this method call.</p>
     *
     * @param transactionClasses the classes of the transactions to ensure are active.
     * @throws MCRTransactionException if an error occurs while beginning any missing transactions.
     */
    @SafeVarargs
    public static void requireTransactions(Class<? extends MCRPersistenceTransaction>... transactionClasses) {
        List<MCRPersistenceTransaction> activeTransactions = getActiveTransactions();
        List<Class<? extends MCRPersistenceTransaction>> nonActiveTransactionClasses = Arrays.stream(transactionClasses)
            .filter(transactionClass -> activeTransactions.stream().noneMatch(transactionClass::isInstance))
            .collect(Collectors.toList());
        if (!nonActiveTransactionClasses.isEmpty()) {
            beginTransactions(nonActiveTransactionClasses);
        }
    }

    /**
     * Begins the specified list of {@link MCRPersistenceTransaction} transaction classes.
     *
     * <p>This method first loads all available transactions and attempts to start each of the specified
     * transaction types. If a transaction of the requested type is not available or cannot be started,
     * an {@link MCRTransactionException} is thrown.</p>
     *
     * <p>If an exception occurs during the begin process, the method attempts to rollback all successfully
     * started transactions and collects any rollback failures as suppressed exceptions in the thrown
     * {@link MCRTransactionException}.</p>
     *
     * @param transactionClasses the list of transaction classes to begin.
     * @throws MCRTransactionException if any of the specified transaction types cannot be started,
     * or if an error occurs during the begin process.
     */
    private static void beginTransactions(List<Class<? extends MCRPersistenceTransaction>> transactionClasses) {
        List<MCRPersistenceTransaction> activeTransactions;
        List<MCRPersistenceTransaction> availableTransactions;
        try {
            activeTransactions = getActiveTransactions();
            availableTransactions = transactionLoader.load();
        } catch (Exception e) {
            throw new MCRTransactionException("Error while beginTransactions " + transactionClasses, e);
        }
        try {
            for (Class<? extends MCRPersistenceTransaction> transactionClass : transactionClasses) {
                MCRPersistenceTransaction transaction = availableTransactions.stream()
                    .filter(transactionClass::isInstance)
                    .findFirst()
                    .orElseThrow(() -> new MCRTransactionException(
                        "No available transaction of type " + transactionClass.getName()));
                activeTransactions.add(transaction);
                transaction.begin();
            }
        } catch (Exception e) {
            throwAndRollback(new MCRTransactionException("Error while beginTransaction: " + transactionClasses, e));
        }
    }

    /**
     * Commits all active {@link MCRPersistenceTransaction} instances.
     *
     * <p>This method iterates over all active transactions in the current thread, commits each one, and removes it
     * from the active transactions list. If an exception occurs during the commit process, it attempts to roll back
     * any transactions that were not yet committed.
     * <p>
     * After a successful commit, it submits any on-commit tasks of the current {@link MCRSession}.
     *
     * @throws MCRTransactionException if an error occurs during the commit process, with any rollback failures
     *         added as suppressed exceptions
     */
    public static void commitTransactions() {
        List<MCRPersistenceTransaction> activeTransactions = new ArrayList<>(getActiveTransactions());
        commitTransactions(activeTransactions);
        MCRSessionMgr.getCurrentSession().submitOnCommitTasks();
    }

    /**
     * Commits the specified types of {@link MCRPersistenceTransaction} transactions.
     *
     * <p>This method commits the active transactions of the specified types in the current thread.
     * If an exception occurs during the commit process, it attempts to roll back
     * any transactions that were not yet committed.
     * <p>
     * After a successful commit, it submits any on-commit tasks of the current {@link MCRSession}.
     *
     * @param transactionClasses the classes of the transactions to commit
     * @throws MCRTransactionException if an error occurs during the commit process
     */
    @SafeVarargs
    public static void commitTransactions(Class<? extends MCRPersistenceTransaction>... transactionClasses) {
        commitTransactions(true, transactionClasses);
    }

    /**
     * Commits the specified types of {@link MCRPersistenceTransaction} transactions, with an option to submit on-commit
     * tasks.
     *
     * <p>This method commits the active transactions of the specified types in the current thread.
     * If an exception occurs during the commit process, it attempts to roll back
     * any transactions that were not yet committed.
     * <p>
     * After a successful commit and if {@code submitOnCommitTasks} is {@code true}, it submits any on-commit tasks
     * of the current {@link MCRSession}.
     *
     * @param submitOnCommitTasks if {@code true}, submit on-commit tasks after successful commits
     * @param transactionClasses the classes of the transactions to commit
     * @throws MCRTransactionException if an error occurs during the commit process, with any rollback failures
     *         added as suppressed exceptions
     */
    @SafeVarargs
    public static void commitTransactions(boolean submitOnCommitTasks,
        Class<? extends MCRPersistenceTransaction>... transactionClasses) {
        List<? extends MCRPersistenceTransaction> activeTransactions = getActiveTransactions(transactionClasses);
        commitTransactions(activeTransactions);
        if (submitOnCommitTasks) {
            MCRSessionMgr.getCurrentSession().submitOnCommitTasks();
        }
    }

    private static void commitTransactions(List<? extends MCRPersistenceTransaction> activeTransactions) {
        try {
            activeTransactions.forEach(transaction -> {
                if (isRollbackOnly(transaction)) {
                    throw new MCRTransactionException(
                        "Transaction " + transaction + " marked as rollback-only. Cannot commit.");
                }
                transaction.commit();
                getActiveTransactions().remove(transaction);
            });
        } catch (Exception e) {
            throwAndRollback(new MCRTransactionException("Error while commitTransaction.", e));
        }
    }

    /**
     * Rolls back all active {@link MCRPersistenceTransaction} instances.
     *
     * <p>This method iterates over all active transactions in the current thread, attempts to roll back each one,
     * and then clears the active transactions list. If exceptions occur during the rollback process, they are collected,
     * and a {@link MCRTransactionException} is thrown at the end with all suppressed exceptions.</p>
     *
     * @throws MCRTransactionException if any errors occur during the rollback process
     */
    public static void rollbackTransactions() {
        List<? extends MCRPersistenceTransaction> transactionsToRollback = new ArrayList<>(getActiveTransactions());
        rollbackTransactions(transactionsToRollback);
    }

    /**
     * Rolls back the specified types of {@link MCRPersistenceTransaction} transactions.
     *
     * <p>This method attempts to roll back the active transactions of the specified types in the current thread.
     * After attempting to roll back the transactions, it removes them from the active transactions list.
     * If exceptions occur during the rollback process, they are collected, and a {@link MCRTransactionException}
     * is thrown at the end with all suppressed exceptions.</p>
     *
     * @param transactionClasses the classes of the transactions to roll back
     * @throws MCRTransactionException if any errors occur during the rollback process
     */
    @SafeVarargs
    public static void rollbackTransactions(Class<? extends MCRPersistenceTransaction>... transactionClasses) {
        List<? extends MCRPersistenceTransaction> transactionsToRollback = getActiveTransactions(transactionClasses);
        rollbackTransactions(transactionsToRollback);
    }

    private static void rollbackTransactions(List<? extends MCRPersistenceTransaction> transactionsToRollback) {
        List<Exception> exceptionsOnRollback = rollback(transactionsToRollback);
        if (!exceptionsOnRollback.isEmpty()) {
            MCRTransactionException rollbackException
                = new MCRTransactionException("Errors occurred during rollbackTransactions.");
            exceptionsOnRollback.forEach(rollbackException::addSuppressed);
            throw rollbackException;
        }
    }

    /**
     * Marks all active {@link MCRPersistenceTransaction} instances so that the only possible outcome
     * of the transaction is for the transaction to be rolled back.
     */
    public static void setRollbackOnly() {
        getRollbackOnlyTransactions().addAll(getActiveTransactions());
    }

    /**
     * Marks the specified types of transactions as rollback-only.
     *
     * <p>This method adds the specified transaction classes to the rollback-only set,
     * indicating that these transactions should be rolled back and not committed.</p>
     *
     * @param transactionClasses the classes of the transactions to mark as rollback-only
     */
    @SafeVarargs
    public static void setRollbackOnly(Class<? extends MCRPersistenceTransaction>... transactionClasses) {
        getActiveTransactions(transactionClasses)
            .forEach(transaction -> getRollbackOnlyTransactions().add(transaction));
    }

    /**
     * Checks if a {@link MCRPersistenceTransaction} of the specified type is ready.
     *
     * <p>This method loads all available persistence transactions and checks if any of them are instances of
     * the specified class. A transaction is considered ready if it is available and ready to be begun.</p>
     *
     * @param transactionClass the class of the transaction to check
     * @return {@code true} if a transaction of the specified type is ready; {@code false} otherwise
     */
    public static boolean isReady(Class<? extends MCRPersistenceTransaction> transactionClass) {
        return transactionLoader.load().stream().anyMatch(transactionClass::isInstance);
    }

    /**
     * Checks if a {@link MCRPersistenceTransaction} of the specified type is active.
     *
     * <p>This method checks the list of active transactions in the current thread to determine if any of them
     * are instances of the specified class.</p>
     *
     * @param transactionClass the class of the transaction to check
     * @return {@code true} if a transaction of the specified type is active; {@code false} otherwise
     */
    public static boolean isActive(Class<? extends MCRPersistenceTransaction> transactionClass) {
        return getActiveTransactions().stream().anyMatch(transactionClass::isInstance);
    }

    /**
     * Checks if the specified transaction class is marked for rollback-only.
     *
     * <p>This method determines if a particular transaction class has been marked
     * as rollback-only, meaning that any attempt to commit transactions of this type
     * will result in a rollback.</p>
     *
     * <p>The rollback-only state is centrally managed by the {@link MCRTransactionManager}
     * and can be set via the {@link #setRollbackOnly(Class[])} method.</p>
     *
     * @param transactionClass the class of the transaction to check
     * @return {@code true} if the transaction class is marked for rollback-only,
     *         {@code false} otherwise
     */
    public static boolean isRollbackOnly(Class<? extends MCRPersistenceTransaction> transactionClass) {
        return getRollbackOnlyTransactions().stream().anyMatch(transactionClass::isInstance);
    }

    /**
     * Checks if the specified transaction is marked for rollback-only.
     *
     * <p>This method determines if a particular transaction has been marked
     * as rollback-only, meaning that any attempt to commit transactions of this type
     * will result in a rollback.</p>
     *
     * <p>The rollback-only state is centrally managed by the {@link MCRTransactionManager}
     * and can be set via the {@link #setRollbackOnly(Class[])} method.</p>
     *
     * @param transaction the class of the transaction to check
     * @return {@code true} if the transaction class is marked for rollback-only,
     *         {@code false} otherwise
     */
    private static boolean isRollbackOnly(MCRPersistenceTransaction transaction) {
        return getRollbackOnlyTransactions().contains(transaction);
    }

    /**
     * Rolls back all active {@link MCRPersistenceTransaction} instances and collects any rollback errors.
     *
     * <p>This method attempts to rollback all transactions that are currently active. If any exceptions occur
     * during the rollback process, they are added as suppressed exceptions to the provided
     * {@link MCRTransactionException}.
     * After attempting to rollback, the {@link #ACTIVE_TRANSACTIONS} list is cleared, and the provided exception is
     * thrown.</p>
     *
     * @param transactionException the {@link MCRTransactionException} that will be thrown after rollbacks
     *                             are attempted, with any rollback exceptions added as suppressed exceptions.
     * @throws MCRTransactionException after attempting rollback, with any rollback exceptions suppressed.
     */
    private static void throwAndRollback(MCRTransactionException transactionException) {
        List<MCRPersistenceTransaction> transactionsToRollback = new ArrayList<>(getActiveTransactions());
        List<Exception> exceptionsOnRollback = rollback(transactionsToRollback);
        exceptionsOnRollback.forEach(transactionException::addSuppressed);
        throw transactionException;
    }

    private static List<Exception> rollback(List<? extends MCRPersistenceTransaction> transactionsToRollback) {
        List<Exception> exceptionsOnRollback = new ArrayList<>();
        for (MCRPersistenceTransaction transaction : transactionsToRollback) {
            try {
                transaction.rollback();
            } catch (Exception rollbackException) {
                exceptionsOnRollback.add(rollbackException);
            } finally {
                getActiveTransactions().remove(transaction);
                getRollbackOnlyTransactions().remove(transaction);
            }
        }
        return exceptionsOnRollback;
    }

    /**
     * Retrieves a list of all active {@link MCRPersistenceTransaction} instances.
     *
     * @return a list of active transactions matching the specified types
     */
    private static List<MCRPersistenceTransaction> getActiveTransactions() {
        return ACTIVE_TRANSACTIONS.get();
    }

    /**
     * Retrieves a list of active {@link MCRPersistenceTransaction} instances matching the specified types.
     *
     * <p>This method iterates over the list of active transactions in the current thread and collects those
     * that are instances of the specified classes.</p>
     *
     * @param requestedTransactionClasses the classes of the transactions to retrieve
     * @return a list of active transactions matching the specified types
     */
    @SafeVarargs
    private static List<? extends MCRPersistenceTransaction>
        getActiveTransactions(Class<? extends MCRPersistenceTransaction>... requestedTransactionClasses) {
        List<MCRPersistenceTransaction> requestedTransactions = new ArrayList<>();
        List<MCRPersistenceTransaction> activeTransactions = getActiveTransactions();
        for (Class<? extends MCRPersistenceTransaction> transactionClass : requestedTransactionClasses) {
            for (MCRPersistenceTransaction transaction : activeTransactions) {
                if (transactionClass.isInstance(transaction)) {
                    requestedTransactions.add(transactionClass.cast(transaction));
                }
            }
        }
        return requestedTransactions;
    }

    public static boolean hasActiveTransactions() {
        return !ACTIVE_TRANSACTIONS.get().isEmpty();
    }

    private static List<MCRPersistenceTransaction> getRollbackOnlyTransactions() {
        return ROLLBACK_ONLY_TRANSACTIONS.get();
    }

    public static boolean hasRollbackOnlyTransactions() {
        return !ROLLBACK_ONLY_TRANSACTIONS.get().isEmpty();
    }

    /**
     * Defines a contract for loading {@link MCRPersistenceTransaction} instances.
     *
     * <p>This interface provides a method to load persistence transactions from different sources,
     * such as a {@link ServiceLoader} or other custom mechanisms. Implementations of this interface
     * can provide the flexibility to load transactions in various ways.</p>
     */
    public interface TransactionLoader {

        /**
         * Loads all available {@link MCRPersistenceTransaction} instances.
         *
         * <p>The method should return a list of ready transactions, meaning that the returned transactions
         * should be available and ready for use.</p>
         *
         * @return a list of ready {@link MCRPersistenceTransaction} instances.
         */
        List<MCRPersistenceTransaction> load();

    }

    /**
     * A {@link TransactionLoader} implementation that uses a pooled {@link ServiceLoader} to load
     * {@link MCRPersistenceTransaction} instances.
     *
     * <p>This loader uses a thread-safe pool of {@link ServiceLoader} instances to manage the loading
     * of {@link MCRPersistenceTransaction} objects. The pool ensures that the service loading is
     * efficient, minimizing redundant loading operations.</p>
     */
    public static class PooledSystemTransactionLoader implements TransactionLoader {

        private static final MCRPool<ServiceLoader<MCRPersistenceTransaction>> SERVICE_LOADER_POOL = new MCRPool<>(
            Runtime.getRuntime().availableProcessors(), () -> ServiceLoader
                .load(MCRPersistenceTransaction.class, MCRClassTools.getClassLoader()));

        /**
         * Performs an operation using a {@link ServiceLoader} instance from the internal pool.
         *
         * <p>This method acquires a {@link ServiceLoader} from the {@link #SERVICE_LOADER_POOL}, applies the given
         * function to it, and then releases the {@link ServiceLoader} back to the pool. If an {@link InterruptedException}
         * occurs while acquiring the {@link ServiceLoader}, the provided default value is returned.</p>
         *
         * @param f the function to apply to the acquired {@link ServiceLoader}
         * @param defaultValue the value to return if the {@link ServiceLoader} could not be acquired
         * @param <V> the type of the result produced by the function
         * @return the result of applying the function to the acquired {@link ServiceLoader}, or the default value
         *         if the {@link ServiceLoader} could not be acquired
         */
        private static <V> V applyServiceLoader(Function<ServiceLoader<MCRPersistenceTransaction>, V> f,
            V defaultValue) {
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

        /**
         * Loads all available {@link MCRPersistenceTransaction} instances that are ready for use.
         *
         * <p>This method uses the {@link #applyServiceLoader(Function, Object)} method to obtain a list of
         * {@link MCRPersistenceTransaction} instances that are ready, by filtering the loaded services
         * with {@link MCRPersistenceTransaction#isReady()}.</p>
         *
         * @return a list of ready {@link MCRPersistenceTransaction} instances
         */
        @Override
        public List<MCRPersistenceTransaction> load() {
            return applyServiceLoader(sl -> sl.stream()
                .map(ServiceLoader.Provider::get)
                .filter(MCRPersistenceTransaction::isReady)
                .toList(), List.of());
        }

    }

}
