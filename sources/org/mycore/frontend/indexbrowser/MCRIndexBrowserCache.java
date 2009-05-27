package org.mycore.frontend.indexbrowser;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * Caches index browser entries in a hash table. Each object type 
 * gets an entry in the table. The value of an entry is a MCRCache.
 * This cache again creates new entries for different search and modes.
 * So the cache key is: object type # search # mode
 *
 * @author Matthias Eichner
 */
public class MCRIndexBrowserCache {
    protected static Logger LOGGER = Logger.getLogger(MCRIndexBrowserCache.class);

    private static Hashtable<String, MCRCache> TYPE_CACHE_TABLE = new Hashtable<String, MCRCache>();
    private static ReentrantReadWriteLock TYPE_CACHE_TABLE_LOCK = new ReentrantReadWriteLock();

    /**
     * Add a new list of index browser entries to the cache.
     * @param listToCache a list to cache
     */
    public static void addToCache(MCRIndexBrowserIncomingData browseData, List<MCRIndexBrowserEntry> listToCache) {
        String index = browseData.getIndex();
        // adds a new mcr cache to the hash table
        MCRCache mcrCache = addIndexCacheToHashtable(index);
        // add the index browser entry list to the mcr cache
        String cacheKey = getCacheKey(browseData);
        mcrCache.put(cacheKey, listToCache);
    }

    /**
     * Adds a new MCRCache to the hash table.
     * @param alias the key of the entry.
     */
    protected static MCRCache addIndexCacheToHashtable(String alias) {
        String objectType = getObjectType(alias);
        if (objectType == null)
            throw new MCRException("Could not determine object type for alias: " + alias);
        MCRCache cache = null;
        try {
            TYPE_CACHE_TABLE_LOCK.writeLock().lock();
            if (!TYPE_CACHE_TABLE.containsKey(objectType))
                TYPE_CACHE_TABLE.put(objectType, cache = new MCRCache(1000, "IndexBrowser,objectType=" + objectType.replace(",", "_")));
        } finally {
            TYPE_CACHE_TABLE_LOCK.writeLock().unlock();
        }
        return cache;
    }

    /**
     * Deletes a whole MCRCache from the hash table.
     * @param objectType the object type which has to be removed
     */
    public static void deleteIndexCacheFromHashtable(String objectType) {
        if (objectType == null)
            return;
        TYPE_CACHE_TABLE_LOCK.writeLock().lock();
        TYPE_CACHE_TABLE.remove(objectType);
        TYPE_CACHE_TABLE_LOCK.writeLock().unlock();
    }
    
    /**
     * Returns the cached index browser list.
     * @return the cached list.
     */
    public static List<MCRIndexBrowserEntry> getFromCache(MCRIndexBrowserIncomingData browseData) {
        // if the list is not cached, return null
        if(!isCached(browseData))
            return null;

        String index = browseData.getIndex();
        String cacheKey = getCacheKey(browseData);
        // get the mcr cache from the hash table
        MCRCache mcrCache = getIndexCache(index);
        // get the cached list from the mcr cache
        List<MCRIndexBrowserEntry> cachedList = ((List<MCRIndexBrowserEntry>)mcrCache.get(cacheKey));
        // return the list
        return cachedList;
    }

    /**
     * Returns the MCRCache from the hash table by the given alias.
     * @param alias the alias of the specific index browser
     * @return a MCRCache from the hash table
     */
    protected static MCRCache getIndexCache(String alias) {
        String objectType = getObjectType(alias);
        try {
            TYPE_CACHE_TABLE_LOCK.readLock().lock();
            return TYPE_CACHE_TABLE.get(objectType);
        } finally {
            TYPE_CACHE_TABLE_LOCK.readLock().unlock();
        }
    }

    /**
     * Returns the object type of the given alias.
     * (jpperson_sub -> returns: person )
     * @param index the alias of the specific index browser
     * @return object type
     */
    protected static String getObjectType(String index) {
        // get object type belonging to alias
        String propKey = "MCR.IndexBrowser." + index + ".Table";
        String objectType = MCRConfiguration.instance().getProperties("MCR.IndexBrowser.").getProperty(propKey);
        return objectType;
    }

    /**
     * Returns the cache key from the incoming browser data.
     * @return the cache key
     */
    protected static String getCacheKey(MCRIndexBrowserIncomingData browseData) {
        return browseData.getIndex() + "#" + browseData.getSearch() + "#" + browseData.getMode();
    }

    /**
     * Checks if a hash table entry with the specified alias
     * exists. If true, the method checks if the specified key
     * exists in the MCRCache.
     * @return true if an entry in the hash table and in the MCRCache exists,
     * otherwise false
     */
    public static boolean isCached(MCRIndexBrowserIncomingData browseData) {
        String index = browseData.getIndex();
        String cacheKey = getCacheKey(browseData);
        String objType = getObjectType(index);
        try {
            TYPE_CACHE_TABLE_LOCK.readLock().lock();
            if (TYPE_CACHE_TABLE.get(objType) != null && TYPE_CACHE_TABLE.get(objType).get(cacheKey) != null)
                return true;
            return false;
        } finally {
            TYPE_CACHE_TABLE_LOCK.readLock().unlock();
        }
    }
}