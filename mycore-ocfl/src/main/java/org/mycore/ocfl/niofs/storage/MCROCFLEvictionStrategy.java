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

package org.mycore.ocfl.niofs.storage;

/**
 * Interface representing an eviction strategy for managing cached files.
 * Provides a method to determine whether files should be evicted.
 */
public interface MCROCFLEvictionStrategy {

    /**
     * Determines whether files should be evicted from the storage.
     * <p>
     * The storage may call this method while holding its internal lock, for example
     * {@link MCROCFLDefaultRemoteTemporaryStorage} after each new cache entry and once per evicted entry. Other
     * threads that access the storage wait until it returns. Implementations should therefore only read the state of
     * the storage, like {@link MCROCFLRemoteTemporaryStorage#allocated()} or
     * {@link MCROCFLRemoteTemporaryStorage#count()}, and must not block, do I/O or wait for other threads.
     *
     * @param storage the storage to evict from.
     * @return {@code true} if files should be evicted, {@code false} otherwise.
     */
    boolean shouldEvict(MCROCFLRemoteTemporaryStorage storage);

}
