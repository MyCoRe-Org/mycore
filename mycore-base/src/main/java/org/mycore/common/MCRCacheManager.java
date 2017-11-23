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
