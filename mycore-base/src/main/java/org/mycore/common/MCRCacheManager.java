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

package org.mycore.common;

/**
 *
 * @author Thomas Scheffler (yagee)
 *
 * Need to insert some things here
 * jmx.mbean
 */
public class MCRCacheManager implements MCRCacheManagerMBean {

    @SuppressWarnings("rawtypes")
    private final MCRCache cache;

    public MCRCacheManager(@SuppressWarnings("rawtypes") final MCRCache cache) {
        this.cache = cache;
    }

    @Override
    public long getCapacity() {
        return cache.getCapacity();
    }

    @Override
    public double getFillRate() {
        return cache.getFillRate();
    }

    @Override
    public double getHitRate() {
        return cache.getHitRate();
    }

    @Override
    public long getHits() {
        return cache.backingCache.stats().hitCount();
    }

    @Override
    public long getRequests() {
        return cache.backingCache.stats().requestCount();
    }

    @Override
    public long getEvictions() {
        return cache.backingCache.stats().evictionCount();
    }

    @Override
    public long getSize() {
        return cache.getCurrentSize();
    }

    /**
     * jmx.managed-operation
     */
    @Override
    public void setCapacity(long capacity) {
        cache.setCapacity(capacity);
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
