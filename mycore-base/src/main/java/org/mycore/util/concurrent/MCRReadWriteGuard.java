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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * A MCRReadWriteGuard acts like a {@link ReadWriteLock} but automatically wraps read and write operations accordingly.
 * @author Thomas Scheffler (yagee)
 */
public class MCRReadWriteGuard {

    private Lock readLock;

    private Lock writeLock;

    public MCRReadWriteGuard() {
        this(new ReentrantReadWriteLock());
    }

    public MCRReadWriteGuard(ReadWriteLock readWriteLock) {
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
    }

    /**
     * Executes the read operation while the read lock is locked.
     * This is a sharable lock. Many <code>reader</code> can be executed simultaneously
     * when no write operation is running.
     * @param reader a read operation that should be guarded.
     * @return result of {@link Supplier#get()}
     */
    public <T> T read(Supplier<T> reader) {
        readLock.lock();
        try {
            return reader.get();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Executes the write operation while the write lock is locked.
     * This is an exclusive lock. So no other read or write operation
     * can be executed while <code>operation</code> is running.
     * @param operation
     */
    public void write(Runnable operation) {
        writeLock.lock();
        try {
            operation.run();
        } finally {
            writeLock.unlock();
        }
    }

    public <T> T lazyLoad(Supplier<Boolean> check, Runnable operation, Supplier<T> reader) {
        readLock.lock();
        boolean holdsReadLock = true;
        try {
            if (check.get()) {
                holdsReadLock = false;
                readLock.unlock();
                write(operation);
            }
        } finally {
            if (holdsReadLock) {
                readLock.unlock();
            }
        }
        return reader.get();

    }

}
