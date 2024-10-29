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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class MCRStoreCenter {
    private Map<String, MCRStore> storeHeap;

    private static MCRStoreCenter instance = new MCRStoreCenter();

    private MCRStoreCenter() {
        this.storeHeap = new HashMap<>();
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
        if (storeHeap.putIfAbsent(id, store) != null) {
            throw new MCRStoreAlreadyExistsException("Could not add store with ID " + id + ", store allready exists");
        }
    }

    /**
     * Get the MyCoRe Store with the given ID from store center.
     *
     * @param id - The id of the to retrieved store
     * @param storeClass - The class type of the retrieved store
     * @return The retrieved store or null if not exists
     * @deprecated use {@link #getStore(String)} instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T extends MCRStore> T getStore(String id, Class<T> storeClass) {
        return (T) storeHeap.get(id);
    }

    /**
     * Get the MyCoRe Store with the given ID from store center.
     *
     * @param id - The id of the to retrieved store
     * @return The retrieved store or null if not exists
     */
    public <T extends MCRStore> T getStore(String id) {
        return (T) storeHeap.get(id);
    }

    /**
     * @return a Stream of all {@link MCRStore}s that are an instance of <code>&lt;T&gt;</code>
     */
    public <T extends MCRStore> Stream<T> getCurrentStores(Class<T> sClass) {
        return storeHeap.values()
            .stream()
            .filter(sClass::isInstance)
            .map(s -> (T) s)
            .filter(Objects::nonNull);
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
