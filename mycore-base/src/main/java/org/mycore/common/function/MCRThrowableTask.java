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

package org.mycore.common.function;

import java.util.Objects;

@FunctionalInterface
public interface MCRThrowableTask<T extends Throwable> {
    void run() throws T;

    /**
     * Returns a composed task that first runs the {@code before}
     * task, and then runs this task.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before}
     * function and then applies this function
     * @throws NullPointerException if before is null
     *
     * @see #andThen(MCRThrowableTask)
     */
    default MCRThrowableTask<T> compose(MCRThrowableTask<? extends T> before) {
        Objects.requireNonNull(before);
        return () -> {
            before.run();
            run();
        };
    }

    /**
     * Returns a composed task that first this task, and then runs the {@code after} task.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the function to apply after this function is applied
     * @return a composed task that first runs this task and then
     * the {@code after} task
     * @throws NullPointerException if after is null
     *
     * @see #compose(MCRThrowableTask)
     */
    default MCRThrowableTask<T> andThen(MCRThrowableTask<? extends T> after) {
        Objects.requireNonNull(after);
        return () -> {
            run();
            after.run();
        };
    }
}
