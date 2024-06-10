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

package org.mycore.ocfl.niofs.storage;

/**
 * Implementation of {@link MCROCFLEvictionStrategy} that never evicts files.
 * This strategy always returns {@code false} for eviction checks, meaning no files will be evicted.
 */
public class MCROCFLNeverEvictStrategy implements MCROCFLEvictionStrategy {

    @Override
    public boolean shouldEvict(long totalFiles, long totalAllocation) {
        return false;
    }

}
