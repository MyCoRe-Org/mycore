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

package org.mycore.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
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

    private static final AtomicLong CREATION_COUNTER = new AtomicLong(0);

    private final Supplier<T> delegate;

    private final int priority;

    private final long created;

    public MCRPrioritySupplier(Supplier<T> delegate, int priority) {
        this.delegate = delegate;
        this.priority = priority;
        this.created = CREATION_COUNTER.incrementAndGet();
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
    public long getCreated() {
        return created;
    }

    /**
     * use this instead of {@link CompletableFuture#supplyAsync(Supplier, Executor)}
     * 
     * This method keep the priority
     */
    public CompletableFuture<T> runAsync(ExecutorService es) {
        CompletableFuture<T> result = new CompletableFuture<>();
        MCRPrioritySupplier<T> supplier = this;
        class MCRAsyncPrioritySupplier
            implements Runnable, MCRPrioritizable, CompletableFuture.AsynchronousCompletionTask {
            @Override
            @SuppressWarnings("PMD.AvoidCatchingThrowable")
            public void run() {
                try {
                    if (!result.isDone()) {
                        result.complete(supplier.get());
                    }
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            }

            @Override
            public int getPriority() {
                return supplier.getPriority();
            }

            @Override
            public long getCreated() {
                return supplier.getCreated();
            }

        }
        es.execute(new MCRAsyncPrioritySupplier());
        return result;

    }
}
