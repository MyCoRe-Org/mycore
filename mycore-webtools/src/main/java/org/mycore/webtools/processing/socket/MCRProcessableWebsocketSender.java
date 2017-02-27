package org.mycore.webtools.processing.socket;

import javax.websocket.Session;

import org.mycore.common.processing.MCRProcessable;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableRegistry;

/**
 * Base interface to send processables, collections and the registry over the wire.
 * 
 * @author Matthias Eichner
 */
public interface MCRProcessableWebsocketSender {

    enum Type {
        error, registry, addCollection, removeCollection, addProcessable, removeProcessable, updateProcessable, updateCollectionProperty
    }

    /**
     * Sends an error code.
     * 
     * @param session the websocket session
     * @param errorCode the error code
     */
    void sendError(Session session, Integer errorCode);

    /**
     * Sends the whole registry.
     * 
     * @param session the websocket session
     * @param registry the registry to send
     */
    void sendRegistry(Session session, MCRProcessableRegistry registry);

    /**
     * Appends the given collection to the registry.
     * 
     * @param session the websocket session
     * @param registry where to add the collection
     * @param collection the collection to add
     */
    void addCollection(Session session, MCRProcessableRegistry registry, MCRProcessableCollection collection);

    /**
     * Removes the given collection.
     * 
     * @param session the websocket session
     * @param collection the collection to remove
     */
    void removeCollection(Session session, MCRProcessableCollection collection);

    /**
     * Appends the given processable to the collection.
     * 
     * @param session the websocket session
     * @param collection where to add the processable
     * @param processable the processable to add
     */
    void addProcessable(Session session, MCRProcessableCollection collection, MCRProcessable processable);

    /**
     * Removes the given processable.
     * 
     * @param session the websocket session
     * @param processable the processable to remove
     */
    void removeProcessable(Session session, MCRProcessable processable);

    /**
     * Updates the content of the given processable.
     * 
     * @param session the websocket session
     * @param processable the processable to update
     */
    void updateProcessable(Session session, MCRProcessable processable);

    /**
     * Updates a property of the given processable collection.
     * 
     * @param session the websocket session
     * @param collection the collection to update
     * @param name name of the property
     * @param value value of the property
     */
    void updateProperty(Session session, MCRProcessableCollection collection, String name, Object value);

}
