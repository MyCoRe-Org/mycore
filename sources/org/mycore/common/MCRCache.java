/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common;

import java.util.Hashtable;

import org.mycore.services.mbeans.MCRJMXBridge;

/**
 * Instances of this class can be used as object cache. Each MCRCache has a
 * certain capacity, the maximum number of objects the cache will hold. When the
 * cache is full and another object is put into the cache, the cache will
 * discard the least recently used object to get place for the new object. The
 * cache will always hold the most recently used objects by updating its
 * internal structure whenever an object is get from the cache or put into the
 * cache. The cache also provides methods for getting the current cache hit rate
 * and fill rate. Like in a hashtable, an MCRCache uses a unique key for each
 * object.
 * 
 * @see java.util.Hashtable
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCache {
    /**
     * For each object in the cache, there is one MCRCacheEntry object
     * encapsulating it. The cache uses a double-linked list of MCRCacheEntries
     * and holds references to the most and least recently used entry.
     */
    class MCRCacheEntry {
        /** The entry before this one, more often used than this entry */
        MCRCacheEntry before;

        /** The entry after this one, less often used than this entry */
        MCRCacheEntry after;

        /** The key for this object, to be used for removing the object */
        Object key;

        /** The timestamp when this object was placed in the cache */
        long time;

        /** The stored object encapsulated by this entry */
        Object object;
    }

    /** The most recently used object * */
    protected MCRCacheEntry mru;

    /** The least recently used object * */
    protected MCRCacheEntry lru;

    /** A hashtable for looking up a cached object by a given key */
    protected Hashtable<Object,MCRCacheEntry> index = new Hashtable<Object,MCRCacheEntry>();

    /** The number of requests to get an object from this cache */
    protected long gets = 0;

    /** The number of hits, where a requested object really was in the cache */
    protected long hits = 0;

    /** The number of objects currently stored in the cache */
    protected int size = 0;

    /** The maximum number of objects that the cache can hold */
    protected int capacity;
    
    /** Tch type string for the MCRCacheJMXBridge */
    protected String type;
    
    /** The constructor */
    private MCRCache(){};

    /**
     * Creates a new cache with a given capacity.
     * 
     * @param capacity
     *            the maximum number of objects this cache will hold
     * @param type the type string for MCRCacheJMXBridge
     */
    public MCRCache(int capacity, String type) {
        setCapacity(capacity);
        this.type=type;
        Object mbean = new MCRCacheManager(this);
        MCRJMXBridge.register(mbean, "MCRCache", type);
    }

    /**
     * Puts an object into the cache, storing it under the given key. If the
     * cache is already full, the least recently used object will be removed
     * from the cache first. If the cache already contains an entry under the
     * key provided, this entry is replaced.
     * 
     * @param key
     *            the non-null key to store the object under
     * @param obj
     *            the non-null object to be put into the cache
     */
    public synchronized void put(Object key, Object obj) {
        if (key == null) {
            throw new MCRUsageException("The value of the argument key is null.");
        }
        if (obj == null) {
            throw new MCRUsageException("The value of the argument obj is null.");
        }

        if (capacity == 0) {
            return;
        }

        if (index.containsKey(key)) {
            remove(key);
        }

        if (isFull()) {
            remove(lru.key);
        }

        MCRCacheEntry added = new MCRCacheEntry();
        added.object = obj;
        added.key = key;
        added.time = System.currentTimeMillis();
        index.put(key, added);

        if (isEmpty()) {
            lru = mru = added;
        } else {
            added.before = mru;
            mru.after = added;
        }

        size++;
        mru = added;
    }

    /**
     * Removes an object from the cache for the given key.
     * 
     * @param key
     *            the key for the object you want to remove from this cache
     */
    public synchronized void remove(Object key) {
        if (key == null) {
            throw new MCRUsageException("The value of the argument key is null.");
        }

        if (!index.containsKey(key)) {
            return;
        }

        MCRCacheEntry removed = (MCRCacheEntry) (index.get(key));

        if (removed == lru) {
            lru = removed.after;
        } else {
            removed.before.after = removed.after;
        }

        if (removed == mru) {
            mru = removed.before;
        } else {
            removed.after.before = removed.before;
        }

        removed.object = null;
        removed.key = null;
        removed.time = 0;
        removed.before = null;
        removed.after = null;
        index.remove(key);
        size--;
    }

    /**
     * Returns an object from the cache for the given key, or null if there
     * currently is no object in the cache with this key.
     * 
     * @param key
     *            the key for the object you want to get from this cache
     * @return the cached object, or null
     */
    public synchronized Object get(Object key) {
        if (key == null) {
            throw new MCRUsageException("The value of the argument key is null.");
        }

        gets++;

        if (!index.containsKey(key)) {
            return null;
        }

        hits++;

        MCRCacheEntry found = (MCRCacheEntry) (index.get(key));

        if (found != mru) {
            found.after.before = found.before;

            if (found == lru) {
                lru = found.after;
            } else {
                found.before.after = found.after;
            }

            found.after = null;
            found.before = mru;
            mru.after = found;
            mru = found;
        }

        return found.object;
    }

    /**
     * Returns an object from the cache for the given key, but only if the cache
     * entry is not older than the given timestamp. If there currently is no
     * object in the cache with this key, null is returned. If the cache entry
     * is older than the timestamp, the entry is removed from the cache and null
     * is returned.
     * 
     * @param key
     *            the key for the object you want to get from this cache
     * @param time
     *            the timestamp to check that the cache entry is up to date
     * @return the cached object, or null
     */
    public synchronized Object getIfUpToDate(Object key, long time) {
        Object value = get(key);

        if (value == null) {
            return null;
        }

        MCRCacheEntry found = (MCRCacheEntry) (index.get(key));

        if (found.time >= time) {
            return value;
        }

        remove(key);

        return null;
    }

    /**
     * Returns the number of objects currently cached.
     * 
     * @return the number of objects currently cached
     */
    public synchronized int getCurrentSize() {
        return size;
    }

    /**
     * Returns the capacity of this cache. This is the maximum number of objects
     * this cache will hold at a time.
     * 
     * @return the capacity of this cache
     */
    public synchronized int getCapacity() {
        return capacity;
    }

    /**
     * Changes the capacity of this cache. This is the maximum number of objects
     * that will be cached at a time. If the new capacity is smaller than the
     * current number of objects in the cache, the least recently used objects
     * will be removed from the cache.
     * 
     * @param capacity
     *            the maximum number of objects this cache will hold
     */
    public synchronized void setCapacity(int capacity) {
        if (capacity < 0) {
            throw new MCRUsageException("The cache capacity must be >= 0.");
        }

        while (size > capacity)
            remove(lru.key);

        this.capacity = capacity;
    }

    /**
     * Returns true if this cache is full.
     * 
     * @return true if this cache is full
     */
    public synchronized boolean isFull() {
        return (size == capacity);
    }

    /**
     * Returns true if this cache is empty.
     * 
     * @return true if this cache is empty
     */
    public synchronized boolean isEmpty() {
        return (size == 0);
    }

    /**
     * Returns the fill rate of this cache. This is the current number of
     * objects in the cache diveded by its capacity.
     * 
     * @return the fill rate of this cache as double value
     */
    public synchronized double getFillRate() {
        return ((capacity == 0) ? 1.0 : ((double) size / (double) capacity));
    }

    /**
     * Returns the hit rate of this cache. This is the number of successful hits
     * divided by the total number of get requests so far. Using this ratio can
     * help finding the appropriate cache capacity.
     * 
     * @return the hit rate of this cache as double value
     */
    public synchronized double getHitRate() {
        return ((gets == 0) ? 1.0 : ((double) hits / (double) gets));
    }

    /**
     * Clears the cache by removing all entries from the cache
     */
    public synchronized void clear() {
        index = new Hashtable<Object,MCRCacheEntry>();
        size = 0;
        mru = lru = null;
    }

    /**
     * Returns a String containing information about cache capacity, size,
     * current fill rate and hit rate. Useful for testing and debugging.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Cache capacity:  ").append(capacity).append("\n");
        sb.append("Cache size:      ").append(size).append("\n");
        sb.append("Cache fill rate: ").append(getFillRate()).append("\n");
        sb.append("Cache hit rate:  ").append(getHitRate());

        return sb.toString();
    }

    /**
     * A small sample program for testing this class.
     */
    public static void main(String[] args) {
        MCRCache cache = new MCRCache(4, "Small Sample Program");
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
        MCRJMXBridge.unregister("MCRCache", this.type);
        clear();
    }
}
