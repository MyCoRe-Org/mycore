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
    default MCRProcessableSupplier<?> submit(Runnable runnable) {
        return submit(runnable, 0);
    }

    /**
     * Submits the runnable with the given priority. 
     * 
     * @param runnable the runnable to submit
     * @return a {@link MCRProcessableSupplier} with no result
     */
    default MCRProcessableSupplier<?> submit(Runnable runnable, int priority) {
        return submit(MCRProcessableFactory.progressableCallable(runnable), priority);
    }

    /**
     * Submits the callable with priority zero (executed at last). 
     * 
     * @param callable the callable to submit
     * @return a {@link MCRProcessableSupplier} with the result of R
     */
    default <R> MCRProcessableSupplier<R> submit(Callable<R> callable) {
        return submit(callable, 0);
    }

    /**
     * Submits the callable with the given priority.  
     * 
     * @param callable the callable to submit
     * @return a {@link MCRProcessableSupplier} with the result of R
     */
    <R> MCRProcessableSupplier<R> submit(Callable<R> callable, int priority);

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
    ExecutorService getExecutor();
}
