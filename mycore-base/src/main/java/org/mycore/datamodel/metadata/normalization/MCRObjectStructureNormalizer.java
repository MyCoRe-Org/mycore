package org.mycore.datamodel.metadata.normalization;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;

/**
 * This normalizer normalizes the structure of an MCRObject.
 * It removes child elements from the structure because they are already present in the child elements.
 * The information can be retained from the Database using
 * {@link org.mycore.datamodel.metadata.MCRMetadataManager#getDerivateIds(MCRObjectID)},
 * {@link org.mycore.datamodel.metadata.MCRMetadataManager#getChildren(MCRObjectID)} or
 * {@link org.mycore.common.MCRExpandedObjectManager#getExpandedObject(MCRObject)}.
 */
public class MCRObjectStructureNormalizer extends MCRObjectNormalizer {

    @Override
    public void normalize(MCRObject mcrObject) {
        MCRObjectStructure normalizedStructure = new MCRObjectStructure();
        normalizedStructure.setFromDOM(mcrObject.getStructure().createXML());
        mcrObject.getStructure().clear();
        mcrObject.getStructure().setFromDOM(normalizedStructure.createXML());
    }

}
