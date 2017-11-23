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
