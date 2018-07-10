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

package org.mycore.util.concurrent.processing;

import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.processing.MCRListenableProgressable;
import org.mycore.common.processing.MCRProcessable;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProgressable;
import org.mycore.common.processing.MCRProgressableListener;
import org.mycore.util.concurrent.MCRDecorator;
import org.mycore.util.concurrent.MCRRunnableComperator;

/**
 * Factory and utility methods for {@link MCRProcessableExecutor}.
 *
 * @author Matthias Eichner
 */
public abstract class MCRProcessableFactory {

    /**
     * Returns a {@link Callable} object that, when
     * called, runs the given task and returns {@code null}.
     * It also takes care if the task implements the
     * {@link MCRProgressable} interface by calling the runnable
     * implementation.
     *
     * @param task the task to run
     * @return a callable object
     * @throws NullPointerException if task null
     */
    public static Callable<Object> progressableCallable(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableProgressableAdapter<>(task);
    }

    /**
     * Creates a new {@link MCRProcessableExecutor}.
     *
     * <p>
     * Be aware that you should shutdown the delegate with the {@link MCRShutdownHandler}
     * by yourself. This method will not do this for you!
     * </p>
     *
     * <p><b>
     * If you like to have priority support you have to use a thread pool with a
     * {@link PriorityBlockingQueue}!
     * </b></p>
     *
     * @param delegate the thread pool delegate
     * @return a newly created thread pool
     */
    public static MCRProcessableExecutor newPool(ExecutorService delegate) {
        return new MCRProcessableThreadPoolExecutorHelper(delegate);
    }

    /**
     * Creates a new {@link MCRProcessableExecutor}. Each task submitted will be
     * automatically added to the given collection and removed if complete.
     *
     * <p>
     * Be aware that you should shutdown the delegate with the {@link MCRShutdownHandler}
     * by yourself. This method will not do this for you!
     * </p>
     *
     * <p><b>
     * If you like to have priority support you have to use a thread pool with a
     * {@link PriorityBlockingQueue}!
     * </b></p>
     *
     * @param delegate the thread pool delegate
     * @param collection the collection which will be linked with the pool
     * @return a newly created thread pool
     */
    public static MCRProcessableExecutor newPool(ExecutorService delegate, MCRProcessableCollection collection) {
        return new MCRProcessableThreadPoolExecutorHelper(delegate, collection);
    }

    /**
     * Creates new PriorityBlockingQueue for runnables. Uses the {@link MCRRunnableComperator}
     * for comparision.
     *
     * @return a new priority blocking queue
     */
    public static PriorityBlockingQueue<Runnable> newPriorityBlockingQueue() {
        int initialCapacity = 11; //taken from java.util.concurrent.PriorityBlockingQueue.DEFAULT_INITIAL_CAPACITY
        return new PriorityBlockingQueue<>(initialCapacity, Comparator.nullsLast(new MCRRunnableComperator()));
    }

    /**
     * Helper class glueing a thread pool and a collection together.
     */
    private static class MCRProcessableThreadPoolExecutorHelper implements MCRProcessableExecutor {

        private ExecutorService executor;

        private MCRProcessableCollection collection;

        public MCRProcessableThreadPoolExecutorHelper(ExecutorService delegate) {
            this(delegate, null);
        }

        public MCRProcessableThreadPoolExecutorHelper(ExecutorService delegate, MCRProcessableCollection collection) {
            this.executor = delegate;
            this.collection = collection;
        }

        /*
         * (non-Javadoc)
         * @see org.mycore.util.concurrent.processing.ProcessableExecutor#submit(java.util.concurrent.Callable, int)
         */
        @Override
        public <R> MCRProcessableSupplier<R> submit(Callable<R> callable, int priority) {
            MCRProcessableSupplier<R> supplier = MCRProcessableSupplier.of(callable, this.executor, priority);
            if (this.collection != null) {
                MCRProcessable processable = supplier;
                if(callable instanceof MCRProcessable) {
                    processable = (MCRProcessable) callable;
                } else if(callable instanceof RunnableProgressableAdapter) {
                    Runnable task = ((RunnableProgressableAdapter) callable).get();
                    if(task instanceof MCRProcessable) {
                        processable = (MCRProcessable) task;
                    }
                }
                this.collection.add(processable);
                supplier.getFuture().whenComplete((result, throwable) -> {
                    this.collection.remove(supplier);
                    if (throwable != null) {
                        LogManager.getLogger().error("Error while processing '{}'", supplier.getName(), throwable);
                    }
                });
            }
            return supplier;
        }

        @Override
        public ExecutorService getExecutor() {
            return this.executor;
        }

    }

    /**
     * A callable that runs given task and returns given result
     */
    private static final class RunnableProgressableAdapter<T>
        implements Callable<T>, MCRListenableProgressable, MCRDecorator<Runnable> {
        final Runnable task;

        RunnableProgressableAdapter(Runnable task) {
            this.task = task;
        }

        @Override
        public T call() {
            task.run();
            return null;
        }

        @Override
        public Integer getProgress() {
            if (task instanceof MCRProgressable) {
                return ((MCRProgressable) task).getProgress();
            }
            return null;
        }

        @Override
        public String getProgressText() {
            if (task instanceof MCRProgressable) {
                return ((MCRProgressable) task).getProgressText();
            }
            return null;
        }

        @Override
        public void addProgressListener(MCRProgressableListener listener) {
            if (task instanceof MCRListenableProgressable) {
                ((MCRListenableProgressable) task).addProgressListener(listener);
            }
        }

        @Override
        public void removeProgressListener(MCRProgressableListener listener) {
            if (task instanceof MCRListenableProgressable) {
                ((MCRListenableProgressable) task).removeProgressListener(listener);
            }
        }

        @Override
        public Runnable get() {
            return task;
        }

        @Override
        public String toString() {
            return task.toString();
        }

    }

}
