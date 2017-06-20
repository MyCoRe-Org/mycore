package org.mycore.datamodel.ifs2;

import java.util.HashMap;
import java.util.Map;

public class MCRStoreCenter {
    private Map<String, MCRStore> storeHeap;

    private static MCRStoreCenter instance = new MCRStoreCenter();

    private MCRStoreCenter() {
        this.storeHeap = new HashMap<String, MCRStore>();
    }

    public static MCRStoreCenter instance() {
        return instance;
    }

    /**
     * Add a store to the store center
     * 
     * @param store - Add this store to store center
     * @throws MCRStoreAlreadyExistsException If with the same id already exists in the store center
     */
    public void addStore(String id, MCRStore store) throws MCRStoreAlreadyExistsException {
        if (storeHeap.containsKey(id)) {
            throw new MCRStoreAlreadyExistsException("Could not add store with ID " + id + ", store allready exists");
        }

        storeHeap.put(id, store);
    }

    /**
     * Get the MyCoRe Store with the given ID from store center.
     * 
     * @param id - The id of the to retrieved store
     * @param storeClass - The class type of the retrieved store
     * @return The retrieved store or null if not exists
     */
    @SuppressWarnings("unchecked")
    public <T extends MCRStore> T getStore(String id, Class<T> storeClass) {
        return (T) storeHeap.get(id);
    }

    /**
     * Remove the store from store center
     * 
     * @param id - Removed this store from store center
     * @return true if successfully removed or false
     */
    public boolean removeStore(String id) {
        return storeHeap.remove(id) != null;
    }

    /**
     * Remove all store from the store center
     */
    public void clear() {
        storeHeap.clear();
    }
}
