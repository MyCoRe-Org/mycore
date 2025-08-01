package org.mycore.datamodel.metadata;

import org.mycore.access.MCRAccessException;

import java.util.List;

/**
 * Interface to define a strategy to store the order of child objects of a parent object.
 *
 */
public interface MCRChildrenOrderStrategy {

    /**
     * The strategy should return the order of the child objects of the parent object.
     * @param parentId the id of the parent object
     * @return the order of the child objects
     */
    List<MCRObjectID> getChildrenOrder(MCRObject parentId);

    /**
     * This method should set the order of the child objects of the parent object. The order is given as a list of
     * MCRObjectIDs.
     * @param parentId the id of the parent object
     * @param newOrder the new order of the child objects
     */
    void setChildrenOrder(MCRObject parentId, List<MCRObjectID> newOrder) throws MCRAccessException;

}
