/**
 * 
 */
package org.mycore.access.strategies;

import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author shermann
 */
public class MCRDerivateIDStrategy implements MCRAccessCheckStrategy {

    @Override
    public boolean checkPermission(String id, String permission) {
        if (!id.contains("_derivate_")) {
            return new MCRObjectIDStrategy().checkPermission(id, permission);
        }
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(id));
        return new MCRObjectIDStrategy().checkPermission(derivate.getOwnerID().toString(), permission);
    }
}
