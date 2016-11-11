package org.mycore.common.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Base implementation of a processable collection.
 * 
 * @author Matthias Eichner
 */
public class MCRProcessableDefaultCollection implements MCRProcessableCollection {

    private String name;

    private List<MCRProcessable> processables;

    private List<MCRProcessableCollectionListener> listenerList;

    /**
     * Creates a new collection with the given a human readable name.
     * 
     * @param name name of this collection
     */
    public MCRProcessableDefaultCollection(String name) {
        this.name = name;
        this.processables = Collections.synchronizedList(new ArrayList<>());
        this.listenerList = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Returns the human readable name of this collection.
     */
    public String getName() {
        return name;
    }

    @Override
    public void add(MCRProcessable processable) {
        this.processables.add(processable);
        fireAdded(processable);
    }

    @Override
    public void remove(MCRProcessable processable) {
        this.processables.remove(processable);
        fireRemoved(processable);
    }

    @Override
    public Stream<MCRProcessable> stream() {
        return this.processables.stream();
    }

    @Override
    public void addListener(MCRProcessableCollectionListener listener) {
        this.listenerList.add(listener);
    }

    @Override
    public void removeListener(MCRProcessableCollectionListener listener) {
        this.listenerList.remove(listener);
    }

    protected void fireAdded(MCRProcessable processable) {
        synchronized (this.listenerList) {
            this.listenerList.forEach(listener -> {
                listener.onAdd(this, processable);
            });
        }
    }

    protected void fireRemoved(MCRProcessable processable) {
        synchronized (this.listenerList) {
            this.listenerList.forEach(listener -> {
                listener.onRemove(this, processable);
            });
        }
    }

}
