package org.mycore.datamodel.ifs2;

import java.util.HashMap;

import org.mycore.common.MCRException;

public class MCRStoreManager {
    
    protected static HashMap<String, MCRStore> stores = new HashMap<String, MCRStore>();

    public static <T extends MCRStore> T createStore(String ID, Class<T> storeClass) throws InstantiationException, IllegalAccessException {
        T store = storeClass.newInstance();
        store.init(ID);
        try {
            MCRStoreCenter.instance().addStore(store);
        } catch (Exception e) {
            throw new MCRException("Could not create store with ID " + ID + ", store allready exists");
        }
        
        return store;
    }
    
    /**
     * Returns the store with the given ID
     * 
     * @param ID
     *            the ID of the store
     */
    public static MCRStore getStore(String ID) {
        return getStore(ID, MCRStore.class);
    }

    public static <T extends MCRStore> T getStore(String ID, Class<T> storeClass) {
        return MCRStoreCenter.instance().getStore(ID, storeClass);
    }
    
    public static void removeStore(String id) {
        MCRStoreCenter.instance().removeStore(id);
    }
}
