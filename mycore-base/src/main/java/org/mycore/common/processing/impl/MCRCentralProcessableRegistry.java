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

    private List<MCRProcessableRegistryListener> listenerList;

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
