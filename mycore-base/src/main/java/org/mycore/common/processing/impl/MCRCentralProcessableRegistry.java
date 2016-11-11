package org.mycore.common.processing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableRegistry;

/**
 * Central base implementation for a processable registry.
 * 
 * @author Matthias Eichner
 */
public class MCRCentralProcessableRegistry implements MCRProcessableRegistry {

    private static Logger LOGGER = LogManager.getLogger();

    private List<MCRProcessableCollection> collections;

    public MCRCentralProcessableRegistry() {
        this.collections = Collections.synchronizedList(new ArrayList<>());
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
    }

    /**
     * Removes a collection from the registry
     * 
     * @param collection the collection to remove
     */
    public void unregister(MCRProcessableCollection collection) {
        this.collections.remove(collection);
    }

    /**
     * Streams all the collections of this registry.
     * 
     * @return stream of the registry content.
     */
    public Stream<MCRProcessableCollection> stream() {
        return this.collections.stream();
    }

}
