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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class MCRStoreCenter {

    private final Map<String, MCRStore> storeHeap = new ConcurrentHashMap<>();

    private MCRStoreCenter() {
    }

    /**
     * @deprecated use {@link #getInstance()} instead
     */
    @Deprecated
    public static MCRStoreCenter instance() {
        return getInstance();
    }

    public static MCRStoreCenter getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    /**
     * Add a store to the store center
     * 
     * @param store - Add this store to store center
     * @throws MCRStoreAlreadyExistsException If with the same id already exists in the store center
     */
    public void addStore(String id, MCRStore store) throws MCRStoreAlreadyExistsException {
        if (storeHeap.putIfAbsent(id, store) != null) {
            throw new MCRStoreAlreadyExistsException("Could not add store with ID " + id + ", store already exists");
        }
    }

    /**
     * Computes a store if it does not exist yet.
     *
     * @param id store id
     * @param storeSupplier the mapping function to create a store
     * @return the original store or the newly computed one
     * @param <T> the store
     */
    @SuppressWarnings("unchecked")
    public <T extends MCRStore> T computeStoreIfAbsent(String id, Supplier<T> storeSupplier) {
        return (T) storeHeap.computeIfAbsent(id, k -> storeSupplier.get());
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

    private static final class LazyInstanceHolder {
        public static final MCRStoreCenter SINGLETON_INSTANCE = new MCRStoreCenter();
    }

}
