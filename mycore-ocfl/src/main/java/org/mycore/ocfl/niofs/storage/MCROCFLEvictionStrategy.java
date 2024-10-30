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
 * Provides a method to determine whether files should be evicted based on the
 * total number of files and the total allocated size.
 */
public interface MCROCFLEvictionStrategy {

    /**
     * Determines whether files should be evicted from the cache.
     *
     * @param totalFiles the total number of files currently in the cache.
     * @param totalAllocation the total size of all files currently in the cache, in bytes.
     * @return {@code true} if files should be evicted, {@code false} otherwise.
     */
    boolean shouldEvict(long totalFiles, long totalAllocation);

}
