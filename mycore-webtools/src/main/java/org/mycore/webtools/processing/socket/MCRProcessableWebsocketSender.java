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

    public enum Type {
        error, registry, addCollection, removeCollection, addProcessable, removeProcessable, updateProcessable
    }

    /**
     * Sends an error code.
     * 
     * @param session the websocket session
     * @param errorCode the error code
     */
    public void sendError(Session session, Integer errorCode);

    /**
     * Sends the whole registry.
     * 
     * @param session the websocket session
     * @param registry the registry to send
     */
    public void sendRegistry(Session session, MCRProcessableRegistry registry);

    /**
     * Appends the given collection to the registry.
     * 
     * @param session the websocket session
     * @param registry where to add the collection
     * @param collection the collection to add
     */
    public void addCollection(Session session, MCRProcessableRegistry registry, MCRProcessableCollection collection);

    /**
     * Removes the given collection.
     * 
     * @param session the websocket session
     * @param collection the collection to remove
     */
    public void removeCollection(Session session, MCRProcessableCollection collection);

    /**
     * Appends the given processable to the collection.
     * 
     * @param session the websocket session
     * @param collection where to add the processable
     * @param processable the processable to add
     */
    public void addProcessable(Session session, MCRProcessableCollection collection, MCRProcessable processable);

    /**
     * Removes the given processable.
     * 
     * @param session the websocket session
     * @param processable the processable to remove
     */
    public void removeProcessable(Session session, MCRProcessable processable);

    /**
     * Updates the content of the given processable.
     * 
     * @param session the websocket session
     * @param processable the processable to update
     */
    public void updateProcessable(Session session, MCRProcessable processable);

}
