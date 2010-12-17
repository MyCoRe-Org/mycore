package org.mycore.datamodel.ifs2;

import java.util.HashMap;
import java.util.Map;

public class MCRStoreCenter {
    private Map<String, MCRStore> storeHeap;

    private static MCRStoreCenter instance;

    private MCRStoreCenter() {
    }

    public static MCRStoreCenter instance() {
        if (instance == null) {
            instance = new MCRStoreCenter();
            instance.setStoreHeap(new HashMap<String, MCRStore>());
        }

        return instance;
    }

    private void setStoreHeap(Map<String, MCRStore> storeHeap) {
        this.storeHeap = storeHeap;
    }

    private Map<String, MCRStore> getStoreHeap() {
        return storeHeap;
    }

    /**
     * Add a store to the store center
     * 
     * @param store - Add this store to store center
     * @throws StoreAlreadyExistsException If with the same id already exists in the store center
     */
    public void addStore(MCRStore store) throws StoreAlreadyExistsException  {
        String storeID = store.getID();
        if (getStoreHeap().containsKey(storeID)) {
            throw new StoreAlreadyExistsException("Could not add store with ID " + storeID + ", store allready exists");
        }

        getStoreHeap().put(storeID, store);
    }

    /**
     * Get the MyCoRe Store with the given ID from store center.
     * 
     * @param id - The id of the to retrieved store
     * @param storeClass - The class type of the retrieved store
     * @return The retrieved store or null if not exists
     */
    public <T extends MCRStore> T getStore(String id, Class<T> storeClass) {
        return (T) getStoreHeap().get(id);
    }

    /**
     * Remove the store from store center
     * 
     * @param store - Removed this store from store center
     * @return True if successfully removed or false
     */
    public boolean removeStore(String id) {
        return getStoreHeap().remove(id) != null;
    }

    /**
     * Remove all store from the store center
     */
    public void clear() {
        instance = null;
    }
}
