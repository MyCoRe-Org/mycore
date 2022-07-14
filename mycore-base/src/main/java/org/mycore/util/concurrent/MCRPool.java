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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A MCRPool allows thread safe pooling of thread unsafe objects. 
 * @param <T>
 */
public class MCRPool<T> {
    private final BlockingQueue<T> pool;

    private final Semaphore semaphore;

    private final Supplier<T> supplier;

    /**
     * Creates an MCRPool of the given size
     * @param size capacity of the pool
     * @param supplier return values for {@link #acquire()}, called not more than size times
     */
    public MCRPool(int size, Supplier<T> supplier) {
        this.pool = new ArrayBlockingQueue<>(size, true);
        this.semaphore = new Semaphore(size, false);
        this.supplier = supplier;
    }

    /**
     * Acquires a value from the pool.
     * The caller has to make sure that any instance returned is released afterwards.
     * @throws InterruptedException if interrupted while waiting
     */
    public T acquire() throws InterruptedException {
        final T earlyTry = pool.poll(0, TimeUnit.NANOSECONDS);
        if (earlyTry != null) {
            return earlyTry;
        }
        if (semaphore.tryAcquire(0, TimeUnit.NANOSECONDS)) {
            //create up to 'size' objects
            return createObject();
        }
        return pool.take();
    }

    /**
     * Puts the resource back into the pool.
     */
    public void release(T resource) {
        pool.add(resource);
    }

    private T createObject() {
        return supplier.get();
    }

}
