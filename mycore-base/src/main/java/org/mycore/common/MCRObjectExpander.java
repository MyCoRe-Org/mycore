package org.mycore.common;

import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Interface for expanding MCRObjects into MCRExpandedObjects. Should gather information from the database and other
 * resources to create a complete representation of the object, which may include duplicated metadata and structure.
 */
public interface MCRObjectExpander {

    /**
     * Expands the given MCRObject into an MCRExpandedObject. This method should gather all necessary information from
     * the database and other resources to create a complete representation of the object.
     *
     * @param mcrObject the normalized MCRObject to expand
     * @return the expanded MCRExpandedObject
     */
    MCRExpandedObject expand(MCRObject mcrObject);
    
}
