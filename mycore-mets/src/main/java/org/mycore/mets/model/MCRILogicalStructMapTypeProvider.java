/**
 * 
 */
package org.mycore.mets.model;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author silvio
 *
 */
public interface MCRILogicalStructMapTypeProvider {

    /**
     * @return the type depending on metadata given by an object id
     */
    public String getType(MCRObjectID objectId);
}
