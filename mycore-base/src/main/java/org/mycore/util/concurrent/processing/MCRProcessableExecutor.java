package org.mycore.util.concurrent.processing;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * A processable executor uses a {@link ExecutorService} to submit
 * given tasks and returns a {@link MCRProcessableSupplier}.
 * 
 * @author Matthias Eichner
 */
public interface MCRProcessableExecutor {

    /**
     * Submits the runnable with priority zero (executed at last). 
     * 
     * @param runnable the runnable to submit
     * @return a {@link MCRProcessableSupplier} with no result
     */
    public default MCRProcessableSupplier<?> submit(Runnable runnable) {
        return submit(runnable, 0);
    }

    /**
     * Submits the runnable with the given priority. 
     * 
     * @param runnable the runnable to submit
     * @return a {@link MCRProcessableSupplier} with no result
     */
    public default MCRProcessableSupplier<?> submit(Runnable runnable, int priority) {
        return submit(MCRProcessableFactory.progressableCallable(runnable), priority);
    }

    /**
     * Submits the callable with priority zero (executed at last). 
     * 
     * @param callable the callable to submit
     * @return a {@link MCRProcessableSupplier} with the result of R
     */
    public default <R> MCRProcessableSupplier<R> submit(Callable<R> callable) {
        return submit(callable, 0);
    }

    /**
     * Submits the callable with the given priority.  
     * 
     * @param callable the callable to submit
     * @return a {@link MCRProcessableSupplier} with the result of R
     */
    public <R> MCRProcessableSupplier<R> submit(Callable<R> callable, int priority);

    /**
     * Returns the underlying executor service.
     * 
     * <p><b>
     * You should not submit task to this thread pool directly. Use the submit
     * methods of this class instead.
     * </b></p>
     * 
     * @return the thread pool.
     */
    public ExecutorService getExecutor();
}
