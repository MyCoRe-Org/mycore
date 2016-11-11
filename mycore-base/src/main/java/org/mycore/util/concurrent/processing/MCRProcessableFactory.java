package org.mycore.util.concurrent.processing;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.processing.MCRListenableProgressable;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProgressable;
import org.mycore.common.processing.MCRProgressableListener;

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
        if (!(task instanceof MCRProgressable)) {
            return Executors.callable(task);
        }
        return new RunnableProgressableAdapter<Object>(task);
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
        MCRProcessableExecutor threadPool = new MCRProcessableThreadPoolExecutorHelper(delegate, collection);
        return threadPool;
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
                this.collection.add(supplier);
                supplier.getFuture().whenComplete((result, throwable) -> {
                    this.collection.remove(supplier);
                });
            }
            return supplier;
        }

        public ExecutorService getExecutor() {
            return this.executor;
        }

    }

    /**
     * A callable that runs given task and returns given result
     */
    private static final class RunnableProgressableAdapter<T> implements Callable<T>, MCRListenableProgressable {
        final Runnable task;

        RunnableProgressableAdapter(Runnable task) {
            this.task = task;
        }

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
    }

}
