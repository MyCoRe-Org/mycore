/**
 * 
 */
package org.mycore.mets.model;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author silvio
 *
 */
public class MCRDefaultLogicalStructMapTypeProvider implements MCRILogicalStructMapTypeProvider {

    /**
     * @return always monograph 
     */
    @Override
    public String getType(MCRObjectID objectId) {
        return "monograph";
    }
}
