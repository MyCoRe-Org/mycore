/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
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

    public long getCapacity() {
        return cache.getCapacity();
    }

    public double getFillRate() {
        return cache.getFillRate();
    }

    public double getHitRate() {
        return cache.getHitRate();
    }

    public long getHits() {
        return cache.backingCache.stats().hitCount();
    }

    public long getRequests() {
        return cache.backingCache.stats().requestCount();
    }

    public long getEvictions() {
        return cache.backingCache.stats().evictionCount();
    }

    public long getSize() {
        return cache.getCurrentSize();
    }

    /**
     * jmx.managed-operation
     */
    public void setCapacity(long capacity) {
        cache.setCapacity(capacity);
    }

    public void clear() {
        cache.clear();
    }

}
