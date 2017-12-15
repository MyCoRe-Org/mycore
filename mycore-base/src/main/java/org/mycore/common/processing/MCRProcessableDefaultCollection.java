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

package org.mycore.common.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base implementation of a processable collection.
 * 
 * @author Matthias Eichner
 */
public class MCRProcessableDefaultCollection implements MCRProcessableCollection {

    private static Logger LOGGER = LogManager.getLogger();

    private String name;

    private List<MCRProcessable> processables;

    private Map<String, Object> properties;

    private final List<MCRProcessableCollectionListener> listenerList;

    /**
     * Creates a new collection with the given a human readable name.
     * 
     * @param name name of this collection
     */
    public MCRProcessableDefaultCollection(String name) {
        this.name = name;
        this.processables = Collections.synchronizedList(new ArrayList<>());
        this.properties = new HashMap<>();
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
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void setProperty(String propertyName, Object propertyValue) {
        Object oldValue = this.properties.get(propertyName);
        if (oldValue == null && propertyValue == null) {
            return;
        }
        if (propertyValue == null) {
            this.properties.remove(propertyName);
            firePropertyChanged(propertyName, oldValue, null);
            return;
        }
        if (propertyValue.equals(oldValue)) {
            return;
        }
        this.properties.put(propertyName, propertyValue);
        firePropertyChanged(propertyName, oldValue, propertyValue);
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
                try {
                    listener.onAdd(this, processable);
                } catch (Exception exc) {
                    LOGGER.error("Unable to inform collection listener due internal error", exc);
                }
            });
        }
    }

    protected void fireRemoved(MCRProcessable processable) {
        synchronized (this.listenerList) {
            this.listenerList.forEach(listener -> {
                try {
                    listener.onRemove(this, processable);
                } catch (Exception exc) {
                    LOGGER.error("Unable to inform collection listener due internal error", exc);
                }
            });
        }
    }

    protected void firePropertyChanged(String propertyName, Object oldValue, Object newValue) {
        synchronized (this.listenerList) {
            this.listenerList.forEach(listener -> {
                try {
                    listener.onPropertyChange(this, propertyName, oldValue, newValue);
                } catch (Exception exc) {
                    LOGGER.error("Unable to inform collection listener due internal error", exc);
                }
            });
        }
    }

}
