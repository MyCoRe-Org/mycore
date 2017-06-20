package org.mycore.datamodel.ifs2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

public class MCRStoreManager {
    private static final Logger LOGGER = LogManager.getLogger(MCRStoreManager.class);

    public static <T extends MCRStore> T createStore(String ID, Class<T> storeClass)
        throws InstantiationException, IllegalAccessException {
        return createStore(new MCRStoreDefaultConfig(ID), storeClass);
    }

    public static <T extends MCRStore> T createStore(MCRStoreConfig config, Class<T> storeClass)
        throws InstantiationException, IllegalAccessException {
        T store = storeClass.newInstance();
        store.init(config);
        try {
            LOGGER.info("Adding instance of " + storeClass.getSimpleName() + " as MCRStore '" + store.getID() + "'.");
            MCRStoreCenter.instance().addStore(store.getID(), store);
        } catch (Exception e) {
            throw new MCRException("Could not create store with ID " + config.getID() + ", store allready exists");
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
        LOGGER.info("Remove MCRStore '" + id + "'.");
        MCRStoreCenter.instance().removeStore(id);
    }
}
