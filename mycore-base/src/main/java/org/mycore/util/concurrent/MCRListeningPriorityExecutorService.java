package org.mycore.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractListeningExecutorService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

/**
 * Execution service where tasks can be prioritized and listened. Extends the guava
 * {@link AbstractListeningExecutorService} class.
 * To prioritize a task it must implement the {@link MCRPrioritizable} interface. If a task
 * does not implement the interface its priority is set to zero (executed at last).
 * 
 * @author Matthias Eichner
 */
public class MCRListeningPriorityExecutorService extends AbstractListeningExecutorService {

    final ExecutorService delegate;

    /**
     * Create a new executor service. The delegate should use a {@link PriorityBlockingQueue}.
     */
    public MCRListeningPriorityExecutorService(ExecutorService delegate) {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public ListenableFuture<?> submit(Runnable task) {
        ListenableFutureTask<Void> ftask = ListenableFutureTask.create(task, null);
        execute(ftask, getPriority(task));
        return ftask;
    }

    @Override
    public <T> ListenableFuture<T> submit(Runnable task, T result) {
        ListenableFutureTask<T> ftask = ListenableFutureTask.create(task, result);
        execute(ftask, getPriority(task));
        return ftask;
    }

    @Override
    public <T> ListenableFuture<T> submit(Callable<T> task) {
        ListenableFutureTask<T> ftask = ListenableFutureTask.create(task);
        execute(ftask, getPriority(task));
        return ftask;
    }

    @SuppressWarnings("unchecked")
    private int getPriority(Object task) {
        if (task instanceof MCRPrioritizable) {
            return ((MCRPrioritizable<Integer>) task).getPriority();
        }
        return 0;
    }

    @Override
    public void execute(Runnable command) {
        execute(command, 0);
    }

    public void execute(Runnable command, int priority) {
        MCRPriorityRunnableDecorator prd = new MCRPriorityRunnableDecorator(command, priority);
        delegate.execute(prd);
    }

}
