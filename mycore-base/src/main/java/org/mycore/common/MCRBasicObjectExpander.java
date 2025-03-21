package org.mycore.common;

import org.mycore.datamodel.metadata.MCRChildOrderStrategyManager;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRExpandedObjectStructure;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkIDFactory;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class expands common elements of MCRObjects, including: children and derivates.
 */
public class MCRBasicObjectExpander implements MCRObjectExpander {


    @Override
    public MCRExpandedObject expand(MCRObject mcrObject) {

        MCRExpandedObjectStructure newStructure = new MCRExpandedObjectStructure();
        if (mcrObject.hasParent()) {
            newStructure.setParent(mcrObject.getParent());
        }

        for (MCRObjectID mcrObjectID : MCRChildOrderStrategyManager.getChildOrderStrategy().getChildOrder(mcrObject)) {
            newStructure.addChild(new MCRMetaLinkID("child", mcrObjectID, null, mcrObjectID.toString()));
        }

        mcrObject.getStructure().getChildrenOrder().forEach(newStructure.getChildrenOrder()::add);

        MCRMetadataManager.getDerivateIds(mcrObject.getId())
            .stream()
            .map(MCRMetadataManager::retrieveMCRDerivate)
            .map(MCRMetaEnrichedLinkIDFactory.obtainInstance()::getDerivateLink)
            .forEach(newStructure::addDerivate);

        MCRExpandedObject expandedObject =
            new MCRExpandedObject(newStructure, mcrObject.getMetadata(), mcrObject.getService(), "");

        expandedObject.setId(mcrObject.getId());
        expandedObject.setVersion(mcrObject.getVersion());
        expandedObject.setSchema(mcrObject.getSchema());

        return expandedObject;
    }

}
