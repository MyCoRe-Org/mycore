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

package org.mycore.datamodel.ifs2;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

/**
 * Manages the lifecycle of {@link MCRStore} instances, including creation, retrieval, and removal.
 * <p>
 * This class provides methods to create new stores, retrieve existing ones, and remove them from
 * the {@link MCRStoreCenter}. It ensures proper initialization and thread-safe store access.
 */
public class MCRStoreManager {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Builds a new store instance using the default configuration  and adds it to the {@link MCRStoreCenter}.
     *
     * @param id         the unique identifier for the store
     * @param storeClass the class of the store to create
     * @return the created store instance
     * @throws ReflectiveOperationException if the store cannot be instantiated
     */
    public static <T extends MCRStore> T createStore(String id, Class<T> storeClass)
        throws ReflectiveOperationException {
        return createStore(new MCRStoreDefaultConfig(id), storeClass);
    }

    /**
     * Builds a new store instance with the specified configuration and adds it to the {@link MCRStoreCenter}.
     *
     * @param config     the store configuration
     * @param storeClass the class of the store to create
     * @return the created store instance
     * @throws ReflectiveOperationException if the store cannot be instantiated
     */
    public static <T extends MCRStore> T createStore(MCRStoreConfig config, Class<T> storeClass)
        throws ReflectiveOperationException {
        T store = buildStore(config, storeClass);
        try {
            LOGGER.info("Adding instance of {} as MCRStore '{}'.", storeClass::getSimpleName, store::getID);
            MCRStoreCenter.getInstance().addStore(store.getID(), store);
        } catch (Exception e) {
            throw new MCRException("Could not create store with ID " + config.getID() + ", store already exists", e);
        }
        return store;
    }

    /**
     * Builds a new store instance using the default configuration.
     *
     * @param id         the unique identifier for the store
     * @param storeClass the class of the store to create
     * @return the built store instance
     * @throws ReflectiveOperationException if the store cannot be instantiated
     */
    public static <T extends MCRStore> T buildStore(String id, Class<T> storeClass)
        throws ReflectiveOperationException {
        return buildStore(new MCRStoreDefaultConfig(id), storeClass);
    }

    /**
     * Builds a new store instance with the specified configuration.
     *
     * @param config     the store configuration
     * @param storeClass the class of the store to create
     * @return the built store instance
     * @throws ReflectiveOperationException if the store cannot be instantiated
     */
    public static <T extends MCRStore> T buildStore(MCRStoreConfig config, Class<T> storeClass)
        throws ReflectiveOperationException {
        T store = storeClass.getDeclaredConstructor().newInstance();
        store.init(config);
        return store;
    }

    /**
     * Retrieves an existing store by its ID, or creates and registers a new one if absent.
     *
     * @param id            the store ID
     * @param storeSupplier the supplier function to create a store if it does not exist
     * @return the existing or newly created store instance
     */
    public static <T extends MCRStore> T computeStoreIfAbsent(String id, Supplier<T> storeSupplier) {
        return MCRStoreCenter.getInstance().computeStoreIfAbsent(id, storeSupplier);
    }

    /**
     * Returns the store with the given id
     *
     * @param id
     *            the ID of the store
     */
    public static <T extends MCRStore> T getStore(String id) {
        return MCRStoreCenter.getInstance().getStore(id);
    }

    public static void removeStore(String id) {
        LOGGER.info("Remove MCRStore '{}'.", id);
        MCRStoreCenter.getInstance().removeStore(id);
    }

}
