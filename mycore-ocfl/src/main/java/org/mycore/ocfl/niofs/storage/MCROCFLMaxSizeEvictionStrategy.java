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

import org.mycore.common.config.annotation.MCRProperty;

/**
 * Implementation of {@link MCROCFLEvictionStrategy} that evicts files based on a maximum size limit.
 * When the total allocation exceeds the specified maximum size, files are evicted.
 */
public class MCROCFLMaxSizeEvictionStrategy implements MCROCFLEvictionStrategy {

    private long maxSize;

    /**
     * Constructs a new {@code MCROCFLMaxSizeEvictionStrategy} with an unlimited size.
     */
    public MCROCFLMaxSizeEvictionStrategy() {
        this(Long.MAX_VALUE);
    }

    /**
     * Constructs a new {@code MCROCFLMaxSizeEvictionStrategy} with the specified maximum size.
     *
     * @param maxSize the maximum size for the total allocation in bytes.
     */
    public MCROCFLMaxSizeEvictionStrategy(long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Determines whether files should be evicted based on the total allocation size.
     */
    @Override
    public boolean shouldEvict(MCROCFLRemoteTemporaryStorage storage) {
        return storage.allocated() > maxSize;
    }

    /**
     * Gets the maximum size for the total allocation.
     *
     * @return the maximum size in bytes.
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the maximum size for the total allocation.
     *
     * @param maxSize the new maximum size in bytes.
     */
    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Sets the maximum size for the total allocation by property.
     * 
     * @param maxSizeInMB the maximum size in MB
     */
    @MCRProperty(name = "MaxSize")
    public void setMaxSizeFromProperty(String maxSizeInMB) {
        this.maxSize = Long.parseLong(maxSizeInMB) * 1024 * 1024;
    }

}
