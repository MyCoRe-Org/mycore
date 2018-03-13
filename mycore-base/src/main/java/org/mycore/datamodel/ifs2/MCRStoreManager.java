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

package org.mycore.datamodel.ifs2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

public class MCRStoreManager {
    private static final Logger LOGGER = LogManager.getLogger(MCRStoreManager.class);

    public static <T extends MCRStore> T createStore(String ID, Class<T> storeClass)
        throws ReflectiveOperationException {
        return createStore(new MCRStoreDefaultConfig(ID), storeClass);
    }

    public static <T extends MCRStore> T createStore(MCRStoreConfig config, Class<T> storeClass)
        throws ReflectiveOperationException {
        T store = storeClass.getDeclaredConstructor().newInstance();
        store.init(config);
        try {
            LOGGER.info("Adding instance of {} as MCRStore '{}'.", storeClass.getSimpleName(), store.getID());
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
        LOGGER.info("Remove MCRStore '{}'.", id);
        MCRStoreCenter.instance().removeStore(id);
    }
}
