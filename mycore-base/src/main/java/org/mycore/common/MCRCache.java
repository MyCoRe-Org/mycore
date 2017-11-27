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

package org.mycore.common;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.mycore.services.mbeans.MCRJMXBridge;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Instances of this class can be used as object cache. Each MCRCache has a certain capacity, the maximum number of
 * objects the cache will hold. When the cache is full and another object is put into the cache, the cache will discard
 * the least recently used object to get place for the new object. The cache will always hold the most recently used
 * objects by updating its internal structure whenever an object is get from the cache or put into the cache. The cache
 * also provides methods for getting the current cache hit rate and fill rate. Like in a hashtable, an MCRCache uses a
 * unique key for each object.
 * 
 * @see java.util.Hashtable
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCache<K, V> {
    /**
     * @author Thomas Scheffler (yagee)
     */
    public interface ModifiedHandle {

        /**
         * check distance in ms. After this period of time use {@link #getLastModified()} to check if object is still
         * up-to-date.
         */
        long getCheckPeriod();

        /**
         * returns timestamp when the cache value was last modified.
         */
        long getLastModified() throws IOException;

    }

    private static class MCRCacheEntry<V> {
        public MCRCacheEntry(V value) {
            this.value = value;
            this.insertTime = System.currentTimeMillis();
        }

        V value;

        long insertTime;

        public long lookUpTime;
    }

    /** Tch type string for the MCRCacheJMXBridge */
    protected String type;

    Cache<K, MCRCacheEntry<V>> backingCache;

    private long capacity;

    /**
     * Creates a new cache with a given capacity.
     * 
     * @param capacity
     *            the maximum number of objects this cache will hold
     * @param type
     *            the type string for MCRCacheJMXBridge
     */
    public MCRCache(long capacity, String type) {
        backingCache = CacheBuilder.newBuilder().recordStats().maximumSize(capacity).build();
        this.capacity = capacity;
        this.type = type;
        Object mbean = new MCRCacheManager(this);
        MCRJMXBridge.register(mbean, "MCRCache", type);
    }

    /**
     * Puts an object into the cache, storing it under the given key. If the cache is already full, the least recently
     * used object will be removed from the cache first. If the cache already contains an entry under the key provided,
     * this entry is replaced.
     * 
     * @param key
     *            the non-null key to store the object under
     * @param value
     *            the non-null object to be put into the cache
     */
    public void put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("The key of a cache entry may not be null.");
        }
        if (value == null) {
            throw new NullPointerException("The value of a cache entry may not be null.");
        }
        MCRCacheEntry<V> entry = new MCRCacheEntry<>(value);
        backingCache.put(key, entry);
    }

    /**
     * Puts an object into the cache, storing it under the given key. If the cache is already full, the least recently
     * used object will be removed from the cache first. If the cache already contains an entry under the key provided,
     * this entry is replaced.
     * 
     * @param key
     *            the non-null key to store the object under
     * @param value
     *            the non-null object to be put into the cache
     * @param insertTime
     *            the given last modified time for this key           
     */
    public void put(K key, V value, long insertTime) {
        if (key == null) {
            throw new NullPointerException("The key of a cache entry may not be null.");
        }
        if (value == null) {
            throw new NullPointerException("The value of a cache entry may not be null.");
        }
        MCRCacheEntry<V> entry = new MCRCacheEntry<>(value);
        entry.insertTime = insertTime;
        backingCache.put(key, entry);
    }

    /**
     * Removes an object from the cache for the given key.
     * 
     * @param key
     *            the key for the object you want to remove from this cache
     */
    public void remove(K key) {
        if (key == null) {
            throw new MCRUsageException("The value of the argument key is null.");
        }
        backingCache.invalidate(key);
    }

    /**
     * Returns an object from the cache for the given key, or null if there currently is no object in the cache with
     * this key.
     * 
     * @param key
     *            the key for the object you want to get from this cache
     * @return the cached object, or null
     */
    public V get(K key) {
        MCRCacheEntry<V> found = backingCache.getIfPresent(key);
        return found == null ? null : found.value;
    }

    /**
     * Returns an object from the cache for the given key, but only if the cache entry is not older than the given
     * timestamp. If there currently is no object in the cache with this key, null is returned. If the cache entry is
     * older than the timestamp, the entry is removed from the cache and null is returned.
     * 
     * @param key
     *            the key for the object you want to get from this cache
     * @param time
     *            the timestamp to check that the cache entry is up to date
     * @return the cached object, or null
     */
    public V getIfUpToDate(K key, long time) {
        MCRCacheEntry<V> found = backingCache.getIfPresent(key);

        if (found == null || found.insertTime < time) {
            return null;
        }

        if (found.insertTime >= time) {
            found.lookUpTime = System.currentTimeMillis();
            return found.value;
        }
        backingCache.invalidate(key);
        return null;
    }

    /**
     * Returns an object from the cache for the given key, but only if the cache entry is not older than the given
     * timestamp of the {@link ModifiedHandle}. In contrast to {@link #getIfUpToDate(Object, long)} you can submit your
     * own handle that returns the last modified timestamp after a certain period is over. Use this method if
     * determining lastModified date is rather expensive and cache access is often.
     * 
     * @param key
     *            the key for the object you want to get from this cache
     * @param handle
     *            the timestamp to check that the cache entry is up to date
     * @return the cached object, or null
     * @throws IOException
     *             thrown by {@link ModifiedHandle#getLastModified()}
     * @since 2.1.81
     */
    public V getIfUpToDate(K key, ModifiedHandle handle) throws IOException {
        MCRCacheEntry<V> found = backingCache.getIfPresent(key);
        if (found == null) {
            return null;
        }
        if (System.currentTimeMillis() - found.lookUpTime > handle.getCheckPeriod()) {
            if (found.insertTime >= handle.getLastModified()) {
                found.lookUpTime = System.currentTimeMillis();
                return found.value;
            }
            backingCache.invalidate(key);
            return null;
        } else {
            return found.value;
        }
    }

    /**
     * Returns the number of objects currently cached.
     * 
     * @return the number of objects currently cached
     */
    public long getCurrentSize() {
        backingCache.cleanUp();
        return backingCache.size();
    }

    /**
     * Returns the capacity of this cache. This is the maximum number of objects this cache will hold at a time.
     * 
     * @return the capacity of this cache
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Changes the capacity of this cache. This is the maximum number of objects that will be cached at a time. If the
     * new capacity is smaller than the current number of objects in the cache, the least recently used objects will be
     * removed from the cache.
     * 
     * @param capacity
     *            the maximum number of objects this cache will hold
     */
    public synchronized void setCapacity(long capacity) {
        this.capacity = capacity;
        Cache<K, MCRCacheEntry<V>> newCache = CacheBuilder.newBuilder().recordStats().maximumSize(capacity).build();
        newCache.putAll(backingCache.asMap());
        Cache<K, MCRCacheEntry<V>> oldCache = backingCache;
        backingCache = newCache;
        oldCache.invalidateAll();
    }

    /**
     * Returns true if this cache is full.
     * 
     * @return true if this cache is full
     */
    public boolean isFull() {
        backingCache.cleanUp();
        return backingCache.size() == capacity;
    }

    /**
     * Returns true if this cache is empty.
     * 
     * @return true if this cache is empty
     */
    public boolean isEmpty() {
        backingCache.cleanUp();
        return backingCache.size() == 0;
    }

    /**
     * Returns the fill rate of this cache. This is the current number of objects in the cache diveded by its capacity.
     * 
     * @return the fill rate of this cache as double value
     */
    public double getFillRate() {
        return capacity == 0 ? 1.0 : (double) getCurrentSize() / (double) capacity;
    }

    /**
     * Returns the hit rate of this cache. This is the number of successful hits divided by the total number of get
     * requests so far. Using this ratio can help finding the appropriate cache capacity.
     * 
     * @return the hit rate of this cache as double value
     */
    public double getHitRate() {
        return backingCache.stats().hitRate();
    }

    /**
     * Clears the cache by removing all entries from the cache
     */
    public void clear() {
        backingCache.invalidateAll();
    }

    /**
     * Returns a String containing information about cache capacity, size, current fill rate and hit rate. Useful for
     * testing and debugging.
     */
    @Override
    public String toString() {

        return "Cache capacity:  " + capacity + "\n" + "Cache size:      " + backingCache.size() + "\n"
            + "Cache fill rate: " + getFillRate() + "\n" + "Cache hit rate:  " + getHitRate();
    }

    /**
     * A small sample program for testing this class.
     */
    public static void main(String[] args) {
        MCRCache<String, String> cache = new MCRCache<>(4, "Small Sample Program");
        System.out.println(cache);
        cache.put("a", "Anton");
        cache.put("b", "Bohnen");
        cache.put("c", "Cache");
        System.out.println(cache);
        cache.get("d");
        cache.get("c");
        cache.put("d", "Dieter");
        cache.put("e", "Egon");
        cache.put("f", "Frank");
        cache.get("c");
        System.out.println(cache);
    }

    public void close() {
        MCRJMXBridge.unregister("MCRCache", type);
        clear();
    }

    /**
     * Returns an iterable list of keys to the cached objects.
     */
    public List<K> keys() {
        return Collections.list(Collections.enumeration(backingCache.asMap().keySet()));
    }
}
