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

package org.mycore.common.processing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.common.processing.MCRProcessableRegistryListener;

import com.google.inject.Singleton;

/**
 * Central base implementation for a processable registry.
 * 
 * @author Matthias Eichner
 */
@Singleton
public class MCRCentralProcessableRegistry implements MCRProcessableRegistry {

    private static Logger LOGGER = LogManager.getLogger();

    private List<MCRProcessableCollection> collections;

    private final List<MCRProcessableRegistryListener> listenerList;

    public MCRCentralProcessableRegistry() {
        this.collections = Collections.synchronizedList(new ArrayList<>());
        this.listenerList = new ArrayList<>();
    }

    /**
     * Registers a new collection to the registry.
     * 
     * @param collection the collection to register
     */
    public void register(MCRProcessableCollection collection) {
        if (this.collections.contains(collection)) {
            LOGGER.warn("Don't add same collection twice!");
            return;
        }
        this.collections.add(collection);
        fireAdded(collection);
    }

    /**
     * Removes a collection from the registry
     * 
     * @param collection the collection to remove
     */
    public void unregister(MCRProcessableCollection collection) {
        this.collections.remove(collection);
        fireRemoved(collection);
    }

    /**
     * Streams all the collections of this registry.
     * 
     * @return stream of the registry content.
     */
    public Stream<MCRProcessableCollection> stream() {
        return this.collections.stream();
    }

    @Override
    public void addListener(MCRProcessableRegistryListener listener) {
        this.listenerList.add(listener);
    }

    @Override
    public void removeListener(MCRProcessableRegistryListener listener) {
        this.listenerList.remove(listener);
    }

    protected void fireAdded(MCRProcessableCollection collection) {
        synchronized (this.listenerList) {
            this.listenerList.forEach(listener -> {
                try {
                    listener.onAdd(this, collection);
                } catch (Exception exc) {
                    LOGGER.error("Unable to inform registry listener due internal error", exc);
                }
            });
        }
    }

    protected void fireRemoved(MCRProcessableCollection collection) {
        synchronized (this.listenerList) {
            this.listenerList.forEach(listener -> {
                try {
                    listener.onRemove(this, collection);
                } catch (Exception exc) {
                    LOGGER.error("Unable to inform registry listener due internal error", exc);
                }
            });
        }
    }

}
