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

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.processing.MCRProcessable;
import org.mycore.common.processing.MCRProcessableStatus;
import org.mycore.common.processing.MCRProcessableTask;
import org.mycore.common.processing.MCRProgressable;
import org.mycore.util.concurrent.MCRDecorator;
import org.mycore.util.concurrent.MCRPrioritySupplier;

/**
 * A processable supplier combines a {@link Supplier} and a {@link MCRProcessable}.
 * The supplier will be executed with the help of an {@link CompletableFuture}.
 * To get the future call {@link #getFuture()}.
 * 
 * @author Matthias Eichner
 *
 * @param <R> the result of the task
 */
public class MCRProcessableSupplier<R> extends MCRProcessableTask<Callable<R>> implements Supplier<R> {

    protected CompletableFuture<R> future;

    /**
     * Creates a new {@link MCRProcessableSupplier} by the already committed task and its future.
     * 
     * @param task the task which should be executed
     * @param future the future
     * @return a new processable supplier
     */
    public static <T> MCRProcessableSupplier<T> of(Callable<T> task, CompletableFuture<T> future) {
        MCRProcessableSupplier<T> ps = new MCRProcessableSupplier<>(task);
        ps.future = future;
        return ps;
    }

    /**
     * Creates a new {@link MCRProcessableSupplier} by submitting the task to the executorService with
     * the given priority.
     * 
     * @param task the task to submit
     * @param executorService the executor service
     * @param priority the priority
     * @return a new processable supplier
     */
    public static <T> MCRProcessableSupplier<T> of(Callable<T> task, ExecutorService executorService,
        Integer priority) {
        MCRProcessableSupplier<T> ps = new MCRProcessableSupplier<>(task);
        ps.future = CompletableFuture.supplyAsync(new MCRPrioritySupplier<>(ps, priority),
            executorService);
        return ps;
    }

    /**
     * Private constructor, 
     * 
     * @param task the task itself
     */
    private MCRProcessableSupplier(Callable<R> task) {
        super(task);
    }

    /**
     * Gets the result of the task. Will usually be called by the executor service
     * and should not be executed directly.
     */
    @Override
    public R get() {
        try {
            this.setStatus(MCRProcessableStatus.processing);
            this.startTime = Instant.now();
            R result = getTask().call();
            this.setStatus(MCRProcessableStatus.successful);
            return result;
        } catch (InterruptedException exc) {
            this.error = exc;
            this.setStatus(MCRProcessableStatus.canceled);
            throw new MCRException(this.error);
        } catch (Exception exc) {
            this.error = exc;
            this.setStatus(MCRProcessableStatus.failed);
            throw new MCRException(this.error);
        }
    }

    /**
     * The future the task is assigned to.
     * 
     * @return the future
     */
    public CompletableFuture<R> getFuture() {
        return future;
    }

    /**
     * Returns true if this task completed. Completion may be due to normal termination,
     * an exception, or cancellation -- in all of these cases, this method will return true.
     *
     * @return true if this task completed
     */
    public boolean isFutureDone() {
        return future.isDone();
    }

    /**
     * Same as {@link Future#cancel(boolean)}.
     * 
     * @param mayInterruptIfRunning true if the thread executing this task should be interrupted;
     *      otherwise, in-progress tasks are allowed to complete
     * @return false if the task could not be cancelled, typically because it has already
     *      completed normally; true otherwise
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * Returns an integer between 0-100 indicating the progress
     * of the processable. Can return null if the task is not started
     * yet or the task is not an instance of {@link MCRProgressable}.
     * 
     * @return the progress between 0-100 or null
     */
    @Override
    public Integer getProgress() {
        if (task instanceof MCRProgressable) {
            return ((MCRProgressable) task).getProgress();
        }
        return super.getProgress();
    }

    /**
     * Returns a human readable text indicating the state of the progress.
     * 
     * @return progress text
     */
    @Override
    public String getProgressText() {
        if (task instanceof MCRProgressable) {
            return ((MCRProgressable) task).getProgressText();
        }
        return super.getProgressText();
    }

    /**
     * Returns the name of this process. If no name is set
     * this returns the simplified class name of the task.
     * 
     * @return a human readable name of this processable task
     */
    @Override
    public String getName() {
        String name = super.getName();
        if (name == null) {
            return MCRDecorator.resolve(this.task).map(object -> {
                if (object instanceof MCRProcessable) {
                    return ((MCRProcessable) object).getName();
                }
                return object.toString();
            }).orElse(this.task.toString());
        }
        return name;
    }

}
