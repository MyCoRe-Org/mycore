package org.mycore.util.concurrent;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * A supplier with a priority.
 *
 * <a href="http://stackoverflow.com/questions/34866757/how-do-i-use-completablefuture-supplyasync-together-with-priorityblockingqueue">stackoverflow</a>
 *
 * @author Matthias Eichner
 *
 * @param <T>  the type of results supplied by this supplier
 */
public class MCRPrioritySupplier<T> implements Supplier<T>, MCRPrioritizable {

    private Supplier<T> delegate;

    private int priority;

    private Instant created;

    public MCRPrioritySupplier(Supplier<T> delegate, int priority) {
        this.delegate = delegate;
        this.priority = priority;
        this.created = Instant.now();
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public Instant getCreated() {
        return this.created;
    }

}
